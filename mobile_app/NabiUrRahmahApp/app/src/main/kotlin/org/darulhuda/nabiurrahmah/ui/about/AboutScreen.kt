package org.darulhuda.nabiurrahmah.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.darulhuda.nabiurrahmah.BuildConfig
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.data.model.About
import org.darulhuda.nabiurrahmah.platform.PLAY_STORE_URL
import org.darulhuda.nabiurrahmah.platform.dial
import org.darulhuda.nabiurrahmah.platform.openMap
import org.darulhuda.nabiurrahmah.platform.openPlayStore
import org.darulhuda.nabiurrahmah.platform.openUrl
import org.darulhuda.nabiurrahmah.platform.openWhatsApp
import org.darulhuda.nabiurrahmah.platform.sendEmail
import org.darulhuda.nabiurrahmah.platform.shareText
import org.darulhuda.nabiurrahmah.ui.AppViewModelProvider
import org.darulhuda.nabiurrahmah.ui.common.BackButton
import org.darulhuda.nabiurrahmah.ui.common.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val about by viewModel.about.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val emailSubject = stringResource(R.string.about_email_subject)
    val shareAppText = stringResource(R.string.share_app_text, PLAY_STORE_URL)
    val shareAppChooser = stringResource(R.string.share_app_chooser)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = { BackButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "app") { AppCard() }

            val info = about
            if (info != null) {
                item(key = "organization") { OrganizationCard(info) }

                if (info.hasContactDetails) {
                    item(key = "contact") {
                        Section(stringResource(R.string.about_contact)) {
                            info.phones.forEach { phone ->
                                ActionRow(Icons.Outlined.Call, phone, stringResource(R.string.about_call)) {
                                    context.dial(phone)
                                }
                            }
                            info.whatsapp?.let { number ->
                                ActionRow(Icons.Outlined.Sms, stringResource(R.string.about_whatsapp), number) {
                                    context.openWhatsApp(number)
                                }
                            }
                            info.email?.let { email ->
                                ActionRow(Icons.Outlined.Email, email, stringResource(R.string.about_email)) {
                                    context.sendEmail(email, emailSubject)
                                }
                            }
                            info.website?.let { site ->
                                ActionRow(Icons.Outlined.Language, site.displayUrl(), stringResource(R.string.about_website)) {
                                    context.openUrl(site)
                                }
                            }
                            info.address?.let { address ->
                                ActionRow(Icons.Outlined.Place, address, stringResource(R.string.about_directions)) {
                                    context.openMap(address, info.mapUrl)
                                }
                            }
                        }
                    }
                }

                if (info.socials.isNotEmpty()) {
                    item(key = "social") {
                        Section(stringResource(R.string.about_follow)) {
                            info.socials.forEach { social ->
                                ActionRow(Icons.Outlined.Public, social.name, null) { context.openUrl(social.url) }
                            }
                        }
                    }
                }
            }

            item(key = "spread") {
                Section(stringResource(R.string.about_spread)) {
                    ActionRow(Icons.Outlined.Share, stringResource(R.string.about_share_app), null) {
                        context.shareText(shareAppText, shareAppChooser)
                    }
                    ActionRow(Icons.Outlined.Star, stringResource(R.string.about_rate_app), null) {
                        context.openPlayStore()
                    }
                }
            }

            item(key = "footer") {
                Text(
                    text = stringResource(
                        R.string.about_footer,
                        about?.organization?.takeIf { it.isNotBlank() } ?: stringResource(R.string.organization_name),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val About.hasContactDetails: Boolean
    get() = phones.isNotEmpty() || whatsapp != null || email != null || website != null || address != null

private fun String.displayUrl(): String = removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')

@Composable
private fun AppCard() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_nabi_ur_rahmah),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.app_name_honorific), style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.about_app_body), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun OrganizationCard(about: About) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_darul_huda),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = about.organization.ifBlank { stringResource(R.string.organization_name) },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (about.summary.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(about.summary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        SectionTitle(title)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else {
            null
        },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
