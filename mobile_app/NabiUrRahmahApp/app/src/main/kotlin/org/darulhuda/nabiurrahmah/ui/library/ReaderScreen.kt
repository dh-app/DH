package org.darulhuda.nabiurrahmah.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.platform.PLAY_STORE_URL
import org.darulhuda.nabiurrahmah.platform.PdfPage
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.BackButton
import org.darulhuda.nabiurrahmah.ui.common.MessageState
import org.darulhuda.nabiurrahmah.ui.common.shimmer

/** Pages drawn light-on-dark for night reading. */
private val NightFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -0.9f, 0f, 0f, 0f, 235f,
            0f, -0.9f, 0f, 0f, 230f,
            0f, 0f, -0.9f, 0f, 220f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/**
 * The book reader: continuous vertical pages rendered sharp at screen width,
 * opening where the reader left off, with night mode, go-to-page, and a tap
 * on any page to study it zoomed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val chooser = stringResource(R.string.share_book_chooser)
    val shareText = stringResource(R.string.share_book_text, viewModel.title, PLAY_STORE_URL)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.Share -> context.shareLibraryFile(event, chooser)
                is LibraryEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.text))
            }
        }
    }
    var night by rememberSaveable { mutableStateOf(false) }
    var zoomedPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var askPage by remember { mutableStateOf(false) }
    val ready = state as? ReaderUiState.Ready
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = ready?.initialPage ?: 0)
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(viewModel.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                            if (ready != null) {
                                Text(
                                    text = stringResource(R.string.reader_page_of, listState.firstVisibleItemIndex + 1, ready.pages.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { askPage = true },
                                )
                            }
                        }
                    },
                    navigationIcon = { BackButton(onBack) },
                    actions = {
                        if (ready != null) {
                            IconButton(onClick = { night = !night }) {
                                Icon(
                                    if (night) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                    contentDescription = stringResource(R.string.action_night_mode),
                                )
                            }
                            IconButton(onClick = viewModel::saveToDevice) {
                                Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.action_save))
                            }
                            IconButton(onClick = { viewModel.share(shareText, toWhatsApp = false) }) {
                                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share))
                            }
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = if (night) Color(0xFF151112) else MaterialTheme.colorScheme.surfaceContainer,
        ) { padding ->
            when (val current = state) {
                is ReaderUiState.Downloading -> DownloadProgress(current.progress, Modifier.padding(padding))
                ReaderUiState.Failed -> MessageState(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.reader_failed_title),
                    body = stringResource(R.string.message_download_failed),
                    modifier = Modifier.padding(padding),
                    action = { FilledTonalButton(onClick = viewModel::open) { Text(stringResource(R.string.action_retry)) } },
                )
                is ReaderUiState.Ready -> {
                    LaunchedEffect(current.path) {
                        if (listState.firstVisibleItemIndex == 0 && current.initialPage > 0) listState.scrollToItem(current.initialPage)
                        snapshotFlow { listState.firstVisibleItemIndex }.collectLatest { page ->
                            delay(400)
                            viewModel.onPageShown(page)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = padding.calculateBottomPadding() + 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(current.pages, key = { index, _ -> index }) { index, ratio ->
                            PageImage(
                                page = PdfPage(current.path, index),
                                ratio = ratio,
                                night = night,
                                description = stringResource(R.string.reader_page, index + 1),
                                onClick = { zoomedPage = index },
                            )
                        }
                    }
                }
            }
        }

        val zoomed = zoomedPage
        if (ready != null && zoomed != null) {
            ZoomedPage(PdfPage(ready.path, zoomed), night, onClose = { zoomedPage = null }, modifier = Modifier.zIndex(1f))
        }
    }

    if (askPage && ready != null) {
        GoToPageDialog(
            current = listState.firstVisibleItemIndex,
            pages = ready.pages.size,
            onDismiss = { askPage = false },
            onGo = { page ->
                askPage = false
                scope.launch { listState.scrollToItem(page) }
            },
        )
    }
}

@Composable
private fun PageImage(page: PdfPage, ratio: Float, night: Boolean, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio.coerceIn(0.3f, 3f))
            .background(if (night) Color(0xFF231C1D) else Color.White)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = page,
            contentDescription = description,
            colorFilter = if (night) NightFilter else null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ZoomedPage(page: PdfPage, night: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BackHandler(onBack = onClose)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        ZoomableAsyncImage(
            // Rendered large, so text stays sharp when zoomed in.
            model = remember(page) { ImageRequest.Builder(context).data(page).size(ZOOM_RENDER_WIDTH).build() },
            contentDescription = null,
            gestures = EnabledZoomGestures.ZoomAndPan,
            colorFilter = if (night) NightFilter else null,
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .statusBarsPadding()
                .padding(4.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = Color.White)
        }
    }
}

private const val ZOOM_RENDER_WIDTH = 2800

@Composable
private fun DownloadProgress(progress: Float?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.reader_preparing), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))
        if (progress != null) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.reader_offline_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoToPageDialog(current: Int, pages: Int, onDismiss: () -> Unit, onGo: (Int) -> Unit) {
    var value by remember { mutableFloatStateOf(current.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_go_to_page)) },
        text = {
            Column {
                Text(stringResource(R.string.reader_page_of, value.roundToInt() + 1, pages), style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..(pages - 1).coerceAtLeast(1).toFloat(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onGo(value.roundToInt()) }) { Text(stringResource(R.string.action_go)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
