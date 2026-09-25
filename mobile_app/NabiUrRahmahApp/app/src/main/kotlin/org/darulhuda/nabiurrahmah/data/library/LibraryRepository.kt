package org.darulhuda.nabiurrahmah.data.library

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.darulhuda.nabiurrahmah.data.source.CatalogStore

/** The book shelves on the home screen. */
enum class ShelfId(val key: String) {
    Biography("biography"),
    Testimonies("testimonies"),
    Books("books"),
}

data class ShelfState(
    val shelf: Shelf? = null,
    val isRefreshing: Boolean = false,
    /** True when the last refresh could not reach any source. */
    val offline: Boolean = false,
    /** The shelf has no sources configured yet. */
    val comingSoon: Boolean = false,
) {
    val isLoading: Boolean get() = shelf == null && !offline && !comingSoon
}

/**
 * Book shelves, each fed by one or more [BookSource]s. Offline-first like the
 * flyers: the saved shelf shows at once, and refreshes are throttled because
 * book lists change slowly.
 */
class LibraryRepository(
    private val sources: Map<ShelfId, List<BookSource>>,
    private val storeFor: (ShelfId) -> CatalogStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }
    private val states = ConcurrentHashMap<ShelfId, MutableStateFlow<ShelfState>>()
    private val locks = ConcurrentHashMap<ShelfId, Mutex>()

    fun state(id: ShelfId): StateFlow<ShelfState> = flow(id).asStateFlow()

    private fun flow(id: ShelfId) = states.getOrPut(id) {
        MutableStateFlow(ShelfState(comingSoon = sources[id].isNullOrEmpty()))
    }

    /** Shows the saved shelf, then refreshes it if it is stale. */
    suspend fun load(id: ShelfId) = refresh(id, force = false)

    suspend fun refresh(id: ShelfId, force: Boolean = true) {
        val shelfSources = sources[id].orEmpty()
        if (shelfSources.isEmpty()) return
        val state = flow(id)
        locks.getOrPut(id) { Mutex() }.withLock {
            val saved = state.value.shelf ?: withContext(ioDispatcher) { readSaved(id) }?.also { shelf ->
                state.update { it.copy(shelf = shelf) }
            }
            if (!force && saved != null && clock() - saved.fetchedAt < MIN_REFRESH_INTERVAL_MS) return
            state.update { it.copy(isRefreshing = true) }
            try {
                val results = coroutineScope {
                    shelfSources.map { source ->
                        async {
                            try {
                                source.books()
                            } catch (e: IOException) {
                                null
                            }
                        }
                    }.awaitAll()
                }
                if (results.all { it == null }) {
                    state.update { it.copy(isRefreshing = false, offline = true) }
                    return
                }
                val books = results.filterNotNull().flatten().distinctBy { it.id }
                val shelf = Shelf(books, fetchedAt = clock())
                state.update { ShelfState(shelf = shelf) }
                withContext(ioDispatcher) { storeFor(id).write(json.encodeToString(Shelf.serializer(), shelf)) }
            } catch (e: CancellationException) {
                state.update { it.copy(isRefreshing = false) }
                throw e
            }
        }
    }

    fun book(id: ShelfId, bookId: String): Book? = flow(id).value.shelf?.books?.firstOrNull { it.id == bookId }

    private fun readSaved(id: ShelfId): Shelf? =
        try {
            storeFor(id).read()?.let { json.decodeFromString(Shelf.serializer(), it) }
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IOException) {
            null
        }

    companion object {
        const val MIN_REFRESH_INTERVAL_MS = 12 * 60 * 60 * 1000L
    }
}
