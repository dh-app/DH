package org.darulhuda.nabiurrahmah.ui.viewer

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.platform.FlyerFiles
import org.darulhuda.nabiurrahmah.platform.GallerySaver
import org.darulhuda.nabiurrahmah.platform.LocalFile
import org.darulhuda.nabiurrahmah.ui.navigation.ViewerRoute

enum class ViewerAction { Share, WhatsApp, Save }

data class ViewerUiState(
    val isLoading: Boolean = true,
    val language: Language? = null,
    val initialPage: Int = 0,
    /** The action in progress, if any. Only one runs at a time. */
    val busy: ViewerAction? = null,
)

sealed interface ViewerEvent {
    data class Share(val file: LocalFile, val flyer: Flyer, val toWhatsApp: Boolean) : ViewerEvent
    data class Message(@StringRes val text: Int) : ViewerEvent
}

class ViewerViewModel(
    savedStateHandle: SavedStateHandle,
    repository: CatalogRepository,
    private val files: FlyerFiles,
    private val saver: GallerySaver,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ViewerRoute>()
    private val busy = MutableStateFlow<ViewerAction?>(null)
    private val _events = Channel<ViewerEvent>(Channel.BUFFERED)

    val events: Flow<ViewerEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ViewerUiState> =
        combine(repository.state, busy) { state, busy ->
            val language = state.catalog?.language(route.languageCode)
            ViewerUiState(
                isLoading = state.isLoading,
                language = language,
                initialPage = language?.flyers?.indexOfFirst { it.id == route.flyerId }?.coerceAtLeast(0) ?: 0,
                busy = busy,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewerUiState())

    val needsStoragePermission: Boolean get() = saver.needsPermission

    fun share(flyer: Flyer, toWhatsApp: Boolean = false) =
        perform(if (toWhatsApp) ViewerAction.WhatsApp else ViewerAction.Share) {
            _events.send(ViewerEvent.Share(bestFile(flyer), flyer, toWhatsApp))
        }

    fun save(flyer: Flyer) = perform(ViewerAction.Save) {
        val file = bestFile(flyer)
        saver.save(file)
        _events.send(ViewerEvent.Message(if (file.isPdf) R.string.message_saved_pdf else R.string.message_saved_image))
    }

    /** The full-size file, or the preview if the full size isn't available on the site. */
    private suspend fun bestFile(flyer: Flyer): LocalFile =
        try {
            files.download(flyer.image, fileName(flyer))
        } catch (e: IOException) {
            val preview = flyer.thumbnail ?: throw e
            files.download(preview, fileName(flyer) + "-preview")
        }

    private fun fileName(flyer: Flyer) = "nabi-ur-rahmah-${route.languageCode}-${flyer.id}"

    private fun perform(action: ViewerAction, block: suspend () -> Unit) {
        if (busy.value != null) return
        busy.value = action
        viewModelScope.launch {
            try {
                block()
            } catch (e: IOException) {
                _events.send(ViewerEvent.Message(R.string.message_download_failed))
            } catch (e: SecurityException) {
                _events.send(ViewerEvent.Message(R.string.message_save_failed))
            } finally {
                busy.value = null
            }
        }
    }
}
