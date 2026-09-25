package org.darulhuda.nabiurrahmah.data.youtube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.darulhuda.nabiurrahmah.data.model.Playlist
import org.darulhuda.nabiurrahmah.data.model.Video
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

/**
 * Reads YouTube playlists without an API key: from the playlist page (up to
 * 100 videos, with durations) or, as a fallback, the playlist's RSS feed
 * (latest 15 videos).
 */
object YouTubeParser {

    private val json = Json { ignoreUnknownKeys = true }

    /** @return the playlist, or null if the page has no playlist data (consent page, layout change). */
    fun parsePlaylistPage(html: String, playlistId: String): Playlist? {
        val data = extractInitialData(html) ?: return null
        val videos = LinkedHashMap<String, Video>()
        var title: String? = null

        walk(data) { obj ->
            (obj["playlistVideoRenderer"] as? JsonObject)?.let { renderer ->
                val id = renderer.string("videoId")
                val videoTitle = renderer["title"]?.text()
                val playable = (renderer["isPlayable"] as? JsonPrimitive)?.booleanOrNull != false
                if (id != null && videoTitle != null && playable && videoTitle !in UNAVAILABLE_TITLES) {
                    videos.putIfAbsent(id, Video(id, videoTitle, renderer.string("lengthSeconds")?.toIntOrNull()))
                }
            }
            if (title == null) {
                (obj["playlistMetadataRenderer"] as? JsonObject)?.string("title")?.let { title = it }
            }
        }
        if (videos.isEmpty()) return null
        return Playlist(playlistId, title ?: metaTitle(html) ?: "", videos.values.toList())
    }

    fun parseFeed(xml: String, playlistId: String): Playlist? {
        val document = Jsoup.parse(xml, "", Parser.xmlParser())
        val videos = document.select("entry").mapNotNull { entry ->
            val id = entry.selectFirst("yt|videoId")?.text()?.trim().orEmpty()
            val title = entry.selectFirst("title")?.text()?.trim().orEmpty()
            if (id.isEmpty() || title.isEmpty() || title in UNAVAILABLE_TITLES) null else Video(id, title)
        }.distinctBy { it.id }
        if (videos.isEmpty()) return null
        val title = document.selectFirst("feed > title")?.text()?.trim().orEmpty()
        return Playlist(playlistId, title, videos)
    }

    /** The playlist id in a YouTube link or embed, e.g. `watch?v=…&list=PL…` or `embed/videoseries?list=PL…`. */
    fun playlistIdFrom(url: String): String? {
        val parsed = url.trim().let { if (it.startsWith("//")) "https:$it" else it }.toHttpUrlOrNull() ?: return null
        val host = parsed.host.removePrefix("www.").removePrefix("m.")
        if (host != "youtube.com" && host != "youtube-nocookie.com" && host != "youtu.be") return null
        return parsed.queryParameter("list")?.takeIf(::isPublicPlaylistId)
    }

    /** Real playlists (PL…), channel uploads (UU…) and official ones (OL…); not personal mixes or watch-later. */
    private fun isPublicPlaylistId(id: String): Boolean =
        id.length >= 12 && id.all { it.isLetterOrDigit() || it == '-' || it == '_' } &&
            (id.startsWith("PL") || id.startsWith("UU") || id.startsWith("OL"))

    /** The `ytInitialData` object embedded in the page's script. */
    private fun extractInitialData(html: String): JsonElement? {
        val marker = html.indexOf("ytInitialData")
        if (marker < 0) return null
        val start = html.indexOf('{', marker)
        if (start < 0) return null
        val end = matchingBrace(html, start) ?: return null
        return try {
            json.parseToJsonElement(html.substring(start, end + 1))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** Index of the brace that closes the object opening at [start], skipping braces inside strings. */
    private fun matchingBrace(text: String, start: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            when {
                escaped -> escaped = false
                inString && c == '\\' -> escaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> depth++
                c == '}' -> if (--depth == 0) return i
            }
        }
        return null
    }

    private fun walk(element: JsonElement, visit: (JsonObject) -> Unit) {
        when (element) {
            is JsonObject -> {
                visit(element)
                element.values.forEach { walk(it, visit) }
            }
            is JsonArray -> element.forEach { walk(it, visit) }
            else -> Unit
        }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    /** YouTube text is either `{"simpleText": …}` or `{"runs": [{"text": …}, …]}`. */
    private fun JsonElement.text(): String? {
        val obj = this as? JsonObject ?: return null
        obj.string("simpleText")?.let { return it.trim() }
        val runs = obj["runs"] as? JsonArray ?: return null
        return runs.mapNotNull { (it as? JsonObject)?.string("text") }.joinToString("").trim().ifEmpty { null }
    }

    private fun metaTitle(html: String): String? =
        Regex("<meta\\s+property=\"og:title\"\\s+content=\"([^\"]+)\"").find(html)?.groupValues?.get(1)
            ?.let { Parser.unescapeEntities(it, true) }

    private val UNAVAILABLE_TITLES = setOf("[Private video]", "[Deleted video]", "[Unavailable video]")
}
