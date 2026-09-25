package org.darulhuda.nabiurrahmah.ui.flyers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.BackButton
import org.darulhuda.nabiurrahmah.ui.common.MessageState

/** Tile 1: every language the flyers are published in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyerLanguagesScreen(
    onBack: () -> Unit,
    onOpenLanguage: (String) -> Unit,
    viewModel: FlyerLanguagesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.tile_teachings), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                fullWidthItem("summary") {
                    Text(
                        text = if (state.languageCount > 0) {
                            stringResource(
                                R.string.flyers_languages_summary,
                                pluralStringResource(R.plurals.language_count, state.languageCount, state.languageCount),
                                pluralStringResource(R.plurals.flyer_count, state.flyerCount, state.flyerCount),
                            )
                        } else {
                            stringResource(R.string.tile_flyers_subtitle)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                fullWidthItem("search") {
                    LanguageSearchField(query = state.query, onQueryChange = viewModel::onQueryChange, modifier = Modifier.padding(vertical = 4.dp))
                }
                when {
                    state.isLoading -> items(PLACEHOLDER_COUNT, key = { "placeholder-$it" }) { LanguageCardPlaceholder() }
                    state.loadFailed -> fullWidthItem("error") {
                        MessageState(
                            icon = Icons.Outlined.CloudOff,
                            title = stringResource(R.string.error_offline_title),
                            body = stringResource(R.string.error_offline_body),
                            action = { FilledTonalButton(onClick = viewModel::refresh) { Text(stringResource(R.string.action_retry)) } },
                        )
                    }
                    state.languages.isEmpty() -> fullWidthItem("empty") {
                        MessageState(
                            icon = Icons.Outlined.Translate,
                            title = stringResource(R.string.search_no_results_title),
                            body = stringResource(R.string.search_no_results_body, state.query.trim()),
                        )
                    }
                    else -> items(state.languages, key = { it.code }) { language ->
                        LanguageCard(
                            language = language,
                            loading = language.code in state.loadingCodes,
                            onClick = { onOpenLanguage(language.code) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}
