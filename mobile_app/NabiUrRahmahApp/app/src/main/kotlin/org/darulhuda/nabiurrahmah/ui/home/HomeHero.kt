package org.darulhuda.nabiurrahmah.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.ui.theme.NurColors
import org.darulhuda.nabiurrahmah.ui.theme.VerseTextStyle

/** The emblem, the name and the verse the project is named after (Al-Anbiyāʾ 21:107). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeHero(
    languageCount: Int,
    flyerCount: Int,
    videoCount: Int,
    onVideosClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(NurColors.Maroon, NurColors.MaroonDeep)))
            .drawBehind {
                // Soft golden glow behind the emblem.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NurColors.Gold.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width / 2, size.height * 0.16f),
                        radius = size.width * 0.55f,
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width / 2, size.height * 0.16f),
                )
            }
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_nabi_ur_rahmah),
            contentDescription = null,
            modifier = Modifier
                .size(104.dp)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.app_name_honorific),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.tagline).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.4.sp),
            color = NurColors.Gold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        Spacer(
            Modifier
                .width(56.dp)
                .height(2.dp)
                .background(NurColors.Gold.copy(alpha = 0.6f), CircleShape),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.verse_arabic),
            style = VerseTextStyle.copy(textDirection = TextDirection.Rtl),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.verse_translation),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.verse_reference),
            style = MaterialTheme.typography.labelMedium,
            color = NurColors.GoldSoft,
        )
        if (languageCount > 0 || videoCount > 0) {
            Spacer(Modifier.height(22.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (languageCount > 0) HeroStat(pluralStringResource(R.plurals.language_count, languageCount, languageCount))
                if (flyerCount > 0) HeroStat(pluralStringResource(R.plurals.flyer_count, flyerCount, flyerCount))
                if (videoCount > 0) {
                    HeroStat(pluralStringResource(R.plurals.video_count, videoCount, videoCount), onClick = onVideosClick)
                }
            }
        }
    }
}

@Composable
private fun HeroStat(text: String, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .background(Color.White.copy(alpha = if (onClick != null) 0.16f else 0.08f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        if (onClick != null) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = NurColors.Gold, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}
