package org.darulhuda.nabiurrahmah.data.library

import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.darulhuda.nabiurrahmah.data.source.CatalogStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRepositoryTest {

    private class MemoryStore : CatalogStore {
        var saved: String? = null
        override fun read() = saved
        override fun write(raw: String) { saved = raw }
    }

    private fun book(id: String) = Book(id, listOf(Edition(id, "en", "Title $id", files = listOf(BookFile("https://x/$id.pdf")))))

    @Test
    fun `merges sources, saves the shelf and survives going offline`() = runTest {
        val store = MemoryStore()
        var online = true
        val repo = LibraryRepository(
            sources = mapOf(
                ShelfId.Biography to listOf(
                    BookSource { if (online) listOf(book("a"), book("b")) else throw IOException() },
                    BookSource { if (online) listOf(book("b"), book("c")) else throw IOException() },
                ),
            ),
            storeFor = { store },
            clock = { 0L },
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        repo.load(ShelfId.Biography)
        assertEquals(listOf("a", "b", "c"), repo.state(ShelfId.Biography).value.shelf!!.books.map { it.id })
        assertTrue(store.saved != null)

        online = false
        repo.refresh(ShelfId.Biography)
        val state = repo.state(ShelfId.Biography).value
        assertTrue(state.offline)
        assertEquals(3, state.shelf!!.books.size)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `a shelf without sources is coming soon`() = runTest {
        val repo = LibraryRepository(emptyMap(), { MemoryStore() }, ioDispatcher = StandardTestDispatcher(testScheduler))

        repo.load(ShelfId.Testimonies)

        assertTrue(repo.state(ShelfId.Testimonies).value.comingSoon)
        assertFalse(repo.state(ShelfId.Testimonies).value.isLoading)
    }
}
