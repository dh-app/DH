package org.darulhuda.nabiurrahmah.ui.flyers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.data.model.Language
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.BackButton
import org.darulhuda.nabiurrahmah.ui.common.MessageState
import org.darulhuda.nabiurrahmah.ui.common.RemoteImage
import org.darulhuda.nabiurrahmah.ui.common.shimmer
import org.darulhuda.nabiurrahmah.ui.theme.NotoNaskhArabic

private const val DEFAULT_FLYER_RATIO = 3f / 4f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyersScreen(
    onBack: () -> Unit,
    onOpenFlyer: (languageCode: String, flyerId: String) -> Unit,
    viewModel: FlyersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val language = state.language

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = language?.nativeName.orEmpty(),
                        fontFamily = if (language?.rtl == true) NotoNaskhArabic else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
            val contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            )
            when {
                state.isLoading -> PlaceholderGrid(contentPadding)
                language == null -> ScrollableMessage {
                    MessageState(
                        icon = Icons.Outlined.CloudOff,
                        title = stringResource(R.string.language_unavailable_title),
                        body = stringResource(R.string.language_unavailable_body),
                        action = {
                            FilledTonalButton(onClick = viewModel::refresh) { Text(stringResource(R.string.action_retry)) }
                        },
                    )
                }
                language.flyers.isEmpty() -> ScrollableMessage {
                    MessageState(
                        icon = Icons.Outlined.AutoStories,
                        title = stringResource(R.string.flyers_coming_soon_title),
                        body = stringResource(R.string.flyers_coming_soon_body, language.name),
                    )
                }
                else -> FlyerGrid(
                    language = language,
                    contentPadding = contentPadding,
                    onOpenFlyer = { onOpenFlyer(language.code, it.id) },
                )
            }
        }
    }
}

@Composable
private fun FlyerGrid(
    language: Language,
    contentPadding: PaddingValues,
    onOpenFlyer: (Flyer) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        contentPadding = contentPadding,
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "summary", span = StaggeredGridItemSpan.FullLine) {
            Text(
                text = stringResource(
                    R.string.flyers_summary,
                    language.name,
                    pluralStringResource(R.plurals.flyer_count, language.flyers.size, language.flyers.size),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
        }
        itemsIndexed(language.flyers, key = { _, flyer -> flyer.id }) { index, flyer ->
            FlyerCard(
                flyer = flyer,
                position = index + 1,
                rtl = language.rtl,
                onClick = { onOpenFlyer(flyer) },
            )
        }
    }
}

@Composable
private fun FlyerCard(
    flyer: Flyer,
    position: Int,
    rtl: Boolean,
    onClick: () -> Unit,
) {
    val description = flyer.title ?: stringResource(R.string.flyer_number, position)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box {
            RemoteImage(
                url = flyer.previewUrl,
                contentDescription = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(flyer.aspectRatio ?: DEFAULT_FLYER_RATIO),
            )
            if (flyer.pdf != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.badge_pdf),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        if (flyer.title != null) {
            Text(
                text = flyer.title,
                style = MaterialTheme.typography.titleSmall.let {
                    if (rtl) it.copy(fontFamily = NotoNaskhArabic) else it
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun PlaceholderGrid(contentPadding: PaddingValues) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        contentPadding = contentPadding,
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(6) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(DEFAULT_FLYER_RATIO)
                    .clip(MaterialTheme.shapes.large)
                    .shimmer(),
            )
        }
    }
}

/** Pull-to-refresh needs scrollable content, even for a single message. */
@Composable
private fun ScrollableMessage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}
