package org.darulhuda.udupi.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import org.darulhuda.udupi.Config

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToQPC: () -> Unit,
    onNavigateToNabi: () -> Unit,          // 👈 renamed: was onNavigateToFlyers
    onNavigateToLibrary: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToContact: () -> Unit
) {
    val ctx = LocalContext.current
    val scroll = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Darul Huda – Nabi-ur-Rahmah ﷺ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .verticalScroll(scroll)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 🕌 Hero / Banner (no external image dependency)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { onNavigateToQPC() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Qur’an Printing Complex",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Be a part of the Glorious Qur’an Printing Complex in Mirzapur – Hyderabad",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(onClick = onNavigateToQPC) {
                            Icon(Icons.Outlined.VolunteerActivism, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("View Details")
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 📞 Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+917022414400")))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Call Now") }

                Button(
                    onClick = {
                        val shareText = buildString {
                            appendLine("Discover the Nabi-ur-Rahmah ﷺ App – spreading the message of mercy.")
                            append("Download: ${Config.APP_SHARE_LINK}")
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        ctx.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Share App") }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Explore Sections",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // 🗂 Sections — clean, touch-friendly cards
            SectionCard(
                title = "ﷺ Nabi-ur-Rahmah",
                subtitle = "Flyers in multiple languages",
                icon = Icons.Outlined.Article,
                onClick = onNavigateToNabi
            )

            SectionCard(
                title = "📘 Digital Library",
                subtitle = "Books and resources",
                icon = Icons.Outlined.MenuBook,
                onClick = onNavigateToLibrary
            )

            SectionCard(
                title = "🏗 Projects",
                subtitle = "Current & upcoming initiatives",
                icon = Icons.Outlined.VolunteerActivism,
                onClick = onNavigateToProjects
            )

            SectionCard(
                title = "ℹ About Darul Huda",
                subtitle = "Our mission & work",
                icon = Icons.Outlined.Info,
                onClick = onNavigateToAbout
            )

            SectionCard(
                title = "☎ Contact Us",
                subtitle = "Get in touch",
                icon = Icons.Outlined.Call,
                onClick = onNavigateToContact
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "May Allah reward you for supporting beneficial knowledge.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
