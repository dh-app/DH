package org.darulhuda.nabiurrahmah.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.library.ShelfId
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.islamicPattern
import org.darulhuda.nabiurrahmah.ui.theme.NurColors

/** Where a home tile leads. */
enum class HomeDestination { Flyers, Videos, Biography, Testimonies, Books, About }

private class Tile(
    val destination: HomeDestination,
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val colors: List<Color>,
)

private val tiles = listOf(
    Tile(HomeDestination.Flyers, Icons.Outlined.AutoStories, R.string.tile_teachings, R.string.tile_flyers_subtitle, listOf(Color(0xFF8A1116), Color(0xFF4A070B))),
    Tile(HomeDestination.Videos, Icons.Outlined.SmartDisplay, R.string.tile_teachings, R.string.tile_videos_subtitle, listOf(Color(0xFF0F6B6E), Color(0xFF063638))),
    Tile(HomeDestination.Biography, Icons.Outlined.MenuBook, R.string.tile_biography, R.string.tile_biography_subtitle, listOf(Color(0xFF1E6B45), Color(0xFF0A3320))),
    Tile(HomeDestination.Testimonies, Icons.Outlined.FormatQuote, R.string.tile_testimonies, R.string.tile_testimonies_subtitle, listOf(Color(0xFF2B4675), Color(0xFF111E3A))),
    Tile(HomeDestination.Books, Icons.Outlined.LocalLibrary, R.string.tile_books, R.string.tile_books_subtitle, listOf(Color(0xFF94621A), Color(0xFF4A2F05))),
    Tile(HomeDestination.About, Icons.Outlined.Info, R.string.tile_about, R.string.tile_about_subtitle, listOf(Color(0xFF5E2B5A), Color(0xFF2E122C))),
)

@Composable
fun HomeScreen(
    onOpen: (HomeDestination) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                HomeHero(languageCount = 0, flyerCount = 0, videoCount = 0, onVideosClick = {}, modifier = Modifier.padding(bottom = 8.dp))
            }
            itemsIndexed(tiles, key = { _, tile -> tile.destination }) { index, tile ->
                HomeTile(
                    tile = tile,
                    badge = badgeFor(tile.destination, state),
                    order = index,
                    onClick = { onOpen(tile.destination) },
                )
            }
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.home_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
            }
        }
    }
}

/** The live count each tile shows, or null while unknown. */
@Composable
private fun badgeFor(destination: HomeDestination, state: HomeUiState): String? = when (destination) {
    HomeDestination.Flyers -> state.languageCount.takeIf { it > 0 }?.let {
        pluralStringResource(R.plurals.language_count, it, it)
    }
    HomeDestination.Videos -> state.videoCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.video_count, it, it) }
    HomeDestination.Biography -> shelfBadge(state.shelves[ShelfId.Biography])
    HomeDestination.Testimonies -> shelfBadge(state.shelves[ShelfId.Testimonies])
    HomeDestination.Books -> shelfBadge(state.shelves[ShelfId.Books])
    HomeDestination.About -> null
}

@Composable
private fun shelfBadge(summary: ShelfSummary?): String? = when {
    summary == null -> null
    summary.comingSoon -> stringResource(R.string.coming_soon)
    summary.books == 0 -> null
    summary.languages > 1 -> stringResource(
        R.string.books_in_languages,
        pluralStringResource(R.plurals.book_count, summary.books, summary.books),
        pluralStringResource(R.plurals.language_count, summary.languages, summary.languages),
    )
    else -> pluralStringResource(R.plurals.book_count, summary.books, summary.books)
}

@Composable
private fun HomeTile(tile: Tile, badge: String?, order: Int, onClick: () -> Unit) {
    // Tiles rise into place one after another the first time the screen appears.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(70L * order)
        entrance.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
        modifier = Modifier
            .aspectRatio(0.8f)
            .graphicsLayer {
                alpha = entrance.value
                translationY = (1 - entrance.value) * 36.dp.toPx()
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(tile.colors))
                .islamicPattern(Color.White.copy(alpha = 0.07f), cell = 36.dp)
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tile.icon, contentDescription = null, tint = Color.White)
            }
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = stringResource(tile.title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(tile.subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = NurColors.GoldSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
