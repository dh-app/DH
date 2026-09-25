package org.darulhuda.nabiurrahmah.data

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import org.darulhuda.nabiurrahmah.data.model.Catalog
import org.darulhuda.nabiurrahmah.data.source.CatalogRemoteSource
import org.darulhuda.nabiurrahmah.data.source.CatalogStore

data class CatalogState(
    val catalog: Catalog? = null,
    val isRefreshing: Boolean = false,
    val error: CatalogError? = null,
) {
    /** Nothing to show yet, and no failure to report. */
    val isLoading: Boolean get() = catalog == null && error == null
}

enum class CatalogError { Network, InvalidData }

/**
 * Single source of truth for the catalogue. Offline-first: the newest local copy
 * (last download, or the one bundled in the APK) is shown immediately, then
 * replaced by the network copy when it arrives.
 */
class CatalogRepository(
    private val remote: CatalogRemoteSource,
    private val store: CatalogStore,
    private val parser: CatalogParser,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private val refreshMutex = Mutex()

    /** Shows the best local copy, then refreshes from the network. */
    suspend fun load() {
        if (_state.value.catalog == null) {
            val local = withContext(ioDispatcher) { readLocal() }
            if (local != null) _state.update { if (it.catalog == null) it.copy(catalog = local) else it }
        }
        refresh()
    }

    /**
     * Downloads the latest catalogue. On failure the current catalogue is kept and
     * [CatalogState.error] explains why. Concurrent calls are coalesced.
     *
     * @return the error, or null on success.
     */
    suspend fun refresh(): CatalogError? {
        if (refreshMutex.isLocked) {
            // A refresh is already running; wait for it rather than starting another.
            refreshMutex.withLock { }
            return _state.value.error
        }
        return refreshMutex.withLock {
            _state.update { it.copy(isRefreshing = true) }
            val error = try {
                val catalog = withContext(ioDispatcher) {
                    val raw = remote.fetch()
                    parser.parse(raw).also { store.writeCached(raw) }
                }
                _state.update { it.copy(catalog = catalog, error = null) }
                null
            } catch (e: CancellationException) {
                _state.update { it.copy(isRefreshing = false) }
                throw e
            } catch (e: SerializationException) {
                CatalogError.InvalidData
            } catch (e: IllegalArgumentException) {
                CatalogError.InvalidData
            } catch (e: IOException) {
                CatalogError.Network
            }
            _state.update { it.copy(isRefreshing = false, error = error) }
            error
        }
    }

    /** Picks the most recently published of the cached and bundled copies. */
    private fun readLocal(): Catalog? {
        val cached = parseOrNull { store.readCached() }
        val bundled = parseOrNull { store.readBundled() }
        return when {
            cached == null -> bundled
            bundled == null -> cached
            bundled.updatedAt > cached.updatedAt -> bundled
            else -> cached
        }
    }

    private inline fun parseOrNull(read: () -> String?): Catalog? =
        try {
            read()?.let(parser::parse)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IOException) {
            null
        }
}
