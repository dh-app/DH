package org.darulhuda.nabiurrahmah.ui.viewer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.model.Flyer
import org.darulhuda.nabiurrahmah.platform.PLAY_STORE_URL
import org.darulhuda.nabiurrahmah.platform.shareFile
import org.darulhuda.nabiurrahmah.platform.shareFileToWhatsApp
import coil.request.ImageRequest
import androidx.compose.material.icons.outlined.Forum
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.ImmersiveSystemBars
import org.darulhuda.nabiurrahmah.ui.common.MessageState
import org.darulhuda.nabiurrahmah.ui.theme.NurTheme

@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    onOpenPdf: (url: String, title: String) -> Unit,
    viewModel: ViewerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareTitle = stringResource(R.string.share_flyer_chooser)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ViewerEvent.Share -> {
                    val text = listOfNotNull(event.flyer.title, context.getString(R.string.share_flyer_text, PLAY_STORE_URL))
                        .joinToString("\n\n")
                    if (event.toWhatsApp) {
                        context.shareFileToWhatsApp(event.file, text, shareTitle)
                    } else {
                        context.shareFile(event.file, text, shareTitle)
                    }
                }
                is ViewerEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.text))
            }
        }
    }

    // Android 9 and older ask for storage access before saving.
    var pendingSaveId by rememberSaveable { mutableStateOf<String?>(null) }
    val permissionDenied = stringResource(R.string.message_permission_needed)
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val flyer = state.language?.flyers?.firstOrNull { it.id == pendingSaveId }
        pendingSaveId = null
        when {
            granted && flyer != null -> viewModel.save(flyer)
            !granted -> scope.launch { snackbarHostState.showSnackbar(permissionDenied) }
        }
    }
    val onSave: (Flyer) -> Unit = { flyer ->
        if (viewModel.needsStoragePermission) {
            pendingSaveId = flyer.id
            requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.save(flyer)
        }
    }

    ViewerContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onShare = { viewModel.share(it) },
        onWhatsApp = { viewModel.share(it, toWhatsApp = true) },
        onSave = onSave,
        onOpenPdf = { flyer -> flyer.pdf?.let { onOpenPdf(it, flyer.title ?: context.getString(R.string.app_name)) } },
    )
}

@Composable
private fun ViewerContent(
    state: ViewerUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onShare: (Flyer) -> Unit,
    onWhatsApp: (Flyer) -> Unit,
    onSave: (Flyer) -> Unit,
    onOpenPdf: (Flyer) -> Unit,
) {
    val flyers = state.language?.flyers.orEmpty()
    var chromeVisible by rememberSaveable { mutableStateOf(true) }
    ImmersiveSystemBars(barsVisible = chromeVisible)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (flyers.isEmpty()) {
            EmptyViewer(isLoading = state.isLoading, onBack = onBack)
        } else {
            FlyerPager(
                flyers = flyers,
                initialPage = state.initialPage,
                busy = state.busy,
                chromeVisible = chromeVisible,
                onToggleChrome = { chromeVisible = !chromeVisible },
                onBack = onBack,
                onShare = onShare,
                onWhatsApp = onWhatsApp,
                onSave = onSave,
                onOpenPdf = onOpenPdf,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 112.dp),
        )
    }
}

@Composable
private fun EmptyViewer(isLoading: Boolean, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                // The viewer is always dark, whatever the system theme.
                NurTheme(darkTheme = true) {
                    Surface(color = Color.Black, contentColor = Color.White) {
                        MessageState(
                            icon = Icons.Outlined.CloudOff,
                            title = stringResource(R.string.language_unavailable_title),
                            body = stringResource(R.string.flyer_unavailable_body),
                        )
                    }
                }
            }
        }
        ViewerTopBar(position = null, title = null, onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun FlyerPager(
    flyers: List<Flyer>,
    initialPage: Int,
    busy: ViewerAction?,
    chromeVisible: Boolean,
    onToggleChrome: () -> Unit,
    onBack: () -> Unit,
    onShare: (Flyer) -> Unit,
    onWhatsApp: (Flyer) -> Unit,
    onSave: (Flyer) -> Unit,
    onOpenPdf: (Flyer) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { flyers.size }
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            key = { flyers[it].id },
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val flyer = flyers[page]
            val imageState = rememberZoomableImageState()
            val context = LocalContext.current
            // If the full-size file isn't on the site, fall back to the best preview we have.
            var useFallback by remember(flyer.id) { mutableStateOf(false) }
            val request = remember(flyer.id, useFallback) {
                ImageRequest.Builder(context)
                    .data(if (useFallback) flyer.previewUrl else flyer.image)
                    .listener(onError = { _, _ -> if (!useFallback && flyer.thumbnail != null) useFallback = true })
                    .build()
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ZoomableAsyncImage(
                    model = request,
                    contentDescription = flyer.title ?: stringResource(R.string.flyer_number, page + 1),
                    gestures = EnabledZoomGestures.ZoomAndPan,
                    state = imageState,
                    onClick = { onToggleChrome() },
                    modifier = Modifier.fillMaxSize(),
                )
                if (!imageState.isImageDisplayed) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        val current = flyers.getOrNull(pagerState.currentPage)
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ViewerTopBar(
                position = stringResource(R.string.viewer_position, pagerState.currentPage + 1, flyers.size),
                title = current?.title,
                onBack = onBack,
            )
        }
        if (current != null) {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                ViewerActions(
                    flyer = current,
                    busy = busy,
                    onShare = { onShare(current) },
                    onWhatsApp = { onWhatsApp(current) },
                    onSave = { onSave(current) },
                    onOpenPdf = { onOpenPdf(current) },
                )
            }
        }
    }
}

@Composable
private fun ViewerTopBar(
    position: String?,
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
            .statusBarsPadding()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = Color.White,
            )
        }
        Column(Modifier.weight(1f)) {
            if (position != null) {
                Text(position, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ViewerActions(
    flyer: Flyer,
    busy: ViewerAction?,
    onShare: () -> Unit,
    onWhatsApp: () -> Unit,
    onSave: () -> Unit,
    onOpenPdf: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            .navigationBarsPadding()
            .padding(top = 32.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ActionButton(Icons.Outlined.Forum, stringResource(R.string.action_whatsapp), busy == ViewerAction.WhatsApp, busy == null, onWhatsApp)
        ActionButton(Icons.Outlined.Share, stringResource(R.string.action_share), busy == ViewerAction.Share, busy == null, onShare)
        ActionButton(Icons.Outlined.Download, stringResource(R.string.action_save), busy == ViewerAction.Save, busy == null, onSave)
        if (flyer.pdf != null) {
            ActionButton(
                Icons.Outlined.PictureAsPdf,
                stringResource(R.string.action_read_pdf),
                false,
                true,
                onOpenPdf,
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color.White.copy(alpha = 0.14f),
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.08f),
                disabledContentColor = Color.White.copy(alpha = 0.5f),
            ),
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = label)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}
