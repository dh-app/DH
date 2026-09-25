package org.darulhuda.nabiurrahmah.data

import java.io.File
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards /content/catalog.json, the file the app downloads and bundles.
 * A mistake there reaches every user, so CI checks it on every change.
 */
class PublishedCatalogTest {

    private val file = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "content/catalog.json") }
        .first { it.isFile }

    private val raw = file.readText()
    private val parser = CatalogParser("https://example.org/content/catalog.json".toHttpUrl())

    @Test
    fun `parses without losing any entries`() {
        val catalog = parser.parse(raw)
        val rawLanguages = Regex("\"code\"\\s*:").findAll(raw).count()
        val rawFlyers = Regex("\"image\"\\s*:").findAll(raw).count()

        assertEquals("every language must be valid and unique", rawLanguages, catalog.languages.size)
        assertEquals("every flyer must be valid and unique", rawFlyers, catalog.flyerCount)
    }

    @Test
    fun `has the essentials`() {
        val catalog = parser.parse(raw)
        assertTrue("updatedAt must be an ISO date", Regex("""\d{4}-\d{2}-\d{2}.*""").matches(catalog.updatedAt))
        assertTrue(catalog.languages.isNotEmpty())
        assertTrue(catalog.about.organization.isNotBlank())
        catalog.languages.forEach { assertTrue("bad code ${it.code}", Regex("[a-z]{2,3}(-[A-Za-z0-9]+)?").matches(it.code)) }
    }

    @Test
    fun `relative files exist in the repository`() {
        val contentDir = file.parentFile!!
        val base = "https://example.org/content/"
        parser.parse(raw).languages.flatMap { it.flyers }.forEach { flyer ->
            listOfNotNull(flyer.image, flyer.thumbnail, flyer.pdf)
                .filter { it.startsWith(base) }
                .forEach { url ->
                    val local = File(contentDir, url.removePrefix(base))
                    assertTrue("missing file for flyer ${flyer.id}: $local", local.isFile)
                }
        }
    }
}
