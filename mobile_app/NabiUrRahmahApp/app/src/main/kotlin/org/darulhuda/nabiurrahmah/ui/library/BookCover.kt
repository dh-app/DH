package org.darulhuda.nabiurrahmah.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import org.darulhuda.nabiurrahmah.data.library.LanguageNames
import org.darulhuda.nabiurrahmah.ui.common.islamicPattern
import org.darulhuda.nabiurrahmah.ui.theme.NotoNaskhArabic
import org.darulhuda.nabiurrahmah.ui.theme.NurColors

private val COVER_COLORS = listOf(
    listOf(Color(0xFF8A1116), Color(0xFF3F0609)),
    listOf(Color(0xFF1E6B45), Color(0xFF0A3320)),
    listOf(Color(0xFF2B4675), Color(0xFF101C36)),
    listOf(Color(0xFF94621A), Color(0xFF462C05)),
    listOf(Color(0xFF0F6B6E), Color(0xFF053133)),
    listOf(Color(0xFF5E2B5A), Color(0xFF2A1028)),
)

/**
 * A cover for books that don't come with one: a jewel-toned binding, a gilt frame
 * and the star lattice, with the title set in the book's own script. Colour is
 * derived from the book, so it stays the same every time.
 */
@Composable
fun BookCover(
    title: String,
    author: String?,
    language: String,
    seed: String,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 15.sp,
) {
    val colors = COVER_COLORS[seed.hashCode().absoluteValue % COVER_COLORS.size]
    val rtl = LanguageNames.isRtl(language)
    val shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp)
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .islamicPattern(NurColors.Gold.copy(alpha = 0.10f), cell = 26.dp)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, NurColors.Gold.copy(alpha = 0.55f), RoundedCornerShape(6.dp)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("۞", color = NurColors.Gold, fontSize = titleSize * 1.2f, fontFamily = NotoNaskhArabic)
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = titleSize,
                    lineHeight = titleSize * if (rtl) 1.7f else 1.3f,
                    fontFamily = if (rtl) NotoNaskhArabic else MaterialTheme.typography.titleSmall.fontFamily,
                ),
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
            if (!author.isNullOrBlank()) {
                Text(
                    text = author,
                    color = NurColors.GoldSoft,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
