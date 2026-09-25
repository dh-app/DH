package org.darulhuda.udupi.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * 🔝 Universal top bar used by all screens.
 * Automatically handles back navigation and optional actions (like Share).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            onBack?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            onShare?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
        }
    )
}
