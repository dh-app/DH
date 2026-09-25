package org.darulhuda.nabiurrahmah.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import java.io.IOException

/**
 * Plays the salawat recording (assets/salawat.mp3) once when the app opens.
 * Stays quiet when the phone is on silent or vibrate, when the person turned it
 * off, or when no recording is bundled.
 */
class SalawatPlayer(
    private val context: Context,
    private val preferences: Preferences,
) {
    private var player: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    val isAvailable: Boolean by lazy {
        try {
            context.assets.list("")?.contains(ASSET) == true
        } catch (e: IOException) {
            false
        }
    }

    fun playOnce() {
        if (!preferences.playSalawatOnOpen || !isAvailable || player != null) return
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) return
        if (!requestFocus()) return

        try {
            val media = MediaPlayer()
            context.assets.openFd(ASSET).use { media.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            media.setAudioAttributes(ATTRIBUTES)
            media.setOnCompletionListener { stop() }
            media.setOnErrorListener { _, _, _ ->
                stop()
                true
            }
            media.setOnPreparedListener { it.start() }
            media.prepareAsync()
            player = media
        } catch (e: IOException) {
            stop()
        }
    }

    fun stop() {
        player?.release()
        player = null
        abandonFocus()
    }

    private fun requestFocus(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(ATTRIBUTES)
                .setOnAudioFocusChangeListener { change -> if (change == AudioManager.AUDIOFOCUS_LOSS) stop() }
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let(audioManager::abandonAudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        focusRequest = null
    }

    private companion object {
        const val ASSET = "salawat.mp3"
        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
}
