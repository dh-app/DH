package org.darulhuda.nabiurrahmah.data.model

import kotlinx.serialization.Serializable

/**
 * Everything read from the Nabi ur Rahmah website. Cached on the device as JSON, so
 * parsing is lenient: unknown fields are ignored and missing ones take defaults,
 * which lets the format change between app versions.
 */
@Serializable
data class Catalog(
    val schemaVersion: Int = 1,
    /** When the whole site was last read successfully (epoch millis). */
    val fetchedAt: Long = 0,
    val languages: List<Language> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
) {
    val flyerCount: Int get() = languages.sumOf { it.flyers.size }

    fun language(code: String): Language? = languages.firstOrNull { it.code == code }
}

@Serializable
data class Language(
    val code: String,
    val name: String,
    val nativeName: String = name,
    val rtl: Boolean = false,
    /** The website page listing this language's flyers, when it has one. */
    val pageUrl: String? = null,
    val flyers: List<Flyer> = emptyList(),
)

@Serializable
data class Flyer(
    val id: String,
    /** Full-size image shown in the viewer (for a PDF-only flyer, the PDF itself). */
    val image: String,
    val title: String? = null,
    /** Smaller rendition for grids. */
    val thumbnail: String? = null,
    val pdf: String? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    val previewUrl: String get() = thumbnail ?: image

    /** Width / height when both are known, so grids can reserve space before the image loads. */
    val aspectRatio: Float?
        get() = if (width != null && height != null && width > 0 && height > 0) {
            width.toFloat() / height
        } else {
            null
        }
}
