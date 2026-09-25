package org.darulhuda.nabiurrahmah.ui.videos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.CatalogState
import org.darulhuda.nabiurrahmah.data.model.Playlist
import org.darulhuda.nabiurrahmah.data.model.Video
import org.darulhuda.nabiurrahmah.ui.navigation.PlaylistRoute
import org.darulhuda.nabiurrahmah.ui.navigation.VideoRoute

data class PlaylistUiState(
    val isLoading: Boolean = true,
    val playlist: Playlist? = null,
)

private fun CatalogState.playlist(id: String) = catalog?.playlists?.firstOrNull { it.id == id }

private fun CatalogState.stillLoading() = isLoading || (catalog?.playlists.isNullOrEmpty() && isRefreshing)

class PlaylistViewModel(savedStateHandle: SavedStateHandle, repository: CatalogRepository) : ViewModel() {

    private val playlistId = savedStateHandle.toRoute<PlaylistRoute>().playlistId

    val uiState: StateFlow<PlaylistUiState> = repository.state
        .map { PlaylistUiState(isLoading = it.stillLoading(), playlist = it.playlist(playlistId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistUiState())
}

data class VideoUiState(
    val isLoading: Boolean = true,
    val playlist: Playlist? = null,
    val current: Video? = null,
)

class VideoViewModel(
    private val savedStateHandle: SavedStateHandle,
    repository: CatalogRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<VideoRoute>()
    private val currentId = savedStateHandle.getStateFlow(KEY_CURRENT, route.videoId)

    val uiState: StateFlow<VideoUiState> =
        combine(repository.state, currentId) { state, id ->
            val playlist = state.playlist(route.playlistId)
            VideoUiState(
                isLoading = state.stillLoading(),
                playlist = playlist,
                current = playlist?.videos?.firstOrNull { it.id == id } ?: playlist?.videos?.firstOrNull(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VideoUiState())

    fun play(video: Video) {
        savedStateHandle[KEY_CURRENT] = video.id
    }

    /** Continues with the next video in the playlist, if there is one. */
    fun playNext() {
        val state = uiState.value
        val videos = state.playlist?.videos ?: return
        val index = videos.indexOfFirst { it.id == state.current?.id }
        videos.getOrNull(index + 1)?.let(::play)
    }

    private companion object {
        const val KEY_CURRENT = "currentVideoId"
    }
}
