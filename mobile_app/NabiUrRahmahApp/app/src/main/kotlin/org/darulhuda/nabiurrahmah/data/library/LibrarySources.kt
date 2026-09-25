package org.darulhuda.nabiurrahmah.data.library

import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
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
 * PDFs attached to a GitHub release, e.g. `github:dh-app/DH@prophetic-biography`.
 * Dropping a PDF onto the release page publishes it to the app; no code change.
 *
 * File names carry the details: "The Sealed Nectar - Urdu.pdf" becomes the Urdu
 * edition of "The Sealed Nectar", grouped with other files of the same title.
 */
class GitHubReleaseSource(
    private val repository: String,
    private val tag: String,
    private val fetch: TextFetcher,
) : BookSource {

    override suspend fun books(): List<Book> {
        val url = "https://api.github.com/repos/$repository/releases/tags/$tag".toHttpUrl()
        // No release yet (HTTP 404) simply means nothing has been uploaded.
        val body = fetch.get(url) ?: return emptyList()
        val assets = try {
            (Json.parseToJsonElement(body) as? JsonObject)?.get("assets") as? JsonArray
        } catch (e: IllegalArgumentException) {
            null
        } ?: return emptyList()

        val files = assets.mapNotNull { element ->
            val asset = element as? JsonObject ?: return@mapNotNull null
            val name = (asset["name"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val download = (asset["browser_download_url"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            if (!name.endsWith(".pdf", ignoreCase = true)) return@mapNotNull null
            val bytes = (asset["size"] as? JsonPrimitive)?.longOrNull
            ReleaseFile(describe(name), download, bytes?.let(::formatSize))
        }
        return files.groupBy { it.title.lowercase() }.map { (key, editions) ->
            Book(
                id = "gh-$tag-${key.hashCode().toUInt().toString(36)}",
                editions = editions.distinctBy { it.language }.map { file ->
                    Edition(
                        id = file.url,
                        language = file.language,
                        title = file.title,
                        files = listOf(BookFile(file.url, size = file.size)),
                    )
                },
            )
        }
    }

    private class ReleaseFile(val title: String, val language: String, val url: String, val size: String?) {
        constructor(described: Pair<String, String>, url: String, size: String?) : this(described.first, described.second, url, size)
    }

    companion object {
        /** "The_Sealed-Nectar (Urdu).pdf" → ("The Sealed Nectar", "ur"). */
        internal fun describe(fileName: String): Pair<String, String> {
            val base = fileName.substringBeforeLast('.').replace(Regex("[_.]+"), " ").replace(Regex("\\s+"), " ").trim()
            val language = KnownLanguages.mentionedIn(base)
            val title = if (language == null) {
                base
            } else {
                listOf(language.name, language.nativeName).plus(language.aliases)
                    .fold(base) { text, name -> text.replace(Regex("(?i)[\\s(\\[-]*\\b${Regex.escape(name)}\\b[\\s)\\]]*"), " ") }
                    .replace(Regex("\\s*[-–—]\\s*$"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .ifEmpty { base }
            }
            return title to (language?.code ?: "en")
        }

        internal fun formatSize(bytes: Long): String = when {
            bytes >= 1_000_000 -> "%.1f MB".format(java.util.Locale.ROOT, bytes / 1_000_000.0)
            else -> "${(bytes / 1_000).coerceAtLeast(1)} KB"
        }
    }
}

/**
 * Parses a shelf's configured sources: `islamhouse:795`, `page:https://…`,
 * `github:owner/repo@tag`,
 * separated by commas. Unknown entries are ignored.
 */
fun bookSourcesFrom(spec: String, fetch: TextFetcher, interfaceLanguage: String = "en"): List<BookSource> =
    spec.split(',').map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { entry ->
        val kind = entry.substringBefore(':')
        val value = entry.substringAfter(':', "")
        when (kind) {
            "islamhouse" -> value.toIntOrNull()?.let { IslamHouseCategorySource(it, fetch, interfaceLanguage) }
            "page" -> runCatching { value.toHttpUrl() }.getOrNull()?.let { WebPagePdfSource(it, fetch) }
            "github" -> value.split('@').takeIf { it.size == 2 && it[0].count { c -> c == '/' } == 1 }
                ?.let { (repository, tag) -> GitHubReleaseSource(repository, tag, fetch) }
            else -> null
        }
    }
