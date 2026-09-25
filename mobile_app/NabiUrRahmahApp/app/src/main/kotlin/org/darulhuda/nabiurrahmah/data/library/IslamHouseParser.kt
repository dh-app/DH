package org.darulhuda.nabiurrahmah.data.library

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup

/** A published IslamHouse item (one language). */
data class IslamHouseItem(
    val id: String,
    /** Shared by translations of the same work. */
    val groupId: String,
    val title: String,
    val language: String,
    val author: String?,
    val description: String?,
    val files: List<BookFile>,
)

data class IslamHousePage(val items: List<IslamHouseItem>, val totalPages: Int?)

/**
 * Reads IslamHouse's public API and, as a fallback, its web pages. The API has
 * changed shape over the years, so parsing looks for fields by meaning rather
 * than by one exact layout.
 */
object IslamHouseParser {

    private val json = Json { ignoreUnknownKeys = true }
    private val READABLE = setOf("pdf", "epub")

    fun parseApi(body: String): IslamHousePage {
        val root = json.parseToJsonElement(body)
        val array = when (root) {
            is JsonArray -> root
            is JsonObject -> (root["data"] ?: root["items"]) as? JsonArray
            else -> null
        } ?: JsonArray(emptyList())
        val links = (root as? JsonObject)?.get("links") as? JsonObject
        val totalPages = listOf("pages_number", "total_pages", "last_page", "pages")
            .firstNotNullOfOrNull { (links?.get(it) as? JsonPrimitive)?.intOrNull }
        return IslamHousePage(array.mapNotNull { (it as? JsonObject)?.let(::parseItem) }, totalPages)
    }

    private fun parseItem(item: JsonObject): IslamHouseItem? {
        val id = item.text("id") ?: return null
        val title = item.text("title")?.clean()?.takeIf { it.isNotEmpty() } ?: return null
        val files = (item["attachments"] as? JsonArray).orEmpty().mapNotNull { element ->
            val attachment = element as? JsonObject ?: return@mapNotNull null
            val url = attachment.text("url")?.let(::https) ?: return@mapNotNull null
            val format = (attachment.text("extension_type") ?: extensionOf(url))?.lowercase() ?: return@mapNotNull null
            if (format !in READABLE) return@mapNotNull null
            BookFile(url = url, format = format, size = attachment.text("size"), label = attachment.text("description")?.clean())
        }
        if (files.isEmpty()) return null
        val language = item.text("translated_language") ?: item.text("source_language") ?: item.text("language")
            ?: languageFromFileUrl(files.first().url) ?: return null
        val people = (item["prepared_by"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        val author = (people.filter { it.text("kind") == "author" }.ifEmpty { people })
            .mapNotNull { it.text("title")?.clean() }.distinct().joinToString(", ").ifEmpty { null }
        return IslamHouseItem(
            id = id,
            groupId = item.text("source_id")?.takeIf { it != "0" && it.isNotBlank() } ?: id,
            title = title,
            language = language.lowercase(),
            author = author,
            description = item.text("description")?.let(::stripHtml)?.takeIf { it.isNotBlank() },
            files = files,
        )
    }

    /** Links to item pages on a category page, e.g. `/en/books/2800006/`. */
    fun parseCategoryHtml(html: String, pageUrl: HttpUrl): List<String> =
        Jsoup.parse(html, pageUrl.toString()).select("a[href]")
            .map { it.absUrl("href").substringBefore('#') }
            .filter { ITEM_PATH.containsMatchIn(it) && it.contains("islamhouse.com") }
            .distinct()

    fun parseItemHtml(html: String, pageUrl: HttpUrl): IslamHouseItem? {
        val document = Jsoup.parse(html, pageUrl.toString())
        val files = document.select("a[href]").mapNotNull { anchor ->
            val url = https(anchor.absUrl("href"))
            val format = extensionOf(url)?.lowercase()
            if (format in READABLE) BookFile(url, format!!, label = anchor.text().clean().ifEmpty { null }) else null
        }.distinctBy { it.url }
        if (files.isEmpty()) return null
        val title = (document.selectFirst("meta[property=og:title]")?.attr("content") ?: document.selectFirst("h1")?.text())
            ?.clean()?.takeIf { it.isNotEmpty() } ?: return null
        val id = ITEM_PATH.find(pageUrl.toString())?.groupValues?.get(2) ?: pageUrl.toString()
        return IslamHouseItem(
            id = id,
            groupId = id,
            title = title,
            language = languageFromFileUrl(files.first().url) ?: pageUrl.pathSegments.firstOrNull() ?: "en",
            author = null,
            description = document.selectFirst("meta[name=description]")?.attr("content")?.clean()?.ifEmpty { null },
            files = files,
        )
    }

    /** Translations of one work become one [Book]; the order of first appearance is kept. */
    fun group(items: List<IslamHouseItem>): List<Book> =
        items.groupBy { it.groupId }.map { (groupId, editions) ->
            Book(
                id = "ih-$groupId",
                editions = editions.distinctBy { it.language }.map { item ->
                    Edition(item.id, item.language, item.title, item.author, item.description, item.files)
                },
            )
        }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.trim()

    private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()

    private fun String.clean(): String = replace(Regex("\\s+"), " ").trim()

    private fun stripHtml(text: String): String = Jsoup.parse(text).text().clean()

    private fun https(url: String): String = if (url.startsWith("http://")) "https://" + url.removePrefix("http://") else url

    private fun extensionOf(url: String): String? =
        url.toHttpUrlOrNull()?.pathSegments?.lastOrNull()?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }

    /** IslamHouse stores files under `/data/<language>/…`. */
    private fun languageFromFileUrl(url: String): String? = Regex("/data/([a-z]{2,3})/").find(url)?.groupValues?.get(1)

    private val ITEM_PATH = Regex("islamhouse\\.com/([a-z]{2,3})/(?:books|articles|pdf)/(\\d+)")
}
