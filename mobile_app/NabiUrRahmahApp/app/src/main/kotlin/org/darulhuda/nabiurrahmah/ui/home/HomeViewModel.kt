package org.darulhuda.nabiurrahmah.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.ui.common.UserRefresh

data class HomeUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isRefreshing: Boolean = false,
    val query: String = "",
    val languages: List<Language> = emptyList(),
    val languageCount: Int = 0,
    val flyerCount: Int = 0,
)

class HomeViewModel(repository: CatalogRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val userRefresh = UserRefresh(repository, viewModelScope)

    val messages: Flow<Int> = userRefresh.messages

    val uiState: StateFlow<HomeUiState> =
        combine(repository.state, query, userRefresh.isRefreshing) { state, query, refreshing ->
            val all = state.catalog?.languages.orEmpty()
            HomeUiState(
                isLoading = state.isLoading,
                loadFailed = state.catalog == null && state.error != null,
                isRefreshing = refreshing,
                query = query,
                languages = all.filter { it.matches(query) },
                languageCount = all.size,
                flyerCount = state.catalog?.flyerCount ?: 0,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun refresh() = userRefresh.refresh()
}

internal fun Language.matches(query: String): Boolean {
    val q = query.trim()
    return q.isEmpty() ||
        name.contains(q, ignoreCase = true) ||
        nativeName.contains(q, ignoreCase = true) ||
        code.equals(q, ignoreCase = true)
}
