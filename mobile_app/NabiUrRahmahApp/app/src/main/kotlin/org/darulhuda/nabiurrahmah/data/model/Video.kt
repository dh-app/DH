package org.darulhuda.nabiurrahmah.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val videos: List<Video> = emptyList(),
) {
    val url: String get() = "https://www.youtube.com/playlist?list=$id"
}

@Serializable
data class Video(
    val id: String,
    val title: String,
    val durationSeconds: Int? = null,
) {
    /** 16:9, 320×180: right for cards and rows. */
    val thumbnailUrl: String get() = "https://i.ytimg.com/vi/$id/mqdefault.jpg"

    fun watchUrl(playlistId: String? = null): String =
        "https://www.youtube.com/watch?v=$id" + (playlistId?.let { "&list=$it" } ?: "")
}
