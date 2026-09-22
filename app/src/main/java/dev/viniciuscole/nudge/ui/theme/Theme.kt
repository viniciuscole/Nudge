package dev.viniciuscole.nudge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun NudgeTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (dark) DarkNudgeColors else LightNudgeColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.coral, onPrimary = c.card, secondary = c.teal,
            background = c.bg, onBackground = c.ink, surface = c.card, onSurface = c.ink,
            surfaceVariant = c.chip, onSurfaceVariant = c.muted, outline = c.line,
            primaryContainer = c.coralContainer, onPrimaryContainer = c.coralOnContainer,
            secondaryContainer = c.tealContainer, onSecondaryContainer = c.tealOnContainer,
        )
    } else {
        lightColorScheme(
            primary = c.coral, onPrimary = c.card, secondary = c.teal,
            background = c.bg, onBackground = c.ink, surface = c.card, onSurface = c.ink,
            surfaceVariant = c.chip, onSurfaceVariant = c.muted, outline = c.line,
            primaryContainer = c.coralContainer, onPrimaryContainer = c.coralOnContainer,
            secondaryContainer = c.tealContainer, onSecondaryContainer = c.tealOnContainer,
        )
    }
    CompositionLocalProvider(LocalNudgeColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = NudgeTypography, content = content)
    }
}

object NudgeTheme {
    val colors: NudgeColors
        @Composable @ReadOnlyComposable get() = LocalNudgeColors.current
}
