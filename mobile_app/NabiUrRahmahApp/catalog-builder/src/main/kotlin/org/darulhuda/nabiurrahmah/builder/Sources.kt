package org.darulhuda.nabiurrahmah.builder

import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.darulhuda.nabiurrahmah.data.source.CatalogStore
import org.darulhuda.nabiurrahmah.data.source.WebPage
import org.darulhuda.nabiurrahmah.data.source.WebsiteSource

/** Reads pages as the Internet Archive last saw them, for when the live site is down. */
class WaybackWebsiteSource(private val inner: WebsiteSource) : WebsiteSource {
    override suspend fun fetch(url: HttpUrl): WebPage {
        val page = inner.fetch(archived(url.toString()).toHttpUrl())
        // Links resolve against the original address, as on the live site.
        return page.copy(url = url)
    }

    companion object {
        /** `id_` asks for the page exactly as captured, without the archive's toolbar or rewritten links. */
        fun archived(url: String): String = "https://web.archive.org/web/2026id_/$url"
    }
}

class MemoryStore : CatalogStore {
    private var raw: String? = null
    override fun read() = raw
    override fun write(raw: String) {
        this.raw = raw
    }
}

/** Downloads a file, trying the live address first and the Internet Archive second. */
class Downloader(private val client: OkHttpClient) {

    suspend fun download(url: String, target: File): Boolean {
        if (target.isFile && target.length() > 0) return true
        target.parentFile?.mkdirs()
        for (candidate in listOf(url, WaybackWebsiteSource.archived(url))) {
            if (tryDownload(candidate, target)) return true
        }
        return false
    }

    private suspend fun tryDownload(url: String, target: File): Boolean = runInterruptible(Dispatchers.IO) {
        val tmp = File(target.path + ".part")
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val body = response.body
                val type = body?.contentType()?.toString().orEmpty()
                // The archive answers missing files with an HTML page; that is not a flyer.
                if (!response.isSuccessful || body == null || type.startsWith("text/")) return@runInterruptible false
                tmp.outputStream().use { out -> body.byteStream().use { it.copyTo(out) } }
            }
            tmp.length() > 0 && tmp.renameTo(target)
        } catch (e: IOException) {
            tmp.delete()
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
