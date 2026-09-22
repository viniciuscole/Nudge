package dev.viniciuscole.nudge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.viniciuscole.nudge.R

val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
    Font(R.font.manrope, FontWeight.ExtraBold),
)

fun manrope(
    size: TextUnit,
    weight: FontWeight,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
): TextStyle = TextStyle(
    fontFamily = Manrope,
    fontSize = size,
    fontWeight = weight,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

private val base = Typography()

val NudgeTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Manrope),
    headlineSmall = base.headlineSmall.copy(fontFamily = Manrope),
    titleLarge = base.titleLarge.copy(fontFamily = Manrope),
    titleMedium = base.titleMedium.copy(fontFamily = Manrope),
    bodyLarge = base.bodyLarge.copy(fontFamily = Manrope),
    bodyMedium = base.bodyMedium.copy(fontFamily = Manrope),
    labelLarge = base.labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Manrope),
)

val CapsLabel = manrope(12.5.sp, FontWeight.SemiBold, letterSpacing = 0.6.sp)
