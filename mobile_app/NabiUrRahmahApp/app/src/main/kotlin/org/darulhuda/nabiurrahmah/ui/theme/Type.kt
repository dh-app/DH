package org.darulhuda.nabiurrahmah.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.darulhuda.nabiurrahmah.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** For Arabic-script text (Arabic, Urdu) and Qur'anic quotations. */
val NotoNaskhArabic = FontFamily(
    Font(R.font.noto_naskh_arabic_regular, FontWeight.Normal),
    Font(R.font.noto_naskh_arabic_bold, FontWeight.Bold),
)

private val base = Typography()

private fun TextStyle.inter(weight: FontWeight? = fontWeight) = copy(fontFamily = Inter, fontWeight = weight)

internal val NurTypography = Typography(
    displayLarge = base.displayLarge.inter(),
    displayMedium = base.displayMedium.inter(),
    displaySmall = base.displaySmall.inter(),
    headlineLarge = base.headlineLarge.inter(FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.inter(FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.inter(FontWeight.SemiBold),
    titleLarge = base.titleLarge.inter(FontWeight.SemiBold),
    titleMedium = base.titleMedium.inter(FontWeight.SemiBold),
    titleSmall = base.titleSmall.inter(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.inter(),
    bodyMedium = base.bodyMedium.inter(),
    bodySmall = base.bodySmall.inter(),
    labelLarge = base.labelLarge.inter(FontWeight.SemiBold),
    labelMedium = base.labelMedium.inter(FontWeight.SemiBold),
    labelSmall = base.labelSmall.inter(FontWeight.Medium),
)

/** Style for a Qur'anic verse in the Arabic script. */
val VerseTextStyle = TextStyle(
    fontFamily = NotoNaskhArabic,
    fontWeight = FontWeight.Normal,
    fontSize = 24.sp,
    lineHeight = 44.sp,
)
