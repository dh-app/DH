package org.darulhuda.nabiurrahmah.ui.flyers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.ui.common.UserRefresh
import org.darulhuda.nabiurrahmah.ui.navigation.FlyersRoute

data class FlyersUiState(
    val isLoading: Boolean = true,
    val language: Language? = null,
    /** Loading finished but there is nothing to show for this language code. */
    val unavailable: Boolean = false,
    val isRefreshing: Boolean = false,
)

class FlyersViewModel(
    savedStateHandle: SavedStateHandle,
    repository: CatalogRepository,
) : ViewModel() {

    val languageCode: String = savedStateHandle.toRoute<FlyersRoute>().languageCode

    private val userRefresh = UserRefresh(repository, viewModelScope)

    val messages: Flow<Int> = userRefresh.messages

    val uiState: StateFlow<FlyersUiState> =
        combine(repository.state, userRefresh.isRefreshing) { state, refreshing ->
            val language = state.catalog?.language(languageCode)
            val waitingForPage = language != null && language.flyers.isEmpty() && language.code in state.loadingLanguages
            FlyersUiState(
                isLoading = state.isLoading || waitingForPage,
                language = language,
                unavailable = !state.isLoading && language == null,
                isRefreshing = refreshing,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FlyersUiState())

    fun refresh() = userRefresh.refresh()
}
