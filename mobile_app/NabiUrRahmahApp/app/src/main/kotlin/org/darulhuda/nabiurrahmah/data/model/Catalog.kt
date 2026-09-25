package org.darulhuda.nabiurrahmah.data.model

import kotlinx.serialization.Serializable

/**
 * Everything the app shows, published as one JSON document (see /content/README.md).
 * Parsing is lenient: unknown fields are ignored so the format can grow without
 * breaking installed versions of the app.
 */
@Serializable
data class Catalog(
    val schemaVersion: Int = 1,
    val updatedAt: String = "",
    val languages: List<Language> = emptyList(),
    val about: About = About(),
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
    val flyers: List<Flyer> = emptyList(),
)

@Serializable
data class Flyer(
    val id: String,
    val image: String,
    val title: String? = null,
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

@Serializable
data class About(
    val organization: String = "",
    val summary: String = "",
    val phones: List<String> = emptyList(),
    val whatsapp: String? = null,
    val email: String? = null,
    val website: String? = null,
    val address: String? = null,
    val mapUrl: String? = null,
    val socials: List<SocialLink> = emptyList(),
)

@Serializable
data class SocialLink(
    val name: String,
    val url: String,
)
