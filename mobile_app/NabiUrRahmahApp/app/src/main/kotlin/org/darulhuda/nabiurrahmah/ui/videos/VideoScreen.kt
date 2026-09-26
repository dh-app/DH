package org.darulhuda.nabiurrahmah.ui.videos

import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.model.Playlist
import org.darulhuda.nabiurrahmah.data.model.Video
import org.darulhuda.nabiurrahmah.platform.openUrl
import org.darulhuda.nabiurrahmah.platform.LocalPictureInPicture
import org.darulhuda.nabiurrahmah.platform.shareText
import org.darulhuda.nabiurrahmah.platform.shareTextToWhatsApp
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.IconButton
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.BackButton
import org.darulhuda.nabiurrahmah.ui.common.ImmersiveSystemBars
import org.darulhuda.nabiurrahmah.ui.common.MessageState
import org.darulhuda.nabiurrahmah.ui.common.SectionTitle

/** The full-screen view the player hands over, and how to leave it. */
private class FullscreenSession(val view: View, val exit: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    onBack: () -> Unit,
    viewModel: VideoViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var fullscreen by remember { mutableStateOf<FullscreenSession?>(null) }
    val playlist = state.playlist
    val current = state.current
    // While this screen is open, leaving the app shrinks the video into a floating window.
    val pictureInPicture = LocalPictureInPicture.current
    DisposableEffect(pictureInPicture) {
        pictureInPicture.wanted = true
        onDispose { pictureInPicture.wanted = false }
    }
    val compact = pictureInPicture.active

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (!compact) TopAppBar(
                    title = {
                        Text(
                            text = playlist?.title?.ifBlank { null } ?: stringResource(R.string.videos_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = { BackButton(onBack) },
                )
            },
        ) { padding ->
            when {
                playlist != null && current != null -> VideoContent(
                    playlist = playlist,
                    current = current,
                    contentPadding = padding,
                    compact = compact,
                    onPlay = viewModel::play,
                    onEnded = viewModel::playNext,
                    onEnterFullscreen = { view, exit -> fullscreen = FullscreenSession(view, exit) },
                    onExitFullscreen = { fullscreen = null },
                )
                state.isLoading -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                else -> MessageState(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.language_unavailable_title),
                    body = stringResource(R.string.playlist_unavailable_body),
                    modifier = Modifier.padding(padding),
                )
            }
        }

        fullscreen?.let { session ->
            FullscreenVideo(session, Modifier.zIndex(1f))
        }
    }
}

@Composable
private fun VideoContent(
    playlist: Playlist,
    current: Video,
    contentPadding: PaddingValues,
    compact: Boolean,
    onPlay: (Video) -> Unit,
    onEnded: () -> Unit,
    onEnterFullscreen: (View, () -> Unit) -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val context = LocalContext.current
    val shareChooser = stringResource(R.string.share_video_chooser)
    // Owners can switch off embedding; then the only way to watch is the YouTube app.
    var blockedVideoId by rememberSaveable { mutableStateOf<String?>(null) }
    val shortMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.62f

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = if (compact) 0.dp else contentPadding.calculateTopPadding()),
    ) {
        if (blockedVideoId == current.id) {
            EmbedBlocked(onWatch = { context.openUrl(current.watchUrl(playlist.id)) })
        } else {
            YouTubeVideoPlayer(
                videoId = current.id,
                onEnded = onEnded,
                onUnplayable = { blockedVideoId = current.id },
                onEnterFullscreen = onEnterFullscreen,
                onExitFullscreen = onExitFullscreen,
                // Wide videos fill the width at 16:9. Shorts stand upright at 9:16, centred on black and
                // capped so the title and the playlist stay in view. Picture-in-picture fills the window.
                modifier = when {
                    compact -> Modifier
                        .fillMaxWidth()
                        .weight(1f)
                    current.isShort -> Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .heightIn(max = shortMaxHeight)
                        .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                    else -> Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                },
            )
        }

        // In picture-in-picture only the player shows; it stays the same view, so playback never restarts.
        if (!compact) LazyColumn(
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(key = "details") {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    Text(current.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    val shareMessage = "${current.title}\n${current.watchUrl()}"
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { context.shareTextToWhatsApp(shareMessage, shareChooser) }) {
                            Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_whatsapp))
                        }
                        IconButton(onClick = { context.shareText(shareMessage, shareChooser) }) {
                            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share))
                        }
                        IconButton(onClick = { context.openUrl(current.watchUrl(playlist.id)) }) {
                            Icon(Icons.Outlined.SmartDisplay, contentDescription = stringResource(R.string.action_watch_on_youtube))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    SectionTitle(stringResource(R.string.up_next))
                }
            }
            items(playlist.videos, key = { it.id }) { video ->
                VideoRow(video, selected = video.id == current.id, onClick = { onPlay(video) })
            }
        }
    }
}

@Composable
private fun EmbedBlocked(onWatch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.video_embed_blocked),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onWatch) { Text(stringResource(R.string.action_watch_on_youtube)) }
    }
}

/** Landscape, edge to edge, system bars hidden; back leaves full screen first. */
@Composable
private fun FullscreenVideo(session: FullscreenSession, modifier: Modifier = Modifier) {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity?.requestedOrientation = previous }
    }
    ImmersiveSystemBars(barsVisible = false)
    BackHandler(onBack = session.exit)
    AndroidView(
        factory = {
            (session.view.parent as? ViewGroup)?.removeView(session.view)
            session.view
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    )
}
