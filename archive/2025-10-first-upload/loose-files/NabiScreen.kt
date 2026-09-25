package org.darulhuda.udupi.feature.nabi

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import org.darulhuda.udupi.core.download.BulkDownloadWorker
import org.darulhuda.udupi.core.model.LanguageLink

/**
 * 📜 Nabi ur Rahmah Screen
 * Displays available languages and flyers for each language.
 */
@Composable
fun NabiScreen(vm: NabiViewModel = viewModel()) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ﷺ Nabi ur Rahmah",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 🌐 Language Selector
        if (s.languages.isNotEmpty()) {
            LanguageRow(s.languages, s.selected) { vm.loadFlyers(it) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 📥 Download All Flyers Button
        if (s.selected != null) {
            Button(
                onClick = { enqueueBulkDownload(context, s.selected!!) },
                enabled = s.flyers.isNotEmpty()
            ) {
                Text("Download All (${s.selected!!.name})")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🔄 UI States
        when {
            s.loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            s.error != null -> Text(
                text = "Error: ${s.error}",
                color = MaterialTheme.colorScheme.error
            )

            else -> FlyersGrid(s)
        }
    }
}

@Composable
private fun LanguageRow(
    langs: List<LanguageLink>,
    selected: LanguageLink?,
    onSelect: (LanguageLink) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(langs) { lang ->
            FilterChip(
                selected = lang == selected,
                onClick = { onSelect(lang) },
                label = { Text(lang.name.take(22)) }
            )
        }
    }
}

@Composable
private fun FlyersGrid(s: NabiState) {
    val grid = GridCells.Adaptive(minSize = 140.dp)
    LazyVerticalGrid(
        columns = grid,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(s.flyers.size) { idx ->
            val flyer = s.flyers[idx]
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = flyer.thumbnailUrl,
                        contentDescription = flyer.title,
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(flyer.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        flyer.fileUrl,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 📦 Enqueue background flyer downloads via WorkManager.
 */
private fun enqueueBulkDownload(context: Context, lang: LanguageLink) {
    val data = Data.Builder()
        .putString("lang_name", lang.name)
        .putString("lang_code", lang.code)
        .putString("lang_url", lang.url)
        .build()

    val work = OneTimeWorkRequestBuilder<BulkDownloadWorker>()
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueue(work)
}
