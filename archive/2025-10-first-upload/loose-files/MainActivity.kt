package org.darulhuda.udupi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import org.darulhuda.udupi.ui.theme.NabiUrRahmahAppTheme
import org.darulhuda.udupi.util.AppNavHost

/**
 * 🚀 MainActivity — Entry point of the Nabi-ur-Rahmah App
 * Uses Jetpack Compose Navigation and custom Material 3 theme.
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      NabiUrRahmahAppTheme {
        val navController = rememberNavController()
        AppNavHost(navController = navController)
      }
    }
  }
}
