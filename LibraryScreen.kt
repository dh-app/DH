package org.darulhuda.udupi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.darulhuda.udupi.ui.components.AppTopBar

@Composable
fun LibraryScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppTopBar(title = "Digital Library", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Explore our growing collection of digital Islamic books and materials.",
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}
