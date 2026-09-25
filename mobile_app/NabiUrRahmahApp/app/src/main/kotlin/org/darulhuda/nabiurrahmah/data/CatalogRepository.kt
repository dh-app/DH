package org.darulhuda.nabiurrahmah.data

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.darulhuda.nabiurrahmah.data.model.Catalog
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.data.site.NabiSiteParser
import org.darulhuda.nabiurrahmah.data.site.SiteLanguage
import org.darulhuda.nabiurrahmah.data.source.CatalogStore
import org.darulhuda.nabiurrahmah.data.source.WebsiteSource

data class CatalogState(
    val catalog: Catalog? = null,
    val isRefreshing: Boolean = false,
    val error: CatalogError? = null,
    /** Languages whose flyer page is being read right now. */
    val loadingLanguages: Set<String> = emptySet(),
) {
    /** Nothing to show yet, and no failure to report. */
    val isLoading: Boolean get() = catalog == null && error == null
}

enum class CatalogError {
    /** The website could not be reached. */
    Network,

    /** The website was reached but no flyers could be found on it. */
    InvalidData,

    /** Some language pages could not be read; their saved flyers are shown. */
    Partial,
}

/**
 * Single source of truth for the flyers, read straight from the Nabi ur Rahmah
 * website, so publishing a flyer on the site is all it takes to reach the app.
 *
 * Offline-first and quick:
 *  - the last catalogue is shown instantly from disk,
 *  - the language list appears as soon as the index page is read, and each
 *    language's flyers fill in as its page arrives (pages load in parallel),
 *  - unchanged pages (HTTP 304) are not parsed again,
 *  - a failure on one page never throws away what the others returned.
 */
class CatalogRepository(
    private val indexUrl: HttpUrl,
    private val website: WebsiteSource,
    private val store: CatalogStore,
    private val parser: NabiSiteParser = NabiSiteParser(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private val refreshMutex = Mutex()

    /** Shows the saved catalogue, then refreshes it if it is stale. */
    suspend fun load() {
        showSaved()
        refresh(force = false)
    }

    private suspend fun showSaved(): Catalog? {
        _state.value.catalog?.let { return it }
        val saved = withContext(ioDispatcher) { readSaved() } ?: return null
        _state.update { if (it.catalog == null) it.copy(catalog = saved) else it }
        return _state.value.catalog
    }

    /**
     * Reads the website again. Without [force], skips the work when the catalogue
     * was refreshed less than [MIN_REFRESH_INTERVAL_MS] ago. Concurrent calls are
     * coalesced into one.
     *
     * @return the problem, or null if everything was read.
     */
    suspend fun refresh(force: Boolean = true): CatalogError? {
        if (refreshMutex.isLocked) {
            refreshMutex.withLock { }
            return _state.value.error
        }
        return refreshMutex.withLock {
            val previous = showSaved()
            if (!force && previous != null && previous.languages.isNotEmpty() &&
                clock() - previous.fetchedAt < MIN_REFRESH_INTERVAL_MS
            ) {
                return@withLock null
            }
            _state.update { it.copy(isRefreshing = true) }
            val error = try {
                val (catalog, failures) = readWebsite(previous)
                _state.update { it.copy(catalog = catalog) }
                withContext(ioDispatcher) { store.write(CatalogJson.encodeCatalog(catalog)) }
                if (failures > 0) CatalogError.Partial else null
            } catch (e: CancellationException) {
                _state.update { it.copy(isRefreshing = false, loadingLanguages = emptySet()) }
                throw e
            } catch (e: IOException) {
                CatalogError.Network
            } catch (e: NoFlyersFoundException) {
                CatalogError.InvalidData
            }
            _state.update { it.copy(isRefreshing = false, loadingLanguages = emptySet(), error = error) }
            error
        }
    }

    /** @return the new catalogue and how many language pages failed. */
    private suspend fun readWebsite(previous: Catalog?): Pair<Catalog, Int> = coroutineScope {
        val indexPage = website.fetch(indexUrl)
        val index = withContext(parseDispatcher) { parser.parseIndex(indexPage.html, indexPage.url) }
        if (index.languages.isEmpty()) throw NoFlyersFoundException()

        val saved = previous?.languages?.associateBy { it.code }.orEmpty()

        // Show the language list at once, with each language's saved flyers until its page arrives.
        val initial = index.languages.map { site ->
            val keep = saved[site.language.code]?.takeIf { it.pageUrl == site.pageUrl && site.pageUrl != null }
            site.toLanguage(flyers = keep?.flyers ?: site.flyers)
        }
        val withPages = index.languages.filter { it.pageUrl != null }
        _state.update {
            it.copy(
                catalog = Catalog(fetchedAt = previous?.fetchedAt ?: 0, languages = initial),
                loadingLanguages = withPages.map { site -> site.language.code }.toSet(),
            )
        }

        val semaphore = Semaphore(MAX_PARALLEL_PAGES)
        val pageResults: Map<String, List<Flyer>?> = withPages.map { site ->
            async {
                val code = site.language.code
                val flyers = semaphore.withPermit { readLanguagePage(site, index.decorationKeys, saved[code]) }
                if (flyers != null) {
                    _state.update { state ->
                        state.copy(
                            catalog = state.catalog?.withFlyers(code, flyers),
                            loadingLanguages = state.loadingLanguages - code,
                        )
                    }
                } else {
                    _state.update { it.copy(loadingLanguages = it.loadingLanguages - code) }
                }
                code to flyers
            }
        }.awaitAll().toMap()

        val failures = pageResults.count { it.value == null }
        if (withPages.isNotEmpty() && failures == withPages.size && index.languages.all { it.pageUrl != null }) {
            // Nothing new could be read at all: treat it like being offline.
            throw IOException("No language page could be read")
        }

        val languages = removeSharedDecorations(
            initial.map { language -> pageResults[language.code]?.let { language.copy(flyers = it) } ?: language },
        )
        // Only a complete read counts as fresh; otherwise try again next launch.
        val fetchedAt = if (failures == 0) clock() else previous?.fetchedAt ?: 0
        Catalog(fetchedAt = fetchedAt, languages = languages) to failures
    }

    /** @return the page's flyers, or null if it could not be read. */
    private suspend fun readLanguagePage(site: SiteLanguage, decorationKeys: Set<String>, saved: Language?): List<Flyer>? {
        val url = site.pageUrl?.toHttpUrlOrNull() ?: return site.flyers
        return try {
            val page = website.fetch(url)
            if (page.unchanged && saved != null && saved.pageUrl == site.pageUrl && saved.flyers.isNotEmpty()) {
                saved.flyers
            } else {
                withContext(parseDispatcher) {
                    (site.flyers + parser.parseFlyers(page.html, page.url, decorationKeys)).distinctBy { it.id }
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun readSaved(): Catalog? =
        try {
            store.read()?.let(CatalogJson::decodeCatalog)?.takeIf { it.languages.isNotEmpty() }
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IOException) {
            null
        }

    private class NoFlyersFoundException : Exception()

    companion object {
        const val MIN_REFRESH_INTERVAL_MS = 15 * 60 * 1000L
        private const val MAX_PARALLEL_PAGES = 4

        /**
         * Images that appear on most language pages (a shared banner, a donation
         * poster) are page decoration, not flyers.
         */
        internal fun removeSharedDecorations(languages: List<Language>): List<Language> {
            val withFlyers = languages.filter { it.flyers.isNotEmpty() }
            if (withFlyers.size < 3) return languages
            val counts = withFlyers.flatMap { language -> language.flyers.map { it.id }.distinct() }
                .groupingBy { it }.eachCount()
            val shared = counts.filterValues { it >= 3 && it * 2 > withFlyers.size }.keys
            if (shared.isEmpty()) return languages
            return languages.map { language -> language.copy(flyers = language.flyers.filterNot { it.id in shared }) }
        }
    }
}

private fun SiteLanguage.toLanguage(flyers: List<Flyer>) = Language(
    code = language.code,
    name = language.name,
    nativeName = language.nativeName,
    rtl = language.rtl,
    pageUrl = pageUrl,
    flyers = flyers,
)

private fun Catalog.withFlyers(code: String, flyers: List<Flyer>): Catalog =
    copy(languages = languages.map { if (it.code == code) it.copy(flyers = flyers) else it })
