package org.darulhuda.nabiurrahmah.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.library.LibraryRepository
import org.darulhuda.nabiurrahmah.data.library.ShelfId
import org.darulhuda.nabiurrahmah.data.library.ShelfState

/** What each home tile shows as its live count. */
data class ShelfSummary(val books: Int = 0, val languages: Int = 0, val comingSoon: Boolean = false)

data class HomeUiState(
    val languageCount: Int = 0,
    val flyerCount: Int = 0,
    val videoCount: Int = 0,
    val shelves: Map<ShelfId, ShelfSummary> = emptyMap(),
)

class HomeViewModel(
    catalog: CatalogRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        catalog.state,
        library.state(ShelfId.Biography),
        library.state(ShelfId.Testimonies),
        library.state(ShelfId.Books),
    ) { state, biography, testimonies, books ->
        HomeUiState(
            languageCount = state.catalog?.languages?.size ?: 0,
            flyerCount = state.catalog?.flyerCount ?: 0,
            videoCount = state.catalog?.playlists?.sumOf { it.videos.size } ?: 0,
            shelves = mapOf(
                ShelfId.Biography to biography.summary(),
                ShelfId.Testimonies to testimonies.summary(),
                ShelfId.Books to books.summary(),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Warm the shelves in the background so opening one is instant.
        ShelfId.entries.forEach { shelf -> viewModelScope.launch { library.load(shelf) } }
    }

    private fun ShelfState.summary() = ShelfSummary(
        books = shelf?.books?.size ?: 0,
        languages = shelf?.books?.flatMap { it.languages }?.distinct()?.size ?: 0,
        // Nothing configured, or nothing published yet.
        comingSoon = comingSoon || shelf?.books?.isEmpty() == true,
    )
}
