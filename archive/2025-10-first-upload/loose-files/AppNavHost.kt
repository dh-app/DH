package org.darulhuda.udupi.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.darulhuda.udupi.ui.*

/**
 * 🧭 AppNavHost — Central navigation graph for the Nabi-ur-Rahmah App.
 * Preserves smooth Material3-like transitions for a modern UX.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavHost(navController: NavHostController) {

  NavHost(
    navController = navController,
    startDestination = "home",
    enterTransition = { fadeInSlideIn() },
    exitTransition = { fadeOutSlideOut() },
    popEnterTransition = { fadeInSlideIn(reverse = true) },
    popExitTransition = { fadeOutSlideOut(reverse = true) }
  ) {

    // 🏠 Home
    composable("home") {
      HomeScreen(
        onNavigateToQPC = { navController.navigate("qpc") },
        onNavigateToNabi = { navController.navigate("nabiurrahmah") },   // 👈 renamed route
        onNavigateToLibrary = { navController.navigate("library") },
        onNavigateToProjects = { navController.navigate("projects") },
        onNavigateToAbout = { navController.navigate("about") },
        onNavigateToContact = { navController.navigate("contact") }
      )
    }

    // 🕌 Qur'an Printing Complex
    composable("qpc") {
      QpcScreen(onBack = { navController.popBackStack() })
    }

    // ﷺ Nabi-ur-Rahmah — Language grid
    composable("nabiurrahmah") {
      NabiUrRahmahScreen(
        onBack = { navController.popBackStack() },
        onOpenLanguage = { code -> navController.navigate("nabiurrahmah/$code") }
      )
    }

    // ﷺ Nabi-ur-Rahmah — Flyer list for a selected language
    composable(
      route = "nabiurrahmah/{code}",
      arguments = listOf(navArgument("code") { type = NavType.StringType })
    ) { backStack ->
      val code = backStack.arguments?.getString("code") ?: ""
      FlyerListScreen(
        languageCode = code,
        onBack = { navController.popBackStack() }
      )
    }

    // 📚 Library
    composable("library") { LibraryScreen(onBack = { navController.popBackStack() }) }

    // 🏗 Projects
    composable("projects") { ProjectsScreen(onBack = { navController.popBackStack() }) }

    // ℹ About
    composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }

    // ☎ Contact
    composable("contact") { ContactScreen(onBack = { navController.popBackStack() }) }
  }
}

/**
 * ✨ Transition Animations
 * Fade + horizontal slide for a premium feel.
 */
@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<*>.fadeInSlideIn(
  reverse: Boolean = false
): EnterTransition {
  val dir =
    if (reverse) AnimatedContentTransitionScope.SlideDirection.Right
    else AnimatedContentTransitionScope.SlideDirection.Left
  return slideIntoContainer(dir, animationSpec = tween(400)) +
          androidx.compose.animation.fadeIn(animationSpec = tween(400))
}

@OptIn(ExperimentalAnimationApi::class)
private fun AnimatedContentTransitionScope<*>.fadeOutSlideOut(
  reverse: Boolean = false
): ExitTransition {
  val dir =
    if (reverse) AnimatedContentTransitionScope.SlideDirection.Right
    else AnimatedContentTransitionScope.SlideDirection.Left
  return slideOutOfContainer(dir, animationSpec = tween(400)) +
          androidx.compose.animation.fadeOut(animationSpec = tween(400))
}
