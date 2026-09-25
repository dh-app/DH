package org.darulhuda.nabiurrahmah.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.darulhuda.nabiurrahmah.data.library.ShelfId
import org.darulhuda.nabiurrahmah.ui.about.AboutScreen
import org.darulhuda.nabiurrahmah.ui.flyers.FlyerLanguagesScreen
import org.darulhuda.nabiurrahmah.ui.flyers.FlyersScreen
import org.darulhuda.nabiurrahmah.ui.home.HomeDestination
import org.darulhuda.nabiurrahmah.ui.home.HomeScreen
import org.darulhuda.nabiurrahmah.ui.landing.LandingOverlay
import org.darulhuda.nabiurrahmah.ui.library.BookScreen
import org.darulhuda.nabiurrahmah.ui.library.ReaderScreen
import org.darulhuda.nabiurrahmah.ui.library.ShelfScreen
import org.darulhuda.nabiurrahmah.ui.videos.PlaylistScreen
import org.darulhuda.nabiurrahmah.ui.videos.VideoScreen
import org.darulhuda.nabiurrahmah.ui.videos.VideosHomeScreen
import org.darulhuda.nabiurrahmah.ui.viewer.ViewerScreen

private const val DURATION = 320

@Composable
fun NurNavHost(showLanding: Boolean, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    // dropUnlessResumed ignores taps that land while a transition is running,
    // so a quick double tap never pushes a screen twice or pops too far.
    val navigateUp = dropUnlessResumed { navController.navigateUp() }
    var landing by rememberSaveable { mutableStateOf(showLanding) }

    Box(modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
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
                    onOpen = { destination ->
                        navController.navigate(
                            when (destination) {
                                HomeDestination.Flyers -> FlyerLanguagesRoute
                                HomeDestination.Videos -> VideosRoute
                                HomeDestination.Biography -> ShelfRoute(ShelfId.Biography.key)
                                HomeDestination.Testimonies -> ShelfRoute(ShelfId.Testimonies.key)
                                HomeDestination.Books -> ShelfRoute(ShelfId.Books.key)
                                HomeDestination.About -> AboutRoute
                            },
                        )
                    },
                )
            }
            composable<FlyerLanguagesRoute> {
                FlyerLanguagesScreen(
                    onBack = navigateUp,
                    onOpenLanguage = { code -> navController.navigate(FlyersRoute(code)) },
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
                ViewerScreen(
                    onBack = navigateUp,
                    onOpenPdf = { url, title -> navController.navigate(ReaderRoute(url, title)) },
                )
            }
            composable<VideosRoute> {
                VideosHomeScreen(
                    onBack = navigateUp,
                    onOpenPlaylist = { id -> navController.navigate(PlaylistRoute(id)) },
                    onOpenVideo = { playlistId, videoId -> navController.navigate(VideoRoute(playlistId, videoId)) },
                )
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
            composable<ShelfRoute> {
                ShelfScreen(
                    onBack = navigateUp,
                    onOpenBook = { shelf, bookId -> navController.navigate(BookRoute(shelf, bookId)) },
                )
            }
            composable<BookRoute> {
                BookScreen(
                    onBack = navigateUp,
                    onRead = { url, title -> navController.navigate(ReaderRoute(url, title)) },
                )
            }
            composable<ReaderRoute> {
                ReaderScreen(onBack = navigateUp)
            }
            composable<AboutRoute> {
                AboutScreen(onBack = navigateUp)
            }
        }

        if (landing) {
            LandingOverlay(onFinished = { landing = false })
        }
    }
}
