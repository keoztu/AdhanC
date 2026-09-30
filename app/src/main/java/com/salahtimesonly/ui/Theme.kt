package com.salahtimesonly.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.salahtimesonly.R

/** Direction C, "Evening": deep green and brass, with a light counterpart. */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val soft: Color,
    val isDark: Boolean,
)

val EveningDark = Palette(
    bg = Color(0xFF0E2621), surface = Color(0xFF143129), text = Color(0xFFF1EBDD), muted = Color(0xFFA9B5A8),
    faint = Color(0xFF6F8279), line = Color(0xFF264539), accent = Color(0xFFD2B06B), onAccent = Color(0xFF0E2621),
    soft = Color(0xFF1A3A31), isDark = true,
)
val EveningLight = Palette(
    bg = Color(0xFFF2F0EA), surface = Color(0xFFFAF9F5), text = Color(0xFF10251F), muted = Color(0xFF5C6861),
    faint = Color(0xFF98A29B), line = Color(0xFFDAD5C8), accent = Color(0xFF86652A), onAccent = Color(0xFFFAF9F5),
    soft = Color(0xFFEDE5D3), isDark = false,
)

val LocalPalette = staticCompositionLocalOf { EveningDark }

val Readex = FontFamily(
    Font(R.font.readex_pro, FontWeight.Light),
    Font(R.font.readex_pro, FontWeight.Normal),
    Font(R.font.readex_pro, FontWeight.Medium),
    Font(R.font.readex_pro, FontWeight.SemiBold),
)

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

@Composable
fun SalahTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) EveningDark else EveningLight
    val scheme = if (dark) darkColorScheme(
        primary = p.accent, onPrimary = p.onAccent, background = p.bg, onBackground = p.text,
        surface = p.surface, onSurface = p.text, surfaceVariant = p.surface, onSurfaceVariant = p.muted,
        outline = p.line, outlineVariant = p.line, surfaceContainerHigh = p.surface,
    ) else lightColorScheme(
        primary = p.accent, onPrimary = p.onAccent, background = p.bg, onBackground = p.text,
        surface = p.surface, onSurface = p.text, surfaceVariant = p.surface, onSurfaceVariant = p.muted,
        outline = p.line, outlineVariant = p.line, surfaceContainerHigh = p.surface,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = Typography().withFont(Readex), content = content)
    }
}
