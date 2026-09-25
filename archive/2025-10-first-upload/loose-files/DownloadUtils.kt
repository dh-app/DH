package udupi.core.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap

fun enqueueDownload(context: Context, url:String, subDir:String, filename:String): Long {
  val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
  val ext = MimeTypeMap.getFileExtensionFromUrl(url).ifBlank { "pdf" }
  val name = if (filename.endsWith(".$ext")) filename else "$filename.$ext"

  val req = DownloadManager.Request(Uri.parse(url))
    .setTitle(name)
    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DarulHudaUdupi/$subDir/$name")
    .setAllowedOverMetered(true)
    .setAllowedOverRoaming(true)

  return dm.enqueue(req)
}
