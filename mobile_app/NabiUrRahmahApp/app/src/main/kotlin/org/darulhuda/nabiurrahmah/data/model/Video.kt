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
    /** A vertical YouTube Short. */
    val isShort: Boolean = false,
) {
    /** 16:9 (320×180) for videos; the full vertical frame (720×1280) for Shorts. */
    val thumbnailUrl: String
        get() = if (isShort) "https://i.ytimg.com/vi/$id/oardefault.jpg" else "https://i.ytimg.com/vi/$id/mqdefault.jpg"

    /** Fallback when a Short has no vertical thumbnail yet. */
    val fallbackThumbnailUrl: String get() = "https://i.ytimg.com/vi/$id/hqdefault.jpg"

    fun watchUrl(playlistId: String? = null): String =
        if (isShort) {
            "https://www.youtube.com/shorts/$id"
        } else {
            "https://www.youtube.com/watch?v=$id" + (playlistId?.let { "&list=$it" } ?: "")
        }
}
