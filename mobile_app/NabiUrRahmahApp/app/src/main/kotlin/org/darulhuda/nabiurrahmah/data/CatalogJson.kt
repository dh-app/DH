package org.darulhuda.nabiurrahmah.data

import kotlinx.serialization.json.Json
import org.darulhuda.nabiurrahmah.data.model.About
import org.darulhuda.nabiurrahmah.data.model.Catalog

/** JSON for the on-device catalogue cache and the bundled About details. */
object CatalogJson {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /** @throws kotlinx.serialization.SerializationException if [raw] is not a catalogue. */
    fun decodeCatalog(raw: String): Catalog = json.decodeFromString(Catalog.serializer(), raw)

    fun encodeCatalog(catalog: Catalog): String = json.encodeToString(Catalog.serializer(), catalog)

    fun decodeAbout(raw: String): About = json.decodeFromString(About.serializer(), raw)
}
