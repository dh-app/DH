package udupi.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object Http {
  lateinit var client: OkHttpClient
  fun init(cacheDir: File) {
    client = OkHttpClient.Builder()
      .cache(Cache(File(cacheDir, "http_cache"), 20L * 1024 * 1024))
      .build()
  }
}

suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
  val req = Request.Builder()
    .url(url)
    .header("User-Agent", "NabiUrRahmahApp/1.0 (Android)")
    .build()
  Http.client.newCall(req).execute().use { resp ->
    if (!resp.isSuccessful) error("HTTP ${resp.code}")
    resp.body?.string() ?: error("Empty body")
  }
}
