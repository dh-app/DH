package org.darulhuda.nabiurrahmah

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.darulhuda.nabiurrahmah.platform.LocalPictureInPicture
import org.darulhuda.nabiurrahmah.platform.PictureInPictureState
import org.darulhuda.nabiurrahmah.ui.navigation.NurNavHost
import org.darulhuda.nabiurrahmah.ui.theme.NurTheme

class MainActivity : ComponentActivity() {

    private val pictureInPicture = PictureInPictureState()
    private val container get() = (application as NabiApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // A fresh start opens with the Durood (and the salawat, if enabled);
        // rotating or returning from the background does not repeat it.
        val freshStart = savedInstanceState == null
        if (freshStart) container.salawatPlayer.playOnce()

        addOnPictureInPictureModeChangedListener { info -> pictureInPicture.active = info.isInPictureInPictureMode }

        enableEdgeToEdge()
        setContent {
            NurTheme {
                CompositionLocalProvider(LocalPictureInPicture provides pictureInPicture) {
                    NurNavHost(showLanding = freshStart)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Coming back to the app later picks up flyers published in the meantime.
        (application as NabiApp).refreshIfStale()
    }

    /** Leaving the app while a video plays keeps it playing in a small window. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (pictureInPicture.wanted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode) {
            try {
                enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
            } catch (e: IllegalStateException) {
                // Picture-in-picture is turned off for this app in system settings.
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) container.salawatPlayer.stop()
    }
}
