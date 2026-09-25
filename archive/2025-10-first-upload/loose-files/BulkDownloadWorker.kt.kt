package org.darulhuda.udupi.core.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * 📥 BulkDownloadWorker — Handles background flyer downloads for selected language.
 * Runs safely with WorkManager and shows foreground notifications.
 */
class BulkDownloadWorker(
  private val ctx: Context,
  params: WorkerParameters
) : CoroutineWorker(ctx, params) {

  private val notificationId = 1001
  private val channelId = "flyer_downloads"

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    try {
      val langName = inputData.getString("lang_name") ?: return@withContext Result.failure()
      val langUrl = inputData.getString("lang_url") ?: return@withContext Result.failure()

      showNotification("Downloading Flyers", "Starting downloads for $langName…")

      val dir = File(ctx.filesDir, "flyers/$langName").apply { mkdirs() }

      val html = URL(langUrl).readText()
      val urls = Regex("https?://[^\\s\"']+\\.pdf").findAll(html).map { it.value }.toList()

      urls.forEachIndexed { i, url ->
        val fileName = url.substringAfterLast("/")
        val file = File(dir, fileName)
        showNotification(
          "Downloading Flyers",
          "Downloading ${i + 1}/${urls.size}: $fileName"
        )
        URL(url).openStream().use { input ->
          file.outputStream().use { output -> input.copyTo(output) }
        }
      }

      showNotification("Download Complete", "All flyers for $langName downloaded successfully.")
      Result.success()
    } catch (e: Exception) {
      e.printStackTrace()
      showNotification("Download Failed", "An error occurred while downloading flyers.")
      Result.failure()
    }
  }

  /**
   * 🪶 Shows a small foreground notification for download progress or status.
   */
  private fun showNotification(title: String, message: String) {
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Flyer Downloads",
        NotificationManager.IMPORTANCE_LOW
      )
      nm.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(ctx, channelId)
      .setContentTitle(title)
      .setContentText(message)
      .setSmallIcon(android.R.drawable.stat_sys_download_done)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .build()

    nm.notify(notificationId, notification)
  }
}
