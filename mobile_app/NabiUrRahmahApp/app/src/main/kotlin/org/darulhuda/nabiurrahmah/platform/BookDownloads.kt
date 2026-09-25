package org.darulhuda.nabiurrahmah.platform

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface DownloadStatus {
    data object NotDownloaded : DownloadStatus

    /** [progress] is 0..1, or null when the size is unknown. */
    data class Downloading(val progress: Float?) : DownloadStatus
    data class Downloaded(val file: File) : DownloadStatus
    data object Failed : DownloadStatus
}

/**
 * Books kept on the phone for offline reading. Downloads run in the app scope,
 * so they continue while the reader moves between screens.
 */
class BookDownloads(
    private val context: Context,
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
) {
    private val dir = File(context.filesDir, DIR)
    private val active = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    private val running = HashMap<String, Deferred<File>>()

    fun fileFor(url: String): File = File(dir, "${sha1(url)}.pdf")

    fun status(url: String): Flow<DownloadStatus> = active.map { statuses ->
        statuses[url] ?: fileFor(url).takeIf { it.isFile && it.length() > 0 }?.let { DownloadStatus.Downloaded(it) }
            ?: DownloadStatus.NotDownloaded
    }

    /** Starts (or joins) the download and waits for the file. */
    suspend fun get(url: String): File {
        fileFor(url).takeIf { it.isFile && it.length() > 0 }?.let { return it }
        return start(url).await()
    }

    fun start(url: String): Deferred<File> = synchronized(running) {
        running[url]?.takeIf { it.isActive }?.let { return it }
        val result = CompletableDeferred<File>()
        running[url] = result
        scope.launch(Dispatchers.IO) {
            try {
                result.complete(download(url))
            } catch (e: Exception) {
                active.update { it + (url to DownloadStatus.Failed) }
                result.completeExceptionally(e)
            } finally {
                synchronized(running) { running.remove(url) }
            }
        }
        result
    }

    fun delete(url: String) {
        fileFor(url).delete()
        active.update { it - url }
    }

    /** A content URI other apps can read, for sharing or opening. */
    fun shareable(file: File, displayName: String): LocalFile {
        val named = File(context.cacheDir, "shared/${displayName.replace(Regex("[^\\p{L}\\p{N}._ -]+"), "-").take(80)}.pdf")
        named.parentFile?.mkdirs()
        if (!named.isFile || named.length() != file.length()) file.copyTo(named, overwrite = true)
        return LocalFile(named, MIME_PDF, FileProvider.getUriForFile(context, "${context.packageName}.files", named))
    }

    private fun download(url: String): File {
        dir.mkdirs()
        val target = fileFor(url)
        val partial = File(dir, "${target.name}.part")
        active.update { it + (url to DownloadStatus.Downloading(null)) }
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            val body = response.body ?: throw IOException("Empty response for $url")
            val total = body.contentLength().takeIf { it > 0 }
            var read = 0L
            var reported = -1
            body.byteStream().use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (scope.isActive) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        read += count
                        val percent = total?.let { (read * 100 / it).toInt() } ?: -1
                        if (percent != reported) {
                            reported = percent
                            active.update { it + (url to DownloadStatus.Downloading(total?.let { t -> read.toFloat() / t })) }
                        }
                    }
                }
            }
        }
        if (!partial.renameTo(target)) throw IOException("Could not store $target")
        active.update { it + (url to DownloadStatus.Downloaded(target)) }
        return target
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val DIR = "books"
    }
}
