package org.darulhuda.nabiurrahmah.data

import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogParserTest {

    private val parser = CatalogParser("https://example.org/content/catalog.json".toHttpUrl())

    @Test
    fun `resolves relative paths against the catalogue url`() {
        val catalog = parser.parse(
            """
            {"languages":[{"code":"ur","name":"Urdu","flyers":[
              {"id":"1","image":"flyers/ur/one.jpg","thumbnail":"flyers/ur/one-thumb.jpg","pdf":"/files/one.pdf"},
              {"id":"2","image":"https://cdn.example.com/two.jpg"}
            ]}]}
            """.trimIndent(),
        )

        val (one, two) = catalog.languages.single().flyers
        assertEquals("https://example.org/content/flyers/ur/one.jpg", one.image)
        assertEquals("https://example.org/content/flyers/ur/one-thumb.jpg", one.previewUrl)
        assertEquals("https://example.org/files/one.pdf", one.pdf)
        assertEquals("https://cdn.example.com/two.jpg", two.image)
        assertEquals(two.image, two.previewUrl)
    }

    @Test
    fun `drops entries that cannot be shown`() {
        val catalog = parser.parse(
            """
            {"languages":[
              {"code":"en","name":"English","flyers":[
                {"id":"a","image":"a.jpg"},
                {"id":"a","image":"duplicate.jpg"},
                {"id":"","image":"no-id.jpg"},
                {"id":"b","image":"  "}
              ]},
              {"code":"en","name":"English again"},
              {"code":"","name":"No code"}
            ]}
            """.trimIndent(),
        )

        val english = catalog.languages.single()
        assertEquals("English", english.name)
        assertEquals(listOf("a"), english.flyers.map { it.id })
        assertEquals("https://example.org/content/a.jpg", english.flyers.single().image)
    }

    @Test
    fun `fills defaults and ignores unknown fields`() {
        val catalog = parser.parse(
            """{"futureField":true,"languages":[{"code":"hi","name":"Hindi","nativeName":"","rtl":null,"extra":1,
               "flyers":[{"id":"x","image":"x.jpg","title":" ","width":600,"height":800}]}]}""",
        )

        val hindi = catalog.language("hi")!!
        assertEquals("Hindi", hindi.nativeName)
        assertEquals(false, hindi.rtl)
        assertNull(hindi.flyers.single().title)
        assertEquals(0.75f, hindi.flyers.single().aspectRatio!!, 0.0001f)
        assertEquals(1, catalog.flyerCount)
    }

    @Test(expected = SerializationException::class)
    fun `rejects documents that are not a catalogue`() {
        parser.parse("""{"languages": "nope"}""")
    }
}
