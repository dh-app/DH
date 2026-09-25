package org.darulhuda.nabiurrahmah.data

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import org.darulhuda.nabiurrahmah.data.model.Catalog
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language

/**
 * Turns catalogue JSON into a [Catalog] the UI can trust: file paths are resolved
 * against [baseUrl], and entries that could never display (blank ids, missing
 * images, duplicates) are dropped instead of failing the whole document.
 */
class CatalogParser(private val baseUrl: HttpUrl) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /** @throws kotlinx.serialization.SerializationException if [raw] is not a catalogue. */
    fun parse(raw: String): Catalog {
        val catalog = json.decodeFromString(Catalog.serializer(), raw)
        return catalog.copy(
            languages = catalog.languages
                .filter { it.code.isNotBlank() && it.name.isNotBlank() }
                .distinctBy { it.code }
                .map { it.sanitized() },
        )
    }

    private fun Language.sanitized(): Language = copy(
        nativeName = nativeName.ifBlank { name },
        flyers = flyers
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .mapNotNull { it.resolved() },
    )

    private fun Flyer.resolved(): Flyer? {
        val image = resolve(image) ?: return null
        return copy(
            image = image,
            thumbnail = thumbnail?.let(::resolve),
            pdf = pdf?.let(::resolve),
            title = title?.takeIf { it.isNotBlank() },
        )
    }

    private fun resolve(path: String): String? =
        path.trim().takeIf { it.isNotEmpty() }?.let { baseUrl.resolve(it)?.toString() }
}
