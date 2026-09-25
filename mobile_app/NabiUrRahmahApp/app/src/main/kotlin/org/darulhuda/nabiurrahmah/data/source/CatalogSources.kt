package org.darulhuda.nabiurrahmah.data.source

import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** An HTML page from the website. */
data class WebPage(
    /** Final URL after redirects; relative links resolve against it. */
    val url: HttpUrl,
    val html: String,
    /** The server confirmed (HTTP 304) that the page is unchanged since the last visit. */
    val unchanged: Boolean,
)

fun interface WebsiteSource {
    suspend fun fetch(url: HttpUrl): WebPage
}

/**
 * Fetches pages with conditional requests: OkHttp's cache sends ETag /
 * Last-Modified validators, so an unchanged page costs a tiny 304 response.
 */
class HttpWebsiteSource(private val client: OkHttpClient) : WebsiteSource {

    override suspend fun fetch(url: HttpUrl): WebPage = runInterruptible(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html,application/xhtml+xml")
            .cacheControl(CacheControl.Builder().noCache().build())
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            val body = response.body ?: throw IOException("Empty response for $url")
            if (body.contentLength() > MAX_PAGE_BYTES) throw IOException("Page too large: $url")
            WebPage(
                url = response.request.url,
                html = body.string(),
                unchanged = response.networkResponse?.code == HTTP_NOT_MODIFIED,
            )
        }
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
        const val MAX_PAGE_BYTES = 8L * 1024 * 1024
    }
}

/** The last catalogue read from the website, kept for instant start-up and offline use. */
interface CatalogStore {
    fun read(): String?
    fun write(raw: String)
}

class FileCatalogStore(private val file: File) : CatalogStore {

    override fun read(): String? = file.takeIf { it.isFile }?.readText()

    override fun write(raw: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.writeText(raw)
        if (!tmp.renameTo(file)) {
            file.delete()
            if (!tmp.renameTo(file)) throw IOException("Could not save catalogue to $file")
        }
    }
}
