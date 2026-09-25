package org.darulhuda.nabiurrahmah.ui.videos

import android.view.View
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * The official YouTube player (IFrame API), embedded. Pauses with the screen,
 * resumes where it was, and hands over its full-screen view via [onEnterFullscreen].
 */
@Composable
fun YouTubeVideoPlayer(
    videoId: String,
    onEnded: () -> Unit,
    onUnplayable: () -> Unit,
    onEnterFullscreen: (view: View, exit: () -> Unit) -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestOnEnded by rememberUpdatedState(onEnded)
    val latestOnUnplayable by rememberUpdatedState(onUnplayable)
    val latestOnEnterFullscreen by rememberUpdatedState(onEnterFullscreen)
    val latestOnExitFullscreen by rememberUpdatedState(onExitFullscreen)

    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    // Remembered per video, so returning to the screen continues instead of restarting.
    var position by rememberSaveable(videoId) { mutableFloatStateOf(0f) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        factory = { context ->
            YouTubePlayerView(context).apply {
                enableAutomaticInitialization = false
                lifecycle.addObserver(this)
                addFullscreenListener(
                    object : FullscreenListener {
                        override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) =
                            latestOnEnterFullscreen(fullscreenView, exitFullscreen)

                        override fun onExitFullscreen() = latestOnExitFullscreen()
                    },
                )
                val options = IFramePlayerOptions.Builder(context)
                    .controls(1)
                    .fullscreen(1)
                    .rel(0)
                    .ivLoadPolicy(3)
                    .build()
                initialize(
                    object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            player = youTubePlayer
                        }

                        override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                            if (state == PlayerConstants.PlayerState.ENDED) latestOnEnded()
                        }

                        override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                            if (error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ||
                                error == PlayerConstants.PlayerError.VIDEO_NOT_FOUND
                            ) {
                                latestOnUnplayable()
                            }
                        }

                        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                            position = second
                        }
                    },
                    true,
                    options,
                )
            }
        },
        onRelease = { view ->
            lifecycle.removeObserver(view)
            view.release()
        },
    )

    LaunchedEffect(player, videoId) {
        player?.loadVideo(videoId, position)
    }
}
