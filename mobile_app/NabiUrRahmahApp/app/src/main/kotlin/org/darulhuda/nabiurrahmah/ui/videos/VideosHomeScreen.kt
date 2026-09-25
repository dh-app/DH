package org.darulhuda.nabiurrahmah.ui.videos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.model.Playlist
import org.darulhuda.nabiurrahmah.data.site.KnownLanguages
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.BackButton
import org.darulhuda.nabiurrahmah.ui.common.MessageState
import org.darulhuda.nabiurrahmah.ui.common.UserRefresh

data class VideosUiState(
    val isLoading: Boolean = true,
    val offline: Boolean = false,
    val isRefreshing: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
)

class VideosHomeViewModel(repository: CatalogRepository) : ViewModel() {

    private val userRefresh = UserRefresh(repository, viewModelScope)

    val uiState: StateFlow<VideosUiState> = combine(repository.state, userRefresh.isRefreshing) { state, refreshing ->
        val playlists = state.catalog?.playlists.orEmpty()
        VideosUiState(
            isLoading = playlists.isEmpty() && (state.isLoading || state.isRefreshing),
            offline = playlists.isEmpty() && !state.isRefreshing && state.error != null,
            isRefreshing = refreshing,
            playlists = playlists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VideosUiState())

    fun refresh() = userRefresh.refresh()
}

/** Tile 2: the teachings as videos, one section per playlist, labelled by language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosHomeScreen(
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenVideo: (playlistId: String, videoId: String) -> Unit,
    viewModel: VideosHomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.tile_teachings), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            when {
                state.playlists.isNotEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "summary") {
                        Text(
                            text = stringResource(R.string.tile_videos_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    items(state.playlists, key = { it.id }) { playlist ->
                        PlaylistSection(
                            playlist = playlist,
                            onPlayAll = { playlist.videos.firstOrNull()?.let { onOpenVideo(playlist.id, it.id) } },
                            onSeeAll = { onOpenPlaylist(playlist.id) },
                            onOpenVideo = { video -> onOpenVideo(playlist.id, video.id) },
                        )
                    }
                }
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> MessageState(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.error_offline_title),
                    body = stringResource(R.string.playlist_unavailable_body),
                    action = { FilledTonalButton(onClick = viewModel::refresh) { Text(stringResource(R.string.action_retry)) } },
                )
            }
        }
    }
}

@Composable
private fun PlaylistSection(
    playlist: Playlist,
    onPlayAll: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenVideo: (org.darulhuda.nabiurrahmah.data.model.Video) -> Unit,
) {
    val language = KnownLanguages.mentionedIn(playlist.title)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (language != null) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = language.nativeName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            AssistChip(
                onClick = onPlayAll,
                label = { Text(stringResource(R.string.action_play_all)) },
                leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        PlaylistCarousel(playlist = playlist, onSeeAll = onSeeAll, onOpenVideo = onOpenVideo, modifier = Modifier.padding(top = 8.dp))
    }
}
