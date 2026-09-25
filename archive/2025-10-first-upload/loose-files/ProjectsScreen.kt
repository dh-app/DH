package org.darulhuda.udupi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.darulhuda.udupi.ui.components.AppTopBar

@Composable
fun ProjectsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { AppTopBar(title = "Our Projects", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Darul Huda Udupi leads multiple ongoing projects supporting education, humanitarian work, and publications.",
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}
