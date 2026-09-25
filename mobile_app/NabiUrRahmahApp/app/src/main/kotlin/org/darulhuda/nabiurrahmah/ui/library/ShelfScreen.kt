package org.darulhuda.nabiurrahmah.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.library.LanguageNames
import org.darulhuda.nabiurrahmah.data.library.ShelfId
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.BackButton
import org.darulhuda.nabiurrahmah.ui.common.MessageState
import org.darulhuda.nabiurrahmah.ui.common.shimmer
import org.darulhuda.nabiurrahmah.ui.flyers.LanguageSearchField

internal fun ShelfId.titleRes(): Int = when (this) {
    ShelfId.Biography -> R.string.tile_biography
    ShelfId.Testimonies -> R.string.tile_testimonies
    ShelfId.Books -> R.string.tile_books
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    onBack: () -> Unit,
    onOpenBook: (shelf: String, bookId: String) -> Unit,
    viewModel: ShelfViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(state.shelf.titleRes()), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.comingSoon || (!state.isLoading && !state.offline && state.totalBooks == 0) -> fullWidth("soon") {
                        MessageState(
                            icon = Icons.Outlined.HourglassEmpty,
                            title = stringResource(R.string.shelf_coming_soon_title),
                            body = stringResource(R.string.shelf_coming_soon_body),
                        )
                    }
                    state.isLoading -> items(6, key = { "placeholder-$it" }) { BookPlaceholder() }
                    state.offline -> fullWidth("offline") {
                        MessageState(
                            icon = Icons.Outlined.CloudOff,
                            title = stringResource(R.string.error_offline_title),
                            body = stringResource(R.string.shelf_offline_body),
                            action = { FilledTonalButton(onClick = viewModel::refresh) { Text(stringResource(R.string.action_retry)) } },
                        )
                    }
                    else -> {
                        fullWidth("search") {
                            Column {
                                LanguageSearchField(
                                    query = state.query,
                                    onQueryChange = viewModel::onQueryChange,
                                    hint = stringResource(R.string.search_books_hint),
                                )
                                if (state.languages.size > 1) {
                                    LanguageChips(state, viewModel::onLanguageSelected)
                                }
                            }
                        }
                        if (state.cards.isEmpty()) {
                            fullWidth("empty") {
                                MessageState(
                                    icon = Icons.Outlined.SearchOff,
                                    title = stringResource(R.string.search_no_results_title),
                                    body = stringResource(R.string.search_books_no_results),
                                )
                            }
                        }
                        items(state.cards, key = { it.book.id }) { card ->
                            BookCardItem(card, onClick = { onOpenBook(state.shelf.key, card.book.id) }, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun LanguageChips(state: ShelfUiState, onSelect: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = state.language == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.filter_all_languages)) },
            )
        }
        items(state.languages, key = { it.code }) { filter ->
            FilterChip(
                selected = state.language == filter.code,
                onClick = { onSelect(if (state.language == filter.code) null else filter.code) },
                label = { Text("${LanguageNames.native(filter.code)} · ${filter.count}") },
            )
        }
    }
}

@Composable
private fun BookCardItem(card: BookCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val edition = card.edition
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        BookCover(
            title = edition.title,
            author = edition.author,
            language = edition.language,
            seed = card.book.id,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = edition.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val languages = card.book.languages.size
        Text(
            text = if (languages > 1) {
                pluralStringResource(R.plurals.language_count, languages, languages)
            } else {
                LanguageNames.native(edition.language)
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BookPlaceholder() {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(MaterialTheme.shapes.medium)
            .shimmer(),
    )
}
