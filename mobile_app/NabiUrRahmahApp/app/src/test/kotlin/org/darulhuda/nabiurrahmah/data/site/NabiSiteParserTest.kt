package org.darulhuda.nabiurrahmah.data.site

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NabiSiteParserTest {

    private val parser = NabiSiteParser()
    private val indexUrl = "https://darulhudaudupi.org/nabi-ur-rahmah/".toHttpUrl()
    private val uploads = "https://darulhudaudupi.org/wp-content/uploads/2025/10"

    private fun page(content: String) = """
        <html><body>
          <header class="site-header"><img src="$uploads/logo.png"><nav>
            <a href="/arabic-college/">Arabic</a><a href="/">Home</a></nav></header>
          <main><article><div class="entry-content">$content</div></article></main>
          <footer><a href="/urdu-books/">Urdu</a><img src="$uploads/footer-banner.jpg" width="900" height="300"></footer>
        </body></html>
    """.trimIndent()

    @Test
    fun `finds language pages from links and ignores header and footer`() {
        val index = parser.parseIndex(
            page(
                """
                <h2>Flyers are available in the following languages</h2>
                <a class="elementor-button" href="https://darulhudaudupi.org/nabi-ur-rahmah-english/"><span>English</span></a>
                <a href="/nabi-ur-rahmah-urdu/">اردو</a>
                <a href="http://darulhudaudupi.org/nabi-ur-rahmah/kannada/#top"><img src="$uploads/kannada-button.jpg" alt="Kannada"></a>
                <a href="/nabi-ur-rahmah-hindi/">Read more</a>
                <a href="/nabi-ur-rahmah/">Nabi ur Rahmah</a>
                """,
            ),
            indexUrl,
        )

        assertEquals(listOf("en", "ur", "kn", "hi"), index.languages.map { it.language.code })
        assertEquals("https://darulhudaudupi.org/nabi-ur-rahmah-english/", index.languages[0].pageUrl)
        assertEquals("https://darulhudaudupi.org/nabi-ur-rahmah-urdu/", index.languages[1].pageUrl)
        assertEquals("https://darulhudaudupi.org/nabi-ur-rahmah/kannada/", index.languages[2].pageUrl)
        assertEquals("https://darulhudaudupi.org/nabi-ur-rahmah-hindi/", index.languages[3].pageUrl)
    }

    @Test
    fun `reads a WordPress gallery with renditions, captions and lazy loading`() {
        val flyers = parser.parseFlyers(
            page(
                """
                <figure class="wp-block-gallery">
                  <figure class="wp-block-image">
                    <a href="$uploads/seerah-01.jpg">
                      <img src="$uploads/seerah-01-300x424.jpg" width="300" height="424" alt="The Mercy to the Worlds"
                        srcset="$uploads/seerah-01-300x424.jpg 300w, $uploads/seerah-01-724x1024.jpg 724w, $uploads/seerah-01-1086x1536.jpg 1086w">
                    </a>
                    <figcaption>His Character ﷺ</figcaption>
                  </figure>
                  <figure class="wp-block-image">
                    <img src="data:image/svg+xml,%3Csvg%3E" data-src="$uploads/seerah-02-600x848.jpg"
                      data-srcset="$uploads/seerah-02-600x848.jpg 600w, $uploads/seerah-02.jpg 1240w" width="600" height="848">
                  </figure>
                </figure>
                <p><img src="$uploads/seerah-01-724x1024.jpg" width="724" height="1024"></p>
                <img src="$uploads/whatsapp-icon.png" width="32" height="32">
                """,
            ),
            "https://darulhudaudupi.org/nabi-ur-rahmah-english/".toHttpUrl(),
        )

        assertEquals("duplicate renditions of one flyer collapse into one", 2, flyers.size)
        val (first, second) = flyers
        assertEquals("$uploads/seerah-01.jpg", first.image)
        assertEquals("$uploads/seerah-01-724x1024.jpg", first.thumbnail)
        assertEquals("His Character ﷺ", first.title)
        assertEquals(300f / 424f, first.aspectRatio!!, 0.001f)

        assertEquals("$uploads/seerah-02.jpg", second.image)
        assertEquals("$uploads/seerah-02-600x848.jpg", second.thumbnail)
        assertNull(second.title)
    }

    @Test
    fun `pdf flyers keep their preview image or fall back to the pdf itself`() {
        val flyers = parser.parseFlyers(
            page(
                """
                <a href="$uploads/prophet-of-mercy-urdu.pdf"><img src="$uploads/prophet-of-mercy-urdu-cover.jpg" width="800" height="1131"></a>
                <a href="$uploads/seerah-part-2.PDF">Seerah part 2</a>
                """,
            ),
            "https://darulhudaudupi.org/nabi-ur-rahmah-urdu/".toHttpUrl(),
        )

        assertEquals(2, flyers.size)
        assertEquals("$uploads/prophet-of-mercy-urdu.pdf", flyers[0].pdf)
        assertEquals("$uploads/prophet-of-mercy-urdu-cover.jpg", flyers[0].image)
        assertEquals("$uploads/seerah-part-2.PDF", flyers[1].pdf)
        assertEquals(flyers[1].pdf, flyers[1].image)
        assertEquals("Seerah part 2", flyers[1].title)
    }

    @Test
    fun `google drive links become direct images`() {
        val flyers = parser.parseFlyers(
            page(
                """
                <a href="https://drive.google.com/file/d/1AbC_xyz/view?usp=sharing">Flyer 1</a>
                <a href="https://drive.google.com/open?id=2DeF">Flyer 2</a>
                """,
            ),
            indexUrl,
        )

        assertEquals(
            listOf("https://drive.google.com/thumbnail?id=1AbC_xyz&sz=w2000", "https://drive.google.com/thumbnail?id=2DeF&sz=w2000"),
            flyers.map { it.image },
        )
    }

    @Test
    fun `groups flyers under language headings and tabs on the index page`() {
        val index = parser.parseIndex(
            page(
                """
                <img src="$uploads/nabi-ur-rahmah-banner.jpg" width="1200" height="400">
                <h3>English</h3>
                <img src="$uploads/en-1.jpg" width="600" height="848">
                <img src="$uploads/en-2.jpg" width="600" height="848">
                <h3 class="wp-block-heading">ಕನ್ನಡ</h3>
                <a href="$uploads/kn-1.jpg"><img src="$uploads/kn-1-300x424.jpg" width="300" height="424"></a>
                <div class="e-n-tabs">
                  <div role="tablist"><button id="tab-1" role="tab">Tamil</button><button id="tab-2" role="tab">Telugu</button></div>
                  <div role="tabpanel" aria-labelledby="tab-1"><img src="$uploads/ta-1.jpg" width="600" height="848"></div>
                  <div role="tabpanel" aria-labelledby="tab-2"><img src="$uploads/te-1.jpg" width="600" height="848"></div>
                </div>
                """,
            ),
            indexUrl,
        )

        val byCode = index.languages.associate { it.language.code to it.flyers.map { f -> f.image.substringAfterLast('/') } }
        assertEquals(listOf("en-1.jpg", "en-2.jpg"), byCode["en"])
        assertEquals(listOf("kn-1.jpg"), byCode["kn"])
        assertEquals(listOf("ta-1.jpg"), byCode["ta"])
        assertEquals(listOf("te-1.jpg"), byCode["te"])
        assertTrue("the banner is decoration, not a flyer", index.decorationKeys.single().startsWith("nabi-ur-rahmah-banner"))
    }

    @Test
    fun `direct file links labelled with a language`() {
        val index = parser.parseIndex(
            page(
                """
                <a href="$uploads/nur-english.pdf">English</a>
                <a href="$uploads/nur-hindi.jpg">हिन्दी</a>
                """,
            ),
            indexUrl,
        )

        assertEquals(listOf("en", "hi"), index.languages.map { it.language.code })
        assertEquals("$uploads/nur-english.pdf", index.languages[0].flyers.single().pdf)
        assertEquals("$uploads/nur-hindi.jpg", index.languages[1].flyers.single().image)
    }

    @Test
    fun `a page without language structure still shows its flyers`() {
        val index = parser.parseIndex(page("""<img src="$uploads/a.jpg" width="600" height="848">"""), indexUrl)

        assertEquals(NabiSiteParser.ALL_LANGUAGES, index.languages.single().language)
        assertEquals(1, index.languages.single().flyers.size)
    }

    @Test
    fun `decoration seen on the index page is skipped on language pages`() {
        val index = parser.parseIndex(
            page("""<img src="$uploads/banner.jpg" width="1200" height="400"><a href="/nabi-ur-rahmah-urdu/">Urdu</a>"""),
            indexUrl,
        )
        val flyers = parser.parseFlyers(
            page("""<img src="$uploads/banner-1024x341.jpg" width="1024" height="341"><img src="$uploads/ur-1.jpg" width="600" height="848">"""),
            "https://darulhudaudupi.org/nabi-ur-rahmah-urdu/".toHttpUrl(),
            index.decorationKeys,
        )

        assertEquals(listOf("$uploads/ur-1.jpg"), flyers.map { it.image })
    }

    @Test
    fun `flyer ids are stable across renditions`() {
        val a = NabiSiteParser.canonicalKey("$uploads/Seerah-01-300x424.jpg?ver=2")
        val b = NabiSiteParser.canonicalKey("$uploads/seerah-01-scaled.jpg")
        assertEquals(a, b)
    }
}
