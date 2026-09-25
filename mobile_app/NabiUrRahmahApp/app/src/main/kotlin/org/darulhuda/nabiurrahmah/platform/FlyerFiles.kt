package org.darulhuda.nabiurrahmah.platform

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/** A downloaded flyer file that other apps can be given access to. */
data class LocalFile(
    val file: File,
    val mimeType: String,
    val uri: Uri,
) {
    val isPdf: Boolean get() = mimeType == MIME_PDF
}

const val MIME_PDF = "application/pdf"

/** Downloads flyer files into the app cache so they can be shared, saved or opened. */
class FlyerFiles(
    private val context: Context,
    private val client: OkHttpClient,
) {

    suspend fun download(url: String, baseName: String): LocalFile = withContext(Dispatchers.IO) {
        val extension = extensionOf(url)
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val file = File(dir, "${safeName(baseName)}.$extension")

        if (!file.isFile || file.length() == 0L) {
            val partial = File(dir, "${file.name}.part")
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
                val body = response.body ?: throw IOException("Empty response for $url")
                partial.outputStream().use { out -> body.byteStream().use { it.copyTo(out) } }
            }
            if (!partial.renameTo(file)) throw IOException("Could not store $file")
        }

        LocalFile(
            file = file,
            mimeType = mimeTypeOf(extension),
            uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file),
        )
    }

    private companion object {
        /** Must match res/xml/file_paths.xml. */
        const val CACHE_DIR = "shared"

        val KNOWN_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "pdf")

        fun extensionOf(url: String): String =
            url.toHttpUrlOrNull()?.pathSegments?.lastOrNull()
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?.takeIf { it in KNOWN_EXTENSIONS }
                ?: "jpg"

        fun mimeTypeOf(extension: String): String = when (extension) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> MIME_PDF
            else -> "image/jpeg"
        }

        fun safeName(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifEmpty { "flyer" }
    }
}
