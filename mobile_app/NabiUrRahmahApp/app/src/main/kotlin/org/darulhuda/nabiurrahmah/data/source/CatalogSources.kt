package org.darulhuda.nabiurrahmah.data.source

import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Fetches the latest catalogue JSON. */
fun interface CatalogRemoteSource {
    suspend fun fetch(): String
}

class HttpCatalogSource(
    private val client: OkHttpClient,
    private val url: HttpUrl,
) : CatalogRemoteSource {

    override suspend fun fetch(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            response.body?.string() ?: throw IOException("Empty response for $url")
        }
    }
}

/** Local copies of the catalogue: the last one downloaded, and the one shipped in the APK. */
interface CatalogStore {
    fun readCached(): String?
    fun writeCached(raw: String)
    fun readBundled(): String?
}

class FileCatalogStore(
    private val file: File,
    private val bundled: () -> String?,
) : CatalogStore {

    override fun readCached(): String? = file.takeIf { it.isFile }?.readText()

    override fun writeCached(raw: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.writeText(raw)
        if (!tmp.renameTo(file)) {
            file.delete()
            if (!tmp.renameTo(file)) throw IOException("Could not save catalogue to $file")
        }
    }

    override fun readBundled(): String? = bundled()
}
