package org.darulhuda.udupi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.darulhuda.udupi.R

// ✍️ Inter — English/Latin font
val InterFamily = FontFamily(
    Font(R.font.inter_18pt_regular, FontWeight.Normal),
    Font(R.font.inter_18pt_medium, FontWeight.Medium),
    Font(R.font.inter_18pt_semibold, FontWeight.SemiBold),
    Font(R.font.inter_18pt_bold, FontWeight.Bold)
)

// 🕌 Noto Naskh Arabic — for Arabic/Urdu text
val NotoNaskhArabicFamily = FontFamily(
    Font(R.font.notonaskharabic_regular, FontWeight.Normal),
    Font(R.font.notonaskharabic_bold, FontWeight.Bold)
)

// 🧾 Typography system
val AppTypography = Typography(
    bodyLarge = TextStyle(fontFamily = InterFamily, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = InterFamily, fontSize = 12.sp),
    headlineSmall = TextStyle(fontFamily = NotoNaskhArabicFamily, fontWeight = FontWeight.Medium, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
)
