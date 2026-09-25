package org.darulhuda.nabiurrahmah.platform

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Saves flyers where people expect them: images in Pictures/Nabi ur Rahmah
 * (visible in the gallery), PDFs in Download/Nabi ur Rahmah.
 */
class GallerySaver(private val context: Context) {

    /** Android 9 and older need the storage permission to write shared folders. */
    val needsPermission: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED

    suspend fun save(source: LocalFile) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveToMediaStore(source) else saveLegacy(source)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(source: LocalFile) {
        val collection = if (source.isPdf) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, source.file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, source.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${publicDirectory(source)}/$FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: throw IOException("Could not create ${source.file.name}")
        try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("Could not write ${source.file.name}")
            out.use { stream -> source.file.inputStream().use { it.copyTo(stream) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    @Suppress("DEPRECATION") // The only way to write shared storage before Android 10.
    private fun saveLegacy(source: LocalFile) {
        val root = Environment.getExternalStoragePublicDirectory(publicDirectory(source))
        val dir = File(root, FOLDER)
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Could not create $dir")
        val target = File(dir, source.file.name)
        source.file.copyTo(target, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(source.mimeType), null)
    }

    private fun publicDirectory(source: LocalFile): String =
        if (source.isPdf) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_PICTURES

    private companion object {
        const val FOLDER = "Nabi ur Rahmah"
    }
}
