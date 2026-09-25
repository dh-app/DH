package org.darulhuda.udupi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.darulhuda.udupi.util.downloadFile
import org.darulhuda.udupi.util.shareText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyerListScreen(
    languageCode: String,
    onBack: () -> Unit
) {
    // 🔁 Replace these with your real feed once ready (Gist/API).
    val flyers = listOf(
        "https://darulhudaudupi.org/wp-content/uploads/flyers/$languageCode/flyer1.jpg",
        "https://darulhudaudupi.org/wp-content/uploads/flyers/$languageCode/flyer2.jpg",
        "https://darulhudaudupi.org/wp-content/uploads/flyers/$languageCode/flyer3.jpg"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nabi-ur-Rahmah ﷺ — ${languageCode.replaceFirstChar { it.uppercase() }}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(flyers) { url ->
                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ElevatedButton(onClick = {
                                shareText(
                                    "🌙 Nabi-ur-Rahmah ﷺ Flyer ($languageCode)\n$url\n\nDownload our app:\nhttps://play.google.com/store/apps/details?id=org.darulhuda.udupi"
                                )
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                            OutlinedButton(onClick = {
                                val safeName = "nabiurrahmah_${languageCode}_${url.hashCode()}.jpg"
                                downloadFile(url = url, suggestedName = safeName)
                            }) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Download")
                            }
                        }
                    }
                }
            }
        }
    }
}
