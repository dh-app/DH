package org.darulhuda.nabiurrahmah.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.darulhuda.nabiurrahmah.data.CatalogRepository
import org.darulhuda.nabiurrahmah.data.model.About

class AboutViewModel(repository: CatalogRepository) : ViewModel() {

    /** Contact details live in the catalogue, so they can change without an app update. */
    val about: StateFlow<About?> = repository.state
        .map { it.catalog?.about }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.state.value.catalog?.about)
}
