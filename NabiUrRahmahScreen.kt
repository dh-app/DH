package org.darulhuda.udupi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Language metadata. `slug` must match what your FlyerListScreen expects,
 * e.g. route nabiurrahmah/{slug}
 */
data class FlyerLanguage(
    val nativeName: String,
    val englishName: String,
    val slug: String,
    val rtl: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NabiUrRahmahScreen(
    onBack: () -> Unit,
    onOpenLanguage: (String) -> Unit
) {
    // 🔤 Full list (curate as needed; slug should match your backend/site slugs)
    val allLanguages = remember {
        listOf(
            FlyerLanguage("اردو", "Urdu", "urdu", rtl = true),
            FlyerLanguage("हिन्दी", "Hindi", "hindi"),
            FlyerLanguage("English", "English", "english"),
            FlyerLanguage("العربية", "Arabic", "arabic", rtl = true),
            FlyerLanguage("తెలుగు", "Telugu", "telugu"),
            FlyerLanguage("தமிழ்", "Tamil", "tamil"),
            FlyerLanguage("മലയാളം", "Malayalam", "malayalam"),
            FlyerLanguage("ಕನ್ನಡ", "Kannada", "kannada"),
            FlyerLanguage("বাংলা", "Bangla", "bangla"),
            FlyerLanguage("ગુજરાતી", "Gujarati", "gujarati"),
            FlyerLanguage("मराठी", "Marathi", "marathi"),
            FlyerLanguage("ਪੰਜਾਬੀ", "Punjabi", "punjabi"),
            FlyerLanguage("ଓଡ଼ିଆ", "Odia", "odia"),
        )
    }

    var query by rememberSaveable { mutableStateOf("") }
    val languages = remember(query, allLanguages) {
        if (query.isBlank()) allLanguages
        else allLanguages.filter {
            it.englishName.contains(query, ignoreCase = true) ||
                    it.nativeName.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nabi-ur-Rahmah ﷺ") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 📝 Subtitle
            Text(
                text = "Flyers Available in Following Languages",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // 🔎 Search (no experimental API)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                        }
                    }
                },
                placeholder = { Text("Search language") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // 🔲 Adaptive grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(languages) { lang ->
                    LanguageCard(lang) { onOpenLanguage(lang.slug) }
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(
    language: FlyerLanguage,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Native label (centered; RTL handled by font shaping automatically)
            Text(
                text = language.nativeName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            // English label
            Text(
                text = language.englishName,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
