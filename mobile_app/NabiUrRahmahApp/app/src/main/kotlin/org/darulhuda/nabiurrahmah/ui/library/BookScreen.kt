package org.darulhuda.nabiurrahmah.ui.library

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.library.Book
import org.darulhuda.nabiurrahmah.data.library.Edition
import org.darulhuda.nabiurrahmah.data.library.LanguageNames
import org.darulhuda.nabiurrahmah.platform.DownloadStatus
import org.darulhuda.nabiurrahmah.platform.PLAY_STORE_URL
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.BackButton
import org.darulhuda.nabiurrahmah.ui.common.SectionTitle
import org.darulhuda.nabiurrahmah.ui.theme.NotoNaskhArabic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    onBack: () -> Unit,
    onRead: (url: String, title: String) -> Unit,
    viewModel: BookViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val chooser = stringResource(R.string.share_book_chooser)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.Share -> context.shareLibraryFile(event, chooser)
                is LibraryEvent.Message -> snackbarHostState.showSnackbar(context.getString(event.text))
            }
        }
    }
    val book = state.book
    val preferred = state.preferred
    val shareText: (Edition) -> String = { edition -> context.getString(R.string.share_book_text, edition.title, PLAY_STORE_URL) }

    Scaffold(
        topBar = { TopAppBar(title = {}, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (book == null || preferred == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                BookHeader(
                    book = book,
                    edition = preferred,
                    onRead = { preferred.pdf?.let { onRead(it.url, preferred.title) } },
                    onShare = { viewModel.share(preferred, shareText(preferred), toWhatsApp = false) },
                    onWhatsApp = { viewModel.share(preferred, shareText(preferred), toWhatsApp = true) },
                )
            }
            item(key = "languages") {
                SectionTitle(
                    pluralStringResource(R.plurals.available_in_languages, book.editions.size, book.editions.size),
                    modifier = Modifier.padding(top = 28.dp),
                )
            }
            items(book.editions, key = { it.id + it.language }) { edition ->
                EditionRow(
                    edition = edition,
                    status = edition.pdf?.url?.let { state.downloads[it] } ?: DownloadStatus.NotDownloaded,
                    onRead = { edition.pdf?.let { onRead(it.url, edition.title) } },
                    onDownload = { viewModel.download(edition) },
                    onDelete = { viewModel.delete(edition) },
                    onShare = { viewModel.share(edition, shareText(edition), toWhatsApp = false) },
                )
            }
        }
    }
}

@Composable
private fun BookHeader(
    book: Book,
    edition: Edition,
    onRead: () -> Unit,
    onShare: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    val rtl = LanguageNames.isRtl(edition.language)
    Column {
        Row {
            BookCover(
                title = edition.title,
                author = edition.author,
                language = edition.language,
                seed = book.id,
                modifier = Modifier.width(128.dp),
                titleSize = 13.sp,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = edition.title,
                    style = MaterialTheme.typography.headlineSmall.let { if (rtl) it.copy(fontFamily = NotoNaskhArabic) else it },
                )
                edition.author?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(
                        text = LanguageNames.native(edition.language) + (edition.pdf?.size?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onRead, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_read))
            }
            OutlinedButton(onClick = onWhatsApp) { Text(stringResource(R.string.action_whatsapp)) }
            IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share)) }
        }
        edition.description?.let { description ->
            var expanded by rememberSaveable { mutableStateOf(false) }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.animateContentSize()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (expanded) Int.MAX_VALUE else 5,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description.length > 240) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(stringResource(if (expanded) R.string.action_show_less else R.string.action_show_more))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditionRow(
    edition: Edition,
    status: DownloadStatus,
    onRead: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onRead),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    edition.language.uppercase().take(3),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        headlineContent = {
            Text(
                text = LanguageNames.native(edition.language),
                fontFamily = if (LanguageNames.isRtl(edition.language)) NotoNaskhArabic else null,
            )
        },
        supportingContent = {
            Text(
                text = listOfNotNull(LanguageNames.english(edition.language), edition.pdf?.size).joinToString(" · "),
                maxLines = 1,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (status) {
                    is DownloadStatus.Downloading -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        val progress = status.progress
                        if (progress != null) {
                            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                    is DownloadStatus.Downloaded -> IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = stringResource(R.string.action_remove_download),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    DownloadStatus.Failed -> IconButton(onClick = onDownload) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = stringResource(R.string.action_retry))
                    }
                    DownloadStatus.NotDownloaded -> IconButton(onClick = onDownload) {
                        Icon(Icons.Outlined.Download, contentDescription = stringResource(R.string.action_download))
                    }
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.action_share))
                }
            }
        },
    )
}
