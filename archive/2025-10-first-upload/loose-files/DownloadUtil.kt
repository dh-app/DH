package org.darulhuda.udupi.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Uses DownloadManager and avoids legacy WRITE_EXTERNAL_STORAGE.
 * - On Android 10+ (API 29+): public Downloads/NabiUrRahmah/...
 * - On Android 6–9: app-specific external files dir (no permission needed)
 */
private fun Context.enqueueDownloadSmart(url: String, name: String) {
    val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val req = DownloadManager.Request(Uri.parse(url))
        .setTitle(name)
        .setDescription("Downloading flyer")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Public Downloads subfolder (visible in Files app)
        req.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            "NabiUrRahmah/$name"
        )
    } else {
        // App-specific external (no runtime permission needed)
        req.setDestinationInExternalFilesDir(
            this,
            Environment.DIRECTORY_DOWNLOADS,
            "NabiUrRahmah/$name"
        )
    }

    dm.enqueue(req)
}

@Composable
fun downloadFile(url: String, suggestedName: String) {
    val ctx = LocalContext.current
    ctx.enqueueDownloadSmart(url, suggestedName)
}
