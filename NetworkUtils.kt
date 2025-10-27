package org.darulhuda.udupi.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

/**
 * 🌐 NetworkUtils.kt
 * Safe coroutine-based HTML utilities for content extraction.
 * Used for scraping or lightweight content preview when JSON API is not available.
 */

/**
 * Fetch full HTML string from a URL.
 */
suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
    try {
        Jsoup.connect(url)
            .timeout(15_000)
            .userAgent("Mozilla/5.0 (Android NabiUrRahmahApp)")
            .get()
            .html()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

/**
 * Extracts basic metadata from an HTML page.
 * Returns map containing title, description, and keywords.
 */
suspend fun extractMetaInfo(url: String): Map<String, String> = withContext(Dispatchers.IO) {
    try {
        val doc: Document = Jsoup.connect(url)
            .timeout(15_000)
            .userAgent("Mozilla/5.0 (Android NabiUrRahmahApp)")
            .get()

        val title = doc.title().orEmpty()
        val desc = doc.select("meta[name=description]").attr("content").orEmpty()
        val keywords = doc.select("meta[name=keywords]").attr("content").orEmpty()

        mapOf(
            "title" to title,
            "description" to desc,
            "keywords" to keywords
        )
    } catch (e: Exception) {
        e.printStackTrace()
        emptyMap()
    }
}

/**
 * Extracts all <img> URLs from a page — ideal for flyers auto-detection.
 */
suspend fun extractImageUrls(url: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val doc: Document = Jsoup.connect(url)
            .timeout(15_000)
            .userAgent("Mozilla/5.0 (Android NabiUrRahmahApp)")
            .get()

        val imgs: Elements = doc.select("img[src]")
        imgs.map { it.absUrl("src") }.filter { it.isNotBlank() }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
