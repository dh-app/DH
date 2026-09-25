package org.darulhuda.nabiurrahmah.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.darulhuda.nabiurrahmah.AppContainer
import org.darulhuda.nabiurrahmah.NabiApp
import org.darulhuda.nabiurrahmah.ui.about.AboutViewModel
import org.darulhuda.nabiurrahmah.ui.flyers.FlyersViewModel
import org.darulhuda.nabiurrahmah.ui.home.HomeViewModel
import org.darulhuda.nabiurrahmah.ui.videos.PlaylistViewModel
import org.darulhuda.nabiurrahmah.ui.videos.VideoViewModel
import org.darulhuda.nabiurrahmah.ui.viewer.ViewerViewModel

/** Builds every ViewModel from the app's [AppContainer]. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { HomeViewModel(container().catalogRepository) }
        initializer { FlyersViewModel(createSavedStateHandle(), container().catalogRepository) }
        initializer {
            val container = container()
            ViewerViewModel(
                savedStateHandle = createSavedStateHandle(),
                repository = container.catalogRepository,
                files = container.flyerFiles,
                saver = container.gallerySaver,
            )
        }
        initializer { AboutViewModel(container().about) }
        initializer { PlaylistViewModel(createSavedStateHandle(), container().catalogRepository) }
        initializer { VideoViewModel(createSavedStateHandle(), container().catalogRepository) }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NabiApp).container
}
