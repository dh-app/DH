package org.darulhuda.nabiurrahmah.ui.library

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.library.Book
import org.darulhuda.nabiurrahmah.data.library.Edition
import org.darulhuda.nabiurrahmah.data.library.LibraryRepository
import org.darulhuda.nabiurrahmah.data.library.ShelfId
import org.darulhuda.nabiurrahmah.platform.BookDownloads
import org.darulhuda.nabiurrahmah.platform.DownloadStatus
import org.darulhuda.nabiurrahmah.platform.GallerySaver
import org.darulhuda.nabiurrahmah.platform.LocalFile
import org.darulhuda.nabiurrahmah.platform.PageRatios
import org.darulhuda.nabiurrahmah.platform.PdfDocuments
import org.darulhuda.nabiurrahmah.platform.Preferences
import org.darulhuda.nabiurrahmah.platform.shareFile
import org.darulhuda.nabiurrahmah.platform.shareFileToWhatsApp
import org.darulhuda.nabiurrahmah.ui.navigation.BookRoute
import org.darulhuda.nabiurrahmah.ui.navigation.ReaderRoute
import org.darulhuda.nabiurrahmah.ui.navigation.ShelfRoute

/** The reader's languages, most preferred first. */
internal fun preferredLanguages(): List<String> = listOf(Locale.getDefault().language, "en").distinct()

internal fun shelfOf(key: String): ShelfId = ShelfId.entries.firstOrNull { it.key == key } ?: ShelfId.Books

/** A book as a card: the edition shown depends on the language filter. */
data class BookCard(val book: Book, val edition: Edition)

data class LanguageFilter(val code: String, val count: Int)

data class ShelfUiState(
    val shelf: ShelfId = ShelfId.Books,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val offline: Boolean = false,
    val comingSoon: Boolean = false,
    val query: String = "",
    val language: String? = null,
    val languages: List<LanguageFilter> = emptyList(),
    val cards: List<BookCard> = emptyList(),
    val totalBooks: Int = 0,
)

class ShelfViewModel(savedStateHandle: SavedStateHandle, private val library: LibraryRepository) : ViewModel() {

    private val shelf = shelfOf(savedStateHandle.toRoute<ShelfRoute>().shelf)
    private val query = MutableStateFlow("")
    private val language = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ShelfUiState> = combine(library.state(shelf), query, language) { state, query, language ->
        val books = state.shelf?.books.orEmpty()
        val preferred = preferredLanguages()
        // Languages by how many books they have, the reader's own language first.
        val languages = books.flatMap { it.languages }.groupingBy { it }.eachCount()
            .map { (code, count) -> LanguageFilter(code, count) }
            .sortedWith(compareBy<LanguageFilter> { preferred.indexOf(it.code).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenByDescending { it.count })
        val cards = books.mapNotNull { book ->
            val edition = if (language != null) book.editions.firstOrNull { it.language == language } else book.preferredEdition(preferred)
            edition?.let { BookCard(book, it) }
        }.filter { card ->
            query.isBlank() || card.book.editions.any { edition ->
                edition.title.contains(query.trim(), ignoreCase = true) || edition.author?.contains(query.trim(), ignoreCase = true) == true
            }
        }
        ShelfUiState(
            shelf = shelf,
            isLoading = state.isLoading,
            isRefreshing = state.isRefreshing && state.shelf != null,
            offline = state.offline && books.isEmpty(),
            comingSoon = state.comingSoon,
            query = query,
            language = language,
            languages = languages,
            cards = cards,
            totalBooks = books.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShelfUiState(shelf = shelf))

    init {
        viewModelScope.launch { library.load(shelf) }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onLanguageSelected(code: String?) {
        language.value = code
    }

    fun refresh() {
        viewModelScope.launch { library.refresh(shelf) }
    }
}

sealed interface LibraryEvent {
    data class Share(val file: LocalFile, val text: String, val toWhatsApp: Boolean) : LibraryEvent
    data class Message(val text: Int) : LibraryEvent
}

data class BookUiState(
    val book: Book? = null,
    val preferred: Edition? = null,
    val downloads: Map<String, DownloadStatus> = emptyMap(),
)

class BookViewModel(
    savedStateHandle: SavedStateHandle,
    library: LibraryRepository,
    private val downloads: BookDownloads,
    private val saver: GallerySaver,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<BookRoute>()
    private val shelf = shelfOf(route.shelf)
    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryEvent> = _events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BookUiState> = library.state(shelf)
        .map { state -> state.shelf?.books?.firstOrNull { it.id == route.bookId } }
        .flatMapLatest { book ->
            val urls = book?.editions?.mapNotNull { it.pdf?.url }.orEmpty()
            val statuses = if (urls.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(urls.map { url -> downloads.status(url).map { url to it } }) { it.toMap() }
            }
            statuses.map { BookUiState(book, book?.preferredEdition(preferredLanguages()), it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookUiState())

    fun download(edition: Edition) {
        edition.pdf?.url?.let(downloads::start)
    }

    fun delete(edition: Edition) {
        edition.pdf?.url?.let(downloads::delete)
    }

    fun share(edition: Edition, shareText: String, toWhatsApp: Boolean) = withFile(edition) { file ->
        _events.send(LibraryEvent.Share(downloads.shareable(file, edition.title), shareText, toWhatsApp))
    }

    fun saveToDevice(edition: Edition) = withFile(edition) { file ->
        saver.save(downloads.shareable(file, edition.title))
        _events.send(LibraryEvent.Message(R.string.message_saved_pdf))
    }

    private fun withFile(edition: Edition, block: suspend (File) -> Unit) {
        val url = edition.pdf?.url ?: return
        viewModelScope.launch {
            try {
                block(downloads.get(url))
            } catch (e: IOException) {
                _events.send(LibraryEvent.Message(R.string.message_download_failed))
            } catch (e: SecurityException) {
                _events.send(LibraryEvent.Message(R.string.message_save_failed))
            }
        }
    }
}

sealed interface ReaderUiState {
    data class Downloading(val progress: Float?) : ReaderUiState
    data class Ready(val path: String, val pages: PageRatios, val initialPage: Int) : ReaderUiState
    data object Failed : ReaderUiState
}

class ReaderViewModel(
    savedStateHandle: SavedStateHandle,
    private val downloads: BookDownloads,
    private val documents: PdfDocuments,
    private val preferences: Preferences,
    private val saver: GallerySaver,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ReaderRoute>()
    val title: String = route.title

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Downloading(null))
    val state: StateFlow<ReaderUiState> = _state

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryEvent> = _events.receiveAsFlow()

    init {
        open()
    }

    fun open() {
        _state.value = ReaderUiState.Downloading(null)
        viewModelScope.launch {
            val progress = launch {
                downloads.status(route.url).collect { status ->
                    if (status is DownloadStatus.Downloading) _state.value = ReaderUiState.Downloading(status.progress)
                }
            }
            _state.value = try {
                val file = downloads.get(route.url)
                val pages = documents.pageRatios(file)
                ReaderUiState.Ready(file.path, pages, preferences.lastPage(route.url).coerceIn(0, (pages.size - 1).coerceAtLeast(0)))
            } catch (e: IOException) {
                ReaderUiState.Failed
            } catch (e: SecurityException) {
                ReaderUiState.Failed
            } finally {
                progress.cancel()
            }
        }
    }

    fun onPageShown(page: Int) = preferences.saveLastPage(route.url, page)

    fun share(text: String, toWhatsApp: Boolean) = withFile { file ->
        _events.send(LibraryEvent.Share(downloads.shareable(file, title), text, toWhatsApp))
    }

    fun saveToDevice() = withFile { file ->
        saver.save(downloads.shareable(file, title))
        _events.send(LibraryEvent.Message(R.string.message_saved_pdf))
    }

    private fun withFile(block: suspend (File) -> Unit) {
        viewModelScope.launch {
            try {
                block(downloads.get(route.url))
            } catch (e: IOException) {
                _events.send(LibraryEvent.Message(R.string.message_download_failed))
            } catch (e: SecurityException) {
                _events.send(LibraryEvent.Message(R.string.message_save_failed))
            }
        }
    }
}

/** Opens a shared file in WhatsApp when asked, otherwise in the share sheet. */
internal fun Context.shareLibraryFile(event: LibraryEvent.Share, chooserTitle: String) {
    if (event.toWhatsApp) {
        shareFileToWhatsApp(event.file, event.text, chooserTitle)
    } else {
        shareFile(event.file, event.text, chooserTitle)
    }
}
