package org.darulhuda.udupi.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.darulhuda.udupi.ui.components.AppTopBar

@Composable
fun ContactScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = { AppTopBar(title = "Contact Darul Huda Udupi", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("We’d love to hear from you. Reach out to us:", style = MaterialTheme.typography.bodyLarge)

            Button(onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+917022414400")))
            }) { Text("📞 Call Us") }

            OutlinedButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/917022414400")))
            }) { Text("💬 WhatsApp") }

            OutlinedButton(onClick = {
                val email = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:info@darulhudaudupi.org")
                    putExtra(Intent.EXTRA_SUBJECT, "Inquiry via Nabi-ur-Rahmah App")
                }
                context.startActivity(email)
            }) { Text("✉️ Email Us") }

            Divider(Modifier.padding(vertical = 12.dp))

            Text(
                "Darul Huda Udupi\nMirzapur, Udupi, Karnataka, India\nWebsite: darulhudaudupi.org",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
