package org.darulhuda.udupi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.darulhuda.udupi.ui.components.AppTopBar

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppTopBar(title = "About Darul Huda Udupi", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Darul Huda Udupi is a center of Islamic learning dedicated to authentic knowledge and community service.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "The Nabi-ur-Rahmah App connects users to Islamic resources, flyers, and projects initiated by Darul Huda.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
