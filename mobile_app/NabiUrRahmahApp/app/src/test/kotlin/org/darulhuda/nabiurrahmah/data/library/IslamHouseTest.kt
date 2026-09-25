package org.darulhuda.nabiurrahmah.data.library

import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslamHouseTest {

    private fun apiItem(id: Int, sourceId: Int, lang: String, title: String, ext: String = "PDF") = """
        {"id":$id,"source_id":$sourceId,"title":"$title","type":"books","translated_language":"$lang",
         "description":"<p>The biography of the Prophet ﷺ</p>",
         "prepared_by":[{"title":"Safiur Rahman Mubarakpuri","kind":"author"},{"title":"Issam Diab","kind":"translator"}],
         "attachments":[{"order":1,"size":"2.6 MB","extension_type":"$ext","description":"Book",
           "url":"http://d1.islamhouse.com/data/$lang/ih_books/single/${lang}_sealed_nectar.${ext.lowercase()}"}]}
    """

    @Test
    fun `parses api items, keeps readable files and groups translations`() {
        val page = IslamHouseParser.parseApi(
            """{"links":{"pages_number":3},"data":[
                ${apiItem(10, 1, "en", "The Sealed Nectar")},
                ${apiItem(11, 1, "ur", "الرحیق المختوم")},
                ${apiItem(12, 2, "ar", "Audio only", ext = "MP3")},
                {"id":13,"title":"No files","attachments":[]}
            ]}""",
        )

        assertEquals(3, page.totalPages)
        assertEquals(listOf("10", "11"), page.items.map { it.id })
        val english = page.items.first()
        assertEquals("Safiur Rahman Mubarakpuri", english.author)
        assertEquals("The biography of the Prophet ﷺ", english.description)
        assertEquals("https://d1.islamhouse.com/data/en/ih_books/single/en_sealed_nectar.pdf", english.files.single().url)
        assertEquals("2.6 MB", english.files.single().size)

        val book = IslamHouseParser.group(page.items).single()
        assertEquals(listOf("en", "ur"), book.languages)
        assertEquals("ur", book.preferredEdition(listOf("ur", "en")).language)
        assertEquals("en", book.preferredEdition(listOf("fr")).language)
    }

    @Test
    fun `reads website category and item pages`() {
        val category = "https://islamhouse.com/en/category/795/showall/showall/1/".toHttpUrl()
        val links = IslamHouseParser.parseCategoryHtml(
            """<a href="/en/books/2800006/">The Sealed Nectar</a><a href="https://islamhouse.com/en/books/2800006/#x">dup</a>
               <a href="/en/videos/1/">video</a><a href="/en/category/795/showall/showall/2/">next</a>""",
            category,
        )
        assertEquals(listOf("https://islamhouse.com/en/books/2800006/"), links)

        val item = IslamHouseParser.parseItemHtml(
            """<html><head><meta property="og:title" content="The Sealed Nectar"></head><body>
               <a href="https://d1.islamhouse.com/data/en/ih_books/single/en_sealed.pdf">Download</a></body></html>""",
            "https://islamhouse.com/en/books/2800006/".toHttpUrl(),
        )!!
        assertEquals("2800006", item.id)
        assertEquals("The Sealed Nectar", item.title)
        assertEquals("en", item.language)
        assertNull(IslamHouseParser.parseItemHtml("<p>no files</p>", "https://islamhouse.com/en/books/1/".toHttpUrl()))
    }

    private class FakeFetcher(val responses: Map<String, String>, val offline: Boolean = false) : TextFetcher {
        val requested = mutableListOf<String>()
        override suspend fun get(url: HttpUrl): String? {
            requested += url.toString()
            if (offline) throw IOException("offline")
            return responses[url.toString()]
        }
    }

    @Test
    fun `falls back to the next api url form and follows pages`() = runTest {
        val base = "https://api3.islamhouse.com/v3/${IslamHouseCategorySource.PUBLIC_API_KEY}/main/get-category-items/795"
        val fetcher = FakeFetcher(
            mapOf(
                "$base/showall/en/showall/1/50/json" to """{"links":{"pages_number":2},"data":[${apiItem(1, 1, "en", "A")}]}""",
                "$base/showall/en/showall/2/50/json" to """{"data":[${apiItem(2, 2, "ta", "B")}]}""",
            ),
        )

        val books = IslamHouseCategorySource(795, fetcher).books()

        assertEquals(2, books.size)
        assertTrue(fetcher.requested.first().contains("/showall/showall/en/1/"))
    }

    @Test(expected = IOException::class)
    fun `offline is reported, not an empty shelf`() = runTest {
        IslamHouseCategorySource(795, FakeFetcher(emptyMap(), offline = true)).books()
    }

    @Test
    fun `web page pdfs become books`() = runTest {
        val page = "https://darulhudaudupi.org/what-they-say/".toHttpUrl()
        val fetcher = FakeFetcher(
            mapOf(
                page.toString() to """<main><a href="/wp-content/uploads/what-they-say-about-muhammad-urdu.pdf">What they say (Urdu)</a>
                   <a href="/wp-content/uploads/muhammad_in_western_eyes.pdf"></a></main>""",
            ),
        )

        val books = WebPagePdfSource(page, fetcher).books()

        assertEquals(2, books.size)
        assertEquals("ur", books[0].editions.single().language)
        assertEquals("Muhammad in western eyes", books[1].editions.single().title)
    }

    @Test
    fun `source spec parsing`() {
        val fetcher = FakeFetcher(emptyMap())
        val sources = bookSourcesFrom("islamhouse:795, page:https://example.org/books/, bogus:1, islamhouse:x", fetcher)
        assertEquals(2, sources.size)
        assertTrue(sources[0] is IslamHouseCategorySource)
        assertTrue(sources[1] is WebPagePdfSource)
    }

    @Test
    fun `language names come from the platform`() {
        assertEquals("Urdu", LanguageNames.english("ur"))
        assertEquals("اردو", LanguageNames.native("ur"))
        assertTrue(LanguageNames.isRtl("ur"))
    }
}
