package org.darulhuda.udupi.feature.quran

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.darulhuda.udupi.R

@Composable
fun QuranComplexScreen() {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header image (local drawable)
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            // This will crash if the drawable is missing, so be sure the file exists
            Image(
                painter = painterResource(id = R.drawable.quran_complex_details),
                contentDescription = "Qur'an Printing Complex",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
            )
        }

        Text(
            "Be a Part of a Lasting Legacy!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Support the construction of the Glorious Qur’an Printing Complex in Mirzapur, " +
                    "Vikarabad (Hyderabad) and earn everlasting Sadaqah Jariyah rewards.\n\n" +
                    "With a contribution of ₹2,750 per sq. ft. or ₹29,600 per sq. meter, " +
                    "you can help spread the divine message for generations to come!",
            style = MaterialTheme.typography.bodyLarge
        )

        // Badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill("80G tax-exempt")
            Pill("CSR eligible")
        }

        // Primary actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { openUrl(context, "https://darulhudaudupi.org/") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Link, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Donate / Learn More")
            }
            OutlinedButton(
                onClick = { dial(context, "+917022414400") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Call")
            }
        }

        Divider()

        // Bank details
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "DAR UL HUDA CHARITABLE TRUST",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text("Bank: Canara Bank")
                Text("Branch: Malpe")
                Text("Account No.: 110029470220")
                Text("IFSC Code: CNRB0010133")
                Text(
                    "Note: Only domestic contributions accepted. International payments not allowed.\n" +
                            "(Zakat donations not accepted.)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Contact
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Contact Us", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { dial(context, "+917022414400") }) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("+91 70224 14400")
                    }
                    OutlinedButton(onClick = { email(context, "info@darulhudaudupi.org") }) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Email")
                    }
                }
            }
        }

        // Videos
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Watch", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = {
                    openUrl(context, "https://youtu.be/Uu5m6oPmUBA?si=75viE6-PfEjJgvfk")
                }) {
                    Text("Darul Huda Introduction")
                }
                OutlinedButton(onClick = {
                    openUrl(context, "https://youtu.be/fvsnpHJy6CA")
                }) {
                    Text("Project Glimpse")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "“Donate today and multiply your blessings.”",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Pill(text: String) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // no-op
    }
}

private fun dial(context: android.content.Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) { }
}

private fun email(context: android.content.Context, to: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) { }
}
