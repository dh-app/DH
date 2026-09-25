package org.darulhuda.nabiurrahmah

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.darulhuda.nabiurrahmah.ui.navigation.NurNavHost
import org.darulhuda.nabiurrahmah.ui.theme.NurTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash briefly while the local catalogue loads, so the first
        // frame already has content. Never longer than SPLASH_MAX_MS.
        val repository = (application as NabiApp).container.catalogRepository
        val start = SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            repository.state.value.isLoading && SystemClock.uptimeMillis() - start < SPLASH_MAX_MS
        }

        enableEdgeToEdge()
        setContent {
            NurTheme {
                NurNavHost()
            }
        }
    }

    private companion object {
        const val SPLASH_MAX_MS = 800L
    }
}
