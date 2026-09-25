package org.darulhuda.nabiurrahmah.ui.about

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.darulhuda.nabiurrahmah.data.model.About
import org.darulhuda.nabiurrahmah.platform.Preferences

class AboutViewModel(
    val about: About,
    private val preferences: Preferences,
    /** Only offer the setting when a recording is bundled. */
    val salawatAvailable: Boolean,
) : ViewModel() {

    private val _playSalawat = MutableStateFlow(preferences.playSalawatOnOpen)
    val playSalawat: StateFlow<Boolean> = _playSalawat

    fun setPlaySalawat(enabled: Boolean) {
        preferences.playSalawatOnOpen = enabled
        _playSalawat.value = enabled
    }
}
