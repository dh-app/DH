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
import org.darulhuda.nabiurrahmah.data.model.Playlist
import org.darulhuda.nabiurrahmah.data.model.Video
import org.darulhuda.nabiurrahmah.data.youtube.PlaylistSource
import org.darulhuda.nabiurrahmah.data.source.PublishedCatalogSource
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

    private fun TestScope.repository(
        website: WebsiteSource,
        store: CatalogStore = FakeStore(),
        now: () -> Long = { 1_000_000L },
        playlists: PlaylistSource = PlaylistSource { throw IOException("offline") },
        configuredPlaylists: List<String> = emptyList(),
        published: PublishedCatalogSource? = null,
    ) =
        CatalogRepository(
            indexUrl = index,
            website = website,
            store = store,
            playlists = playlists,
            configuredPlaylistIds = configuredPlaylists,
            published = published,
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

    private fun playlist(id: String, vararg videos: String) = Playlist(id, "Playlist $id", videos.map { Video(it, "Video $it") })

    @Test
    fun `videos load even when the website cannot be read`() = runTest {
        val repo = repository(
            FakeWebsite(mutableMapOf()),
            playlists = { id -> playlist(id, "v1", "v2") },
            configuredPlaylists = listOf("PL1"),
        )

        repo.load()

        val catalog = repo.state.value.catalog!!
        assertEquals(listOf("PL1"), catalog.playlists.map { it.id })
        assertTrue(catalog.languages.isEmpty())
        assertEquals(CatalogError.Network, repo.state.value.error)
    }

    @Test
    fun `playlists linked on the website are added after the configured ones`() = runTest {
        val website = FakeWebsite(
            mutableMapOf(
                index.toString() to indexHtml("English") +
                    """<iframe src="https://www.youtube.com/embed/videoseries?list=PLfromSite123"></iframe>""",
                "https://site.test/nabi-ur-rahmah-english/" to flyersHtml("en-1"),
            ),
        )
        val repo = repository(website, playlists = { id -> playlist(id, "v-$id") }, configuredPlaylists = listOf("PL1"))

        repo.load()

        assertEquals(listOf("PL1", "PLfromSite123"), repo.state.value.catalog!!.playlists.map { it.id })
    }

    @Test
    fun `a playlist that fails to load keeps its saved videos`() = runTest {
        val saved = Catalog(playlists = listOf(playlist("PL1", "old")))
        val repo = repository(
            FakeWebsite(mutableMapOf()),
            store = FakeStore(CatalogJson.encodeCatalog(saved)),
            playlists = { throw IOException("offline") },
            configuredPlaylists = listOf("PL1"),
        )

        repo.refresh()

        assertEquals(listOf("old"), repo.state.value.catalog!!.playlists.single().videos.map { it.id })
    }

    @Test
    fun `a complete published catalogue is used as is, without touching the website`() = runTest {
        val published = Catalog(
            languages = listOf(Language("hi", "Hindi", flyers = listOf(Flyer("h1", "https://raw.example/h1.jpg")))),
            playlists = listOf(playlist("PL1", "v1")),
        )
        val website = FakeWebsite(mutableMapOf())
        val repo = repository(website, published = { published }, configuredPlaylists = listOf("PL1"))

        repo.load()

        assertEquals(listOf("hi"), repo.state.value.catalog!!.languages.map { it.code })
        assertEquals(listOf("PL1"), repo.state.value.catalog!!.playlists.map { it.id })
        assertNull(repo.state.value.error)
        assertTrue("one download, nothing else", website.requests.isEmpty())
    }

    @Test
    fun `published flyers with no videos still read youtube`() = runTest {
        val published = Catalog(languages = listOf(Language("hi", "Hindi", flyers = listOf(Flyer("h1", "h1.jpg")))))
        val website = FakeWebsite(mutableMapOf())
        val repo = repository(
            website,
            published = { published },
            playlists = { id -> playlist(id, "v1") },
            configuredPlaylists = listOf("PL1"),
        )

        repo.load()

        assertEquals(1, repo.state.value.catalog!!.flyerCount)
        assertEquals(listOf("PL1"), repo.state.value.catalog!!.playlists.map { it.id })
        assertTrue(website.requests.isEmpty())
        assertNull(repo.state.value.error)
    }

    @Test
    fun `an unreachable published catalogue falls back to the website`() = runTest {
        val website = FakeWebsite(
            mutableMapOf(
                index.toString() to indexHtml("English"),
                "https://site.test/nabi-ur-rahmah-english/" to flyersHtml("en-1"),
            ),
        )
        val repo = repository(website, published = { throw IOException("github down") })

        repo.load()

        assertEquals(1, repo.state.value.catalog!!.flyerCount)
    }
}
