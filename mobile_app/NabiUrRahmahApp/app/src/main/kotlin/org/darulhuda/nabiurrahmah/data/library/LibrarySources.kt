package org.darulhuda.nabiurrahmah.data.library

import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.darulhuda.nabiurrahmah.data.site.KnownLanguages
import org.darulhuda.nabiurrahmah.data.site.NabiSiteParser

/** Fetches text over HTTP. Returns null for HTTP errors; throws [IOException] when offline. */
fun interface TextFetcher {
    suspend fun get(url: HttpUrl): String?
}

/** Somewhere books come from. */
fun interface BookSource {
    /** @throws IOException if the source could not be reached at all. */
    suspend fun books(): List<Book>
}

/**
 * An IslamHouse category, in every language it is published in. Tries the
 * public API first (several URL forms it has used) and falls back to reading
 * the website's category and item pages.
 */
class IslamHouseCategorySource(
    private val categoryId: Int,
    private val fetch: TextFetcher,
    private val interfaceLanguage: String = "en",
    private val apiKey: String = PUBLIC_API_KEY,
) : BookSource {

    override suspend fun books(): List<Book> {
        var reachedServer = false
        for (variant in apiVariants()) {
            val items = try {
                readApi(variant)
            } catch (e: SerializationException) {
                reachedServer = true
                null
            } catch (e: IllegalArgumentException) {
                reachedServer = true
                null
            }
            if (items == null) continue
            reachedServer = true
            if (items.isNotEmpty()) return IslamHouseParser.group(items)
        }
        val fromWebsite = readWebsite()
        if (fromWebsite.isNotEmpty() || reachedServer) return IslamHouseParser.group(fromWebsite)
        throw IOException("IslamHouse could not be reached")
    }

    private fun apiVariants(): List<(Int) -> String> {
        val base = "https://api3.islamhouse.com/v3/$apiKey/main/get-category-items/$categoryId"
        return listOf(
            { page -> "$base/showall/showall/$interfaceLanguage/$page/$PAGE_SIZE/json" },
            { page -> "$base/showall/$interfaceLanguage/showall/$page/$PAGE_SIZE/json" },
            { page -> "$base/books/showall/$interfaceLanguage/$page/$PAGE_SIZE/json" },
        )
    }

    /** @return the items, or null if this URL form isn't served. */
    private suspend fun readApi(url: (Int) -> String): List<IslamHouseItem>? {
        val first = fetch.get(url(1).toHttpUrl())?.let(IslamHouseParser::parseApi) ?: return null
        val pages = (first.totalPages ?: 1).coerceAtMost(MAX_PAGES)
        if (pages <= 1 || first.items.isEmpty()) return first.items
        val rest = coroutineScope {
            (2..pages).map { page ->
                async { fetch.get(url(page).toHttpUrl())?.let(IslamHouseParser::parseApi)?.items.orEmpty() }
            }.awaitAll()
        }
        return first.items + rest.flatten()
    }

    private suspend fun readWebsite(): List<IslamHouseItem> = coroutineScope {
        val itemUrls = LinkedHashSet<String>()
        for (page in 1..MAX_PAGES) {
            val url = "https://islamhouse.com/$interfaceLanguage/category/$categoryId/showall/showall/$page/".toHttpUrl()
            val found = fetch.get(url)?.let { IslamHouseParser.parseCategoryHtml(it, url) }.orEmpty()
            if (!itemUrls.addAll(found)) break
            if (itemUrls.size >= MAX_WEB_ITEMS) break
        }
        val semaphore = Semaphore(6)
        itemUrls.take(MAX_WEB_ITEMS).map { link ->
            async {
                semaphore.withPermit {
                    val url = link.toHttpUrl()
                    try {
                        fetch.get(url)?.let { IslamHouseParser.parseItemHtml(it, url) }
                    } catch (e: IOException) {
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }

    companion object {
        /** The key IslamHouse publishes for its public, read-only API. */
        const val PUBLIC_API_KEY = "paV29H2gm56kvLPy"
        private const val PAGE_SIZE = 50
        private const val MAX_PAGES = 20
        private const val MAX_WEB_ITEMS = 200
    }
}

/**
 * Every PDF linked from a web page, e.g. a page on darulhudaudupi.org where new
 * books are uploaded. Publishing a PDF there adds it to the app.
 */
class WebPagePdfSource(
    private val pageUrl: HttpUrl,
    private val fetch: TextFetcher,
    private val parser: NabiSiteParser = NabiSiteParser(),
) : BookSource {

    override suspend fun books(): List<Book> {
        val html = fetch.get(pageUrl) ?: return emptyList()
        return parser.parseFlyers(html, pageUrl).mapNotNull { flyer ->
            val pdf = flyer.pdf ?: return@mapNotNull null
            val name = pdf.substringAfterLast('/').substringBeforeLast('.')
            val title = flyer.title ?: name.replace(Regex("[-_]+"), " ").trim().replaceFirstChar { it.titlecase() }
            val language = KnownLanguages.match(title)?.code ?: KnownLanguages.matchSlug(name)?.code ?: "en"
            Book(
                id = "web-${flyer.id}",
                editions = listOf(Edition(flyer.id, language, title, files = listOf(BookFile(pdf)))),
            )
        }
    }
}

/**
 * Parses a shelf's configured sources: `islamhouse:795`, `page:https://…`,
 * separated by commas. Unknown entries are ignored.
 */
fun bookSourcesFrom(spec: String, fetch: TextFetcher, interfaceLanguage: String = "en"): List<BookSource> =
    spec.split(',').map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { entry ->
        val kind = entry.substringBefore(':')
        val value = entry.substringAfter(':', "")
        when (kind) {
            "islamhouse" -> value.toIntOrNull()?.let { IslamHouseCategorySource(it, fetch, interfaceLanguage) }
            "page" -> runCatching { value.toHttpUrl() }.getOrNull()?.let { WebPagePdfSource(it, fetch) }
            else -> null
        }
    }
