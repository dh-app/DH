package org.darulhuda.nabiurrahmah.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.MessageState
import org.darulhuda.nabiurrahmah.ui.common.shimmer
import org.darulhuda.nabiurrahmah.ui.theme.NotoNaskhArabic
import org.darulhuda.nabiurrahmah.ui.theme.NurTheme
import org.darulhuda.nabiurrahmah.ui.videos.PlaylistCarousel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenLanguage: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenVideo: (playlistId: String, videoId: String) -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    HomeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onRefresh = viewModel::refresh,
        onOpenLanguage = onOpenLanguage,
        onOpenPlaylist = onOpenPlaylist,
        onOpenVideo = onOpenVideo,
        onOpenAbout = onOpenAbout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenLanguage: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenVideo: (playlistId: String, videoId: String) -> Unit,
    onOpenAbout: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // Index of the Videos heading: hero, search and title come first, then the language items.
    val languageItems = when {
        state.isLoading -> PLACEHOLDER_COUNT
        state.loadFailed || state.languages.isEmpty() -> 1
        else -> state.languages.size
    }
    val videosIndex = 3 + languageItems

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { HomeTopBar(scrollBehavior, onOpenAbout) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                fullWidthItem("hero") {
                    HomeHero(
                        languageCount = state.languageCount,
                        flyerCount = state.flyerCount,
                        videoCount = state.videoCount,
                        onVideosClick = { scope.launch { gridState.animateScrollToItem(videosIndex) } },
                    )
                }
                fullWidthItem("search") {
                    LanguageSearchField(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                fullWidthItem("title") {
                    Text(
                        text = stringResource(R.string.home_choose_language),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    )
                }
                when {
                    state.isLoading -> items(PLACEHOLDER_COUNT, key = { "placeholder-$it" }) { LanguageCardPlaceholder() }
                    state.loadFailed -> fullWidthItem("error") {
                        MessageState(
                            icon = Icons.Outlined.CloudOff,
                            title = stringResource(R.string.error_offline_title),
                            body = stringResource(R.string.error_offline_body),
                            action = { FilledTonalButton(onClick = onRefresh) { Text(stringResource(R.string.action_retry)) } },
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
                if (state.playlists.isNotEmpty()) {
                    fullWidthItem("videos") {
                        Text(
                            text = stringResource(R.string.videos_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 4.dp, top = 20.dp),
                        )
                    }
                    state.playlists.forEach { playlist ->
                        fullWidthItem("playlist-${playlist.id}") {
                            PlaylistCarousel(
                                playlist = playlist,
                                onSeeAll = { onOpenPlaylist(playlist.id) },
                                onOpenVideo = { video -> onOpenVideo(playlist.id, video.id) },
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
                fullWidthItem("about") {
                    AboutEntryCard(onClick = onOpenAbout, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

private const val PLACEHOLDER_COUNT = 6

private fun LazyGridScope.fullWidthItem(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(scrollBehavior: TopAppBarScrollBehavior, onOpenAbout: () -> Unit) {
    TopAppBar(
        title = {
            // The hero already shows the name; bring it into the bar once the hero scrolls away.
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.graphicsLayer { alpha = scrollBehavior.state.overlappedFraction },
            )
        },
        actions = {
            IconButton(onClick = onOpenAbout) {
                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about_title))
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
private fun LanguageSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_languages_hint)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_clear_search))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
internal fun LanguageCard(
    language: Language,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = language.flyers.size
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = language.code.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.titleLarge.let {
                    if (language.rtl) it.copy(fontFamily = NotoNaskhArabic) else it
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = language.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    count > 0 -> pluralStringResource(R.plurals.flyer_count, count, count)
                    loading -> stringResource(R.string.loading_flyers)
                    else -> stringResource(R.string.coming_soon)
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LanguageCardPlaceholder() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(MaterialTheme.shapes.large)
            .shimmer(),
    )
}

@Composable
private fun AboutEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_darul_huda),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.home_about_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    NurTheme {
        HomeContent(
            state = HomeUiState(
                isLoading = false,
                languages = listOf(
                    Language("en", "English", flyers = listOf(Flyer("1", "a.jpg"))),
                    Language("ur", "Urdu", "اردو", rtl = true),
                ),
                loadingCodes = setOf("ur"),
                languageCount = 2,
                flyerCount = 1,
            ),
            snackbarHostState = SnackbarHostState(),
            onQueryChange = {},
            onRefresh = {},
            onOpenLanguage = {},
            onOpenPlaylist = {},
            onOpenVideo = { _, _ -> },
            onOpenAbout = {},
        )
    }
}
