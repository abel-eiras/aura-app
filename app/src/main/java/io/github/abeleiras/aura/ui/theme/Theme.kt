package io.github.abeleiras.aura.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = AuraColors.Purple,
    onPrimary = AuraColors.White,
    secondary = AuraColors.Lime,
    onSecondary = AuraColors.Ink,
    tertiary = AuraColors.Ink,
    onTertiary = AuraColors.Paper,
    background = AuraColors.Paper,
    onBackground = AuraColors.Ink,
    surface = AuraColors.White,
    onSurface = AuraColors.Ink,
    surfaceVariant = AuraColors.PaperAlt,
    onSurfaceVariant = AuraColors.Muted,
    outline = AuraColors.Ink,
    outlineVariant = Color(0xFFBDBDBD),
    error = AuraColors.Danger,
    onError = AuraColors.White,
    primaryContainer = AuraColors.Lime,
    onPrimaryContainer = AuraColors.Ink,
    secondaryContainer = AuraColors.Lime,
    onSecondaryContainer = AuraColors.Ink,
    surfaceContainerHighest = AuraColors.PaperAlt,
)

private val DarkColors = darkColorScheme(
    primary = AuraColors.Lime,
    onPrimary = AuraColors.Ink,
    secondary = AuraColors.PurpleOnDark,
    onSecondary = AuraColors.Ink,
    tertiary = AuraColors.Paper,
    onTertiary = AuraColors.Ink,
    background = AuraColors.Ink,
    onBackground = AuraColors.Paper,
    surface = AuraColors.DarkSurface,
    onSurface = AuraColors.Paper,
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = AuraColors.DarkMuted,
    outline = AuraColors.Paper,
    outlineVariant = Color(0xFF4A4A4A),
    error = AuraColors.DangerOnDark,
    onError = AuraColors.Ink,
    primaryContainer = AuraColors.Lime,
    onPrimaryContainer = AuraColors.Ink,
    secondaryContainer = AuraColors.PurpleOnDark,
    onSecondaryContainer = AuraColors.Ink,
    surfaceContainerHighest = Color(0xFF262626),
)

/** Square corners everywhere: the brutalist look of the Formula Farma site. */
private val AuraShapes = Shapes(
    extraSmall = RectangleShape,
    small = RectangleShape,
    medium = RectangleShape,
    large = RectangleShape,
    extraLarge = RectangleShape,
)

/** Colours of things that are not Material roles: the accent used for "attention" blocks, and the shadow of raised blocks. */
data class AuraExtras(val attention: Color, val onAttention: Color, val shadow: Color, val recording: Color, val onRecording: Color)

val LocalAuraExtras = staticCompositionLocalOf {
    AuraExtras(AuraColors.Lime, AuraColors.Ink, AuraColors.Ink, AuraColors.Purple, AuraColors.White)
}

private val LightExtras = AuraExtras(AuraColors.Lime, AuraColors.Ink, AuraColors.Ink, AuraColors.Purple, AuraColors.White)
private val DarkExtras = AuraExtras(AuraColors.Lime, AuraColors.Ink, AuraColors.Lime, AuraColors.PurpleOnDark, AuraColors.Ink)

val MaterialTheme.aura: AuraExtras
    @Composable get() = LocalAuraExtras.current

/** Fixed brand palette (no dynamic colour): light by default as on the site, dark when the system asks for it. */
@Composable
fun AuraTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAuraExtras provides if (darkTheme) DarkExtras else LightExtras) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AuraTypography,
            shapes = AuraShapes,
            content = content,
        )
    }
}

val BorderWidth = 2.dp
