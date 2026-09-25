package org.darulhuda.nabiurrahmah.ui.common

import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.CatalogError
import org.darulhuda.nabiurrahmah.data.CatalogRepository

/**
 * Pull-to-refresh started by the person. Tracked separately from background
 * refreshes so the spinner only shows when they asked for it.
 */
class UserRefresh(
    private val repository: CatalogRepository,
    private val scope: CoroutineScope,
) {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /** One-off messages (string resources) to show in a snackbar. */
    val messages: Flow<Int> = _messages.receiveAsFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        scope.launch {
            try {
                val error = repository.refresh()
                // With nothing on screen the error state explains itself; no snackbar needed.
                if (error != null && repository.state.value.catalog != null) _messages.send(error.messageRes)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

@get:StringRes
val CatalogError.messageRes: Int
    get() = when (this) {
        CatalogError.Network -> R.string.message_offline
        CatalogError.InvalidData -> R.string.message_update_failed
    }
