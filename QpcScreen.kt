package org.darulhuda.udupi.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import org.darulhuda.udupi.model.QpcModel
import org.darulhuda.udupi.util.shareText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QpcScreen(
    qpc: QpcModel? = null,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(qpc?.title ?: "Qur'an Printing Complex") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    qpc?.let {
                        IconButton(onClick = {
                            shareText(
                                ctx,
                                it.title ?: "Qur'an Printing Complex",
                                it.share_url
                            )
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { padding ->

        if (qpc == null) {
            // Show loader until data available
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 🖼 Header Image
            qpc.hero_image?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "QPC Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            // 📝 Intro HTML (Rich Text Rendering)
            qpc.intro_html?.takeIf { it.isNotBlank() }?.let { html ->
                val richState = remember { rememberRichTextState().apply { setHtml(html) } }

                // Use RichText for rendering only (no editing)
                RichText(
                    state = richState,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 💰 Contribution Rates
            qpc.contribution_rates?.let { rate ->
                Text(
                    text = buildString {
                        append("Contribution: ₹${rate.per_sq_ft_inr ?: "-"} per sq.ft")
                        append(" | ₹${rate.per_sq_m_inr ?: "-"} per sq.m")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 🏦 Bank Details
            qpc.bank_details?.let { details ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        details.trust_name?.let {
                            Text(it, style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Bank: ${details.bank ?: "-"} • Branch: ${details.branch ?: "-"}")
                        Text("A/C: ${details.account_no ?: "-"} • IFSC: ${details.ifsc ?: "-"}")
                        details.note?.takeIf { it.isNotBlank() }?.let {
                            Text("Note: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        details.zakat?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ☎ Contact Actions
            qpc.contacts?.let { contact ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    contact.phone?.let { phone ->
                        Button(
                            onClick = {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Call") }

                        OutlinedButton(
                            onClick = {
                                val wnum = phone.replace("+", "").replace(" ", "")
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$wnum"))
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("WhatsApp") }
                    }

                    contact.website?.let { site ->
                        OutlinedButton(
                            onClick = {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(site)))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Website") }
                    }
                }
            }

            // 📄 QR / Brochure Image
            qpc.qr_brochure_image?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Brochure QR",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            // 🎥 Videos Section
            if (qpc.videos.isNotEmpty()) {
                Text("Videos", style = MaterialTheme.typography.titleMedium)
                qpc.videos.forEach { video ->
                    TextButton(
                        onClick = {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url)))
                        }
                    ) {
                        Text(video.title ?: "Watch Video")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
