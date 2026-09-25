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
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val name = safeName(baseName)
        val file = dir.listFiles { f -> f.nameWithoutExtension == name && f.extension in KNOWN_EXTENSIONS && f.length() > 0 }
            ?.firstOrNull()
            ?: fetch(url, dir, name)
        LocalFile(
            file = file,
            mimeType = mimeTypeOf(file.extension),
            uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file),
        )
    }

    private fun fetch(url: String, dir: File, name: String): File {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            val body = response.body ?: throw IOException("Empty response for $url")
            // Links such as Google Drive's have no extension; the server says what the file is.
            val extension = extensionOf(url) ?: extensionOfType(body.contentType()?.toString()) ?: "jpg"
            val file = File(dir, "$name.$extension")
            val partial = File(dir, "${file.name}.part")
            partial.outputStream().use { out -> body.byteStream().use { it.copyTo(out) } }
            if (!partial.renameTo(file)) throw IOException("Could not store $file")
            file
        }
    }

    private companion object {
        /** Must match res/xml/file_paths.xml. */
        const val CACHE_DIR = "shared"

        val KNOWN_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "pdf")

        fun extensionOf(url: String): String? =
            url.toHttpUrlOrNull()?.pathSegments?.lastOrNull()
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?.takeIf { it in KNOWN_EXTENSIONS }

        fun extensionOfType(contentType: String?): String? = when (contentType?.substringBefore(';')?.trim()?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            MIME_PDF -> "pdf"
            else -> null
        }

        fun mimeTypeOf(extension: String): String = when (extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> MIME_PDF
            else -> "image/jpeg"
        }

        fun safeName(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifEmpty { "flyer" }
    }
}
