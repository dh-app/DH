package org.darulhuda.nabiurrahmah.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.darulhuda.nabiurrahmah.ui.about.AboutScreen
import org.darulhuda.nabiurrahmah.ui.flyers.FlyersScreen
import org.darulhuda.nabiurrahmah.ui.home.HomeScreen
import org.darulhuda.nabiurrahmah.ui.videos.PlaylistScreen
import org.darulhuda.nabiurrahmah.ui.videos.VideoScreen
import org.darulhuda.nabiurrahmah.ui.viewer.ViewerScreen

private const val DURATION = 320

@Composable
fun NurNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    // dropUnlessResumed ignores taps that land while a transition is running,
    // so a quick double tap never pushes a screen twice or pops too far.
    val navigateUp = dropUnlessResumed { navController.navigateUp() }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        // Material "shared axis X" motion.
        enterTransition = {
            slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { it / 8 } +
                fadeIn(tween(DURATION, delayMillis = 60))
        },
        exitTransition = {
            slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -it / 8 } +
                fadeOut(tween(DURATION / 2))
        },
        popEnterTransition = {
            slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -it / 8 } +
                fadeIn(tween(DURATION, delayMillis = 60))
        },
        popExitTransition = {
            slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { it / 8 } +
                fadeOut(tween(DURATION / 2))
        },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenLanguage = { code -> navController.navigate(FlyersRoute(code)) },
                onOpenPlaylist = { id -> navController.navigate(PlaylistRoute(id)) },
                onOpenVideo = { playlistId, videoId -> navController.navigate(VideoRoute(playlistId, videoId)) },
                onOpenAbout = dropUnlessResumed { navController.navigate(AboutRoute) },
            )
        }
        composable<FlyersRoute> {
            FlyersScreen(
                onBack = navigateUp,
                onOpenFlyer = { code, id -> navController.navigate(ViewerRoute(code, id)) },
            )
        }
        composable<ViewerRoute>(
            enterTransition = { fadeIn(tween(DURATION)) + scaleIn(tween(DURATION), initialScale = 0.94f) },
            popExitTransition = { fadeOut(tween(DURATION / 2)) },
        ) {
            ViewerScreen(onBack = navigateUp)
        }
        composable<PlaylistRoute> {
            PlaylistScreen(
                onBack = navigateUp,
                onOpenVideo = { playlistId, videoId -> navController.navigate(VideoRoute(playlistId, videoId)) },
            )
        }
        composable<VideoRoute> {
            VideoScreen(onBack = navigateUp)
        }
        composable<AboutRoute> {
            AboutScreen(onBack = navigateUp)
        }
    }
}
