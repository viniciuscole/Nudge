package dev.viniciuscole.nudge.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class NudgeColors(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val body: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val chip: Color,
    val field: Color,
    val coral: Color,
    val coralDeep: Color,
    val coralContainer: Color,
    val coralOnContainer: Color,
    val coralMuted: Color,
    val teal: Color,
    val tealDeep: Color,
    val tealContainer: Color,
    val tealOnContainer: Color,
    val tealMuted: Color,
    val carbs: Color,
    val carbsContainer: Color,
    val carbsOnContainer: Color,
    val toggleOn: Color,
    val toggleKnobOn: Color,
    val toggleOff: Color,
    val toggleKnobOff: Color,
    val isDark: Boolean,
)

val LightNudgeColors = NudgeColors(
    bg = Color(0xFFFBF7F4),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF241E1B),
    body = Color(0xFF6B5C55),
    muted = Color(0xFF8A7A73),
    faint = Color(0xFFA2938B),
    line = Color(0xFFE4DAD3),
    chip = Color(0xFFF1E8E1),
    field = Color(0xFFF7F0EA),
    coral = Color(0xFFE9704B),
    coralDeep = Color(0xFFC1543A),
    coralContainer = Color(0xFFFDE9E2),
    coralOnContainer = Color(0xFF8E3A26),
    coralMuted = Color(0xFFB4705C),
    teal = Color(0xFF2E8F92),
    tealDeep = Color(0xFF24736F),
    tealContainer = Color(0xFFDDF0EF),
    tealOnContainer = Color(0xFF1C5D5E),
    tealMuted = Color(0xFF4E8A89),
    carbs = Color(0xFFF0B27A),
    carbsContainer = Color(0xFFFBEEDD),
    carbsOnContainer = Color(0xFFA9702F),
    toggleOn = Color(0xFF2E8F92),
    toggleKnobOn = Color(0xFFFFFFFF),
    toggleOff = Color(0xFFE4DAD3),
    toggleKnobOff = Color(0xFFB3A49C),
    isDark = false,
)

val DarkNudgeColors = NudgeColors(
    bg = Color(0xFF151210),
    card = Color(0xFF211C19),
    ink = Color(0xFFF4EDE8),
    body = Color(0xFFC9BDB6),
    muted = Color(0xFF9A8B84),
    faint = Color(0xFF7E716A),
    line = Color(0xFF2E2723),
    chip = Color(0xFF2A2320),
    field = Color(0xFF2A2320),
    coral = Color(0xFFE9704B),
    coralDeep = Color(0xFFF0A88F),
    coralContainer = Color(0xFF33170F),
    coralOnContainer = Color(0xFFFBE4DA),
    coralMuted = Color(0xFFC08D7C),
    teal = Color(0xFF3ABDB8),
    tealDeep = Color(0xFF7FC6C4),
    tealContainer = Color(0xFF12302F),
    tealOnContainer = Color(0xFFD9F2F0),
    tealMuted = Color(0xFF8FB8B7),
    carbs = Color(0xFFF0B27A),
    carbsContainer = Color(0xFF3A2A16),
    carbsOnContainer = Color(0xFFF0C48F),
    toggleOn = Color(0xFF3ABDB8),
    toggleKnobOn = Color(0xFF062E2D),
    toggleOff = Color(0xFF2E2723),
    toggleKnobOff = Color(0xFF6E625C),
    isDark = true,
)

object RingColors {
    val mealBg = Color(0xFF2A140E)
    val mealIcon = Color(0xFFFFD2C2)
    val mealBars = Color(0xFFFFAA8C)
    val waterBg = Color(0xFF1C5D5E)
    val coral = Color(0xFFE9704B)
}

val LocalNudgeColors = staticCompositionLocalOf { LightNudgeColors }
