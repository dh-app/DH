package org.darulhuda.udupi.feature.nabi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.darulhuda.udupi.core.model.Flyer
import org.darulhuda.udupi.core.model.LanguageLink
import org.darulhuda.udupi.core.network.loadFlyersForLanguage
import org.darulhuda.udupi.core.network.loadNabiLanguages

/**
 * 🕌 ViewModel for Nabi ur Rahmah section.
 * Handles language selection and flyer loading.
 */
class NabiViewModel : ViewModel() {

  private val _state = MutableStateFlow(NabiState())
  val state: StateFlow<NabiState> = _state

  init {
    loadLanguages()
  }

  /**
   * Loads all available languages for the Nabi ur Rahmah section.
   */
  private fun loadLanguages() {
    viewModelScope.launch {
      _state.value = _state.value.copy(loading = true, error = null)
      try {
        val langs = loadNabiLanguages()
        _state.value = _state.value.copy(
          languages = langs,
          loading = false
        )
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          error = e.localizedMessage ?: "Failed to load languages",
          loading = false
        )
      }
    }
  }

  /**
   * Loads flyers for the selected language.
   */
  fun loadFlyers(lang: LanguageLink) {
    viewModelScope.launch {
      _state.value = _state.value.copy(
        loading = true,
        selected = lang,
        error = null
      )
      try {
        val flyers = loadFlyersForLanguage(lang)
        _state.value = _state.value.copy(
          flyers = flyers,
          loading = false
        )
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          error = e.localizedMessage ?: "Failed to load flyers",
          loading = false
        )
      }
    }
  }
}

/**
 * 📦 UI state for the Nabi ur Rahmah screen.
 */
data class NabiState(
  val languages: List<LanguageLink> = emptyList(),
  val selected: LanguageLink? = null,
  val flyers: List<Flyer> = emptyList(),
  val loading: Boolean = false,
  val error: String? = null
)
