package org.darulhuda.nabiurrahmah.data

import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.darulhuda.nabiurrahmah.data.model.Catalog
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.data.source.CatalogStore
import org.darulhuda.nabiurrahmah.data.source.WebPage
import org.darulhuda.nabiurrahmah.data.source.WebsiteSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest {

    private val index = "https://site.test/nabi-ur-rahmah/".toHttpUrl()
    private val uploads = "https://site.test/wp-content/uploads"

    private class FakeStore(var saved: String? = null) : CatalogStore {
        override fun read() = saved
        override fun write(raw: String) { saved = raw }
    }

    /** Serves [pages] by URL; anything missing fails like a network error. */
    private class FakeWebsite(val pages: MutableMap<String, String>) : WebsiteSource {
        val requests = mutableListOf<String>()
        val unchanged = mutableSetOf<String>()
        override suspend fun fetch(url: HttpUrl): WebPage {
            requests += url.toString()
            val html = pages[url.toString()] ?: throw IOException("unreachable $url")
            return WebPage(url, html, unchanged = url.toString() in unchanged)
        }
    }

    private fun indexHtml(vararg languages: String) =
        languages.joinToString("") { """<a href="/nabi-ur-rahmah-${it.lowercase()}/">$it</a>""" }

    private fun flyersHtml(vararg names: String) =
        names.joinToString("") { """<img src="$uploads/$it.jpg" width="600" height="848">""" }

    private fun TestScope.repository(website: WebsiteSource, store: CatalogStore = FakeStore(), now: () -> Long = { 1_000_000L }) =
        CatalogRepository(
            indexUrl = index,
            website = website,
            store = store,
            clock = now,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            parseDispatcher = StandardTestDispatcher(testScheduler),
        )

    @Test
    fun `reads languages and their flyers from the website and saves them`() = runTest {
        val website = FakeWebsite(
            mutableMapOf(
                index.toString() to indexHtml("English", "Urdu"),
                "https://site.test/nabi-ur-rahmah-english/" to flyersHtml("en-1", "en-2"),
                "https://site.test/nabi-ur-rahmah-urdu/" to flyersHtml("ur-1"),
            ),
        )
        val store = FakeStore()
        val repo = repository(website, store)

        repo.load()

        val catalog = repo.state.value.catalog!!
        assertEquals(listOf("en", "ur"), catalog.languages.map { it.code })
        assertEquals(listOf(2, 1), catalog.languages.map { it.flyers.size })
        assertEquals("اردو", catalog.language("ur")!!.nativeName)
        assertTrue(catalog.language("ur")!!.rtl)
        assertNull(repo.state.value.error)
        assertFalse(repo.state.value.isRefreshing)
        assertTrue(repo.state.value.loadingLanguages.isEmpty())
        assertEquals(catalog, CatalogJson.decodeCatalog(store.saved!!))
    }

    @Test
    fun `offline start shows the saved catalogue`() = runTest {
        val saved = Catalog(fetchedAt = 1, languages = listOf(Language("en", "English", flyers = listOf(Flyer("a", "a.jpg")))))
        val repo = repository(FakeWebsite(mutableMapOf()), FakeStore(CatalogJson.encodeCatalog(saved)))

        repo.load()

        assertEquals(saved, repo.state.value.catalog)
        assertEquals(CatalogError.Network, repo.state.value.error)
    }

    @Test
    fun `first start offline is an error, not endless loading`() = runTest {
        val repo = repository(FakeWebsite(mutableMapOf()))
        assertTrue(repo.state.value.isLoading)

        repo.load()

        assertNull(repo.state.value.catalog)
        assertFalse(repo.state.value.isLoading)
        assertEquals(CatalogError.Network, repo.state.value.error)
    }

    @Test
    fun `one failing language page keeps its saved flyers and the rest update`() = runTest {
        val saved = Catalog(
            fetchedAt = 1,
            languages = listOf(
                Language("en", "English", pageUrl = "https://site.test/nabi-ur-rahmah-english/", flyers = listOf(Flyer("old", "old.jpg"))),
                Language("ur", "Urdu", pageUrl = "https://site.test/nabi-ur-rahmah-urdu/", flyers = listOf(Flyer("saved-ur", "s.jpg"))),
            ),
        )
        val website = FakeWebsite(
            mutableMapOf(
                index.toString() to indexHtml("English", "Urdu"),
                "https://site.test/nabi-ur-rahmah-english/" to flyersHtml("en-new"),
            ),
        )
        val repo = repository(website, FakeStore(CatalogJson.encodeCatalog(saved)))

        assertEquals(CatalogError.Partial, repo.refresh())

        val catalog = repo.state.value.catalog!!
        assertEquals(listOf("en-new.jpg"), catalog.language("en")!!.flyers.map { it.image.substringAfterLast('/') })
        assertEquals(listOf("saved-ur"), catalog.language("ur")!!.flyers.map { it.id })
        assertEquals("a partial read is not fresh, so the next start retries", 1L, catalog.fetchedAt)
    }

    @Test
    fun `a site with no recognisable flyers keeps the current catalogue`() = runTest {
        val saved = Catalog(fetchedAt = 1, languages = listOf(Language("en", "English", flyers = listOf(Flyer("a", "a.jpg")))))
        val website = FakeWebsite(mutableMapOf(index.toString() to "<p>Under maintenance</p>"))
        val store = FakeStore(CatalogJson.encodeCatalog(saved))
        val repo = repository(website, store)

        repo.load()

        assertEquals(saved, repo.state.value.catalog)
        assertEquals(CatalogError.InvalidData, repo.state.value.error)
        assertEquals(CatalogJson.encodeCatalog(saved), store.saved)
    }

    @Test
    fun `unchanged pages reuse saved flyers without parsing`() = runTest {
        val pageUrl = "https://site.test/nabi-ur-rahmah-english/"
        val saved = Catalog(fetchedAt = 1, languages = listOf(Language("en", "English", pageUrl = pageUrl, flyers = listOf(Flyer("keep", "k.jpg")))))
        val website = FakeWebsite(mutableMapOf(index.toString() to indexHtml("English"), pageUrl to "<p>not parsed</p>"))
        website.unchanged += pageUrl
        val repo = repository(website, FakeStore(CatalogJson.encodeCatalog(saved)))

        repo.refresh()

        assertEquals(listOf("keep"), repo.state.value.catalog!!.language("en")!!.flyers.map { it.id })
    }

    @Test
    fun `a fresh catalogue is not fetched again on start`() = runTest {
        val saved = Catalog(fetchedAt = 1_000_000L, languages = listOf(Language("en", "English")))
        val website = FakeWebsite(mutableMapOf())
        val repo = repository(website, FakeStore(CatalogJson.encodeCatalog(saved)), now = { 1_000_000L + 60_000 })

        repo.load()

        assertTrue(website.requests.isEmpty())
        assertNull(repo.state.value.error)

        repo.refresh(force = true)
        assertEquals(listOf(index.toString()), website.requests)
    }

    @Test
    fun `corrupt saved data is ignored`() = runTest {
        val website = FakeWebsite(
            mutableMapOf(
                index.toString() to indexHtml("English"),
                "https://site.test/nabi-ur-rahmah-english/" to flyersHtml("en-1"),
            ),
        )
        val repo = repository(website, FakeStore("{broken"))

        repo.load()

        assertNotNull(repo.state.value.catalog)
        assertEquals(1, repo.state.value.catalog!!.flyerCount)
    }

    @Test
    fun `images repeated on most language pages are removed as decoration`() {
        val banner = Flyer("banner", "banner.jpg")
        val languages = (1..4).map { Language("l$it", "L$it", flyers = listOf(banner, Flyer("f$it", "f$it.jpg"))) }

        val cleaned = CatalogRepository.removeSharedDecorations(languages)

        assertTrue(cleaned.all { language -> language.flyers.none { it.id == "banner" } && language.flyers.size == 1 })
    }
}
