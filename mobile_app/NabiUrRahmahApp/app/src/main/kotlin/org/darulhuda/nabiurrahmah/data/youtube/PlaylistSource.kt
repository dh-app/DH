package org.darulhuda.nabiurrahmah.data.youtube

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import okhttp3.OkHttpClient
import okhttp3.Request
import org.darulhuda.nabiurrahmah.data.model.Playlist

fun interface PlaylistSource {
    /** @throws IOException if the playlist could not be read. */
    suspend fun playlist(id: String): Playlist
}

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36"

class YouTubePlaylistSource(private val client: OkHttpClient) : PlaylistSource {

    override suspend fun playlist(id: String): Playlist =
        get("https://www.youtube.com/playlist?list=$id")?.let { YouTubeParser.parsePlaylistPage(it, id) }
            ?: get("https://www.youtube.com/feeds/videos.xml?playlist_id=$id")?.let { YouTubeParser.parseFeed(it, id) }
            ?: throw IOException("Playlist $id could not be read")

    private suspend fun get(url: String): String? = runInterruptible(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept-Language", "en")
            // A desktop browser gets the plain page layout the parser reads.
            .header("User-Agent", DESKTOP_USER_AGENT)
            // Skips the cookie-consent interstitial shown in some regions.
            .header("Cookie", "SOCS=CAI; CONSENT=YES+")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }
}
