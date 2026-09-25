package org.darulhuda.nabiurrahmah.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Lets the video screen ask for picture-in-picture, and tells it when it is in it. */
class PictureInPictureState {
    /** A video is on screen and playing, so leaving the app should shrink it into a window. */
    var wanted by mutableStateOf(false)

    var active by mutableStateOf(false)
}

val LocalPictureInPicture = staticCompositionLocalOf { PictureInPictureState() }
