package org.darulhuda.nabiurrahmah.data

import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.darulhuda.nabiurrahmah.data.source.CatalogRemoteSource
import org.darulhuda.nabiurrahmah.data.source.CatalogStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRepositoryTest {

    private val parser = CatalogParser("https://example.org/catalog.json".toHttpUrl())

    private fun catalogJson(updatedAt: String, vararg codes: String) =
        """{"updatedAt":"$updatedAt","languages":[${codes.joinToString { """{"code":"$it","name":"$it"}""" }}]}"""

    private class FakeStore(var cached: String? = null, val bundled: String? = null) : CatalogStore {
        override fun readCached() = cached
        override fun writeCached(raw: String) { cached = raw }
        override fun readBundled() = bundled
    }

    @Test
    fun `network copy replaces local copy and is cached`() = runTest {
        val store = FakeStore(bundled = catalogJson("2026-01-01", "en"))
        val remote = catalogJson("2026-02-01", "en", "ur")
        val repo = CatalogRepository({ remote }, store, parser, StandardTestDispatcher(testScheduler))

        repo.load()

        val state = repo.state.value
        assertEquals(listOf("en", "ur"), state.catalog!!.languages.map { it.code })
        assertNull(state.error)
        assertFalse(state.isRefreshing)
        assertEquals(remote, store.cached)
    }

    @Test
    fun `offline keeps the newest local copy and reports a network error`() = runTest {
        val store = FakeStore(
            cached = catalogJson("2026-03-01", "en", "ur", "hi"),
            bundled = catalogJson("2026-01-01", "en"),
        )
        val repo = CatalogRepository(
            { throw IOException("offline") },
            store,
            parser,
            StandardTestDispatcher(testScheduler),
        )

        repo.load()

        val state = repo.state.value
        assertEquals(3, state.catalog!!.languages.size)
        assertEquals(CatalogError.Network, state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `bundled copy wins when it is newer than the cache`() = runTest {
        val store = FakeStore(
            cached = catalogJson("2026-01-01", "en"),
            bundled = catalogJson("2026-05-01", "en", "ar"),
        )
        val repo = CatalogRepository(
            { throw IOException("offline") },
            store,
            parser,
            StandardTestDispatcher(testScheduler),
        )

        repo.load()

        assertEquals(listOf("en", "ar"), repo.state.value.catalog!!.languages.map { it.code })
    }

    @Test
    fun `invalid network data keeps the current catalogue`() = runTest {
        val store = FakeStore(bundled = catalogJson("2026-01-01", "en"))
        val repo = CatalogRepository({ "<html>" }, store, parser, StandardTestDispatcher(testScheduler))

        repo.load()

        val state = repo.state.value
        assertEquals(listOf("en"), state.catalog!!.languages.map { it.code })
        assertEquals(CatalogError.InvalidData, state.error)
        assertNull("broken data must never be cached", store.cached)
    }

    @Test
    fun `corrupt cache falls back to the bundled copy`() = runTest {
        val store = FakeStore(cached = "{not json", bundled = catalogJson("2026-01-01", "en"))
        val repo = CatalogRepository(
            { throw IOException("offline") },
            store,
            parser,
            StandardTestDispatcher(testScheduler),
        )

        repo.load()

        assertEquals(listOf("en"), repo.state.value.catalog!!.languages.map { it.code })
    }

    @Test
    fun `nothing local and offline is an error, not endless loading`() = runTest {
        val repo = CatalogRepository(
            { throw IOException("offline") },
            FakeStore(),
            parser,
            StandardTestDispatcher(testScheduler),
        )
        assertTrue(repo.state.value.isLoading)

        repo.load()

        assertNull(repo.state.value.catalog)
        assertFalse(repo.state.value.isLoading)
        assertEquals(CatalogError.Network, repo.state.value.error)
    }

    @Test
    fun `successful refresh clears a previous error`() = runTest {
        var online = false
        val remote = CatalogRemoteSource {
            if (!online) throw IOException("offline")
            catalogJson("2026-02-01", "en")
        }
        val repo = CatalogRepository(remote, FakeStore(), parser, StandardTestDispatcher(testScheduler))

        repo.load()
        assertEquals(CatalogError.Network, repo.state.value.error)

        online = true
        assertNull(repo.refresh())
        assertNull(repo.state.value.error)
    }
}
