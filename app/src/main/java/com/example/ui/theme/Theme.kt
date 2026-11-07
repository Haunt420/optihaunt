package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme(val id: String, val title: String, val description: String, val icon: String) {
    COPPER_HUNTER_OLED(
        "copper_hunter_oled",
        "Copper & Chrome Hunter (OLED)",
        "Deep copper, bright silver chrome, dark hunter green with gold fleck on true OLED black",
        "✨"
    ),
    SPECTRAL_VIOLET(
        "spectral_violet",
        "Spectral Violet",
        "Dark obsidian with electric violet, cyan and neon magenta",
        "🌌"
    ),
    CYBER_NEON(
        "cyber_neon",
        "Cyber Neon",
        "Midnight blue with luminous cyan, electric amber and hot pink",
        "⚡"
    ),
    STUDIO_LIGHT(
        "studio_light",
        "Studio Light",
        "Crisp high-contrast day theme",
        "☀️"
    );

    companion object {
        fun fromId(id: String): AppTheme = entries.find { it.id == id } ?: COPPER_HUNTER_OLED
    }
}

// 1. Sleek Modern Dark Theme: Deep Copper, Bright Silver (Chrome), Dark Hunter Green with Gold Fleck on OLED Black
val CopperHunterOledColorScheme = darkColorScheme(
    primary = DeepCopperLight,
    onPrimary = Color(0xFF260D02),
    primaryContainer = DeepCopperDark,
    onPrimaryContainer = DeepCopperGlow,
    secondary = BrightChrome, // Bright silver chrome
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = BrightChromeHighlight,
    tertiary = GoldFleck, // Very fine gold fleck
    onTertiary = Color(0xFF2B1D00),
    tertiaryContainer = DarkHunterGreen, // Dark hunter green
    onTertiaryContainer = GoldFleckShimmer,
    background = OledBlack, // OLED Black main bg
    onBackground = BrightChrome,
    surface = OledHunterSurface,
    onSurface = BrightChrome,
    surfaceVariant = OledHunterSurfaceVariant,
    onSurfaceVariant = ChromeMuted,
    outline = DarkHunterGreenBorder,
    outlineVariant = Color(0xFF3E2D1D),
    error = OptiAntiRed
)

// 2. Spectral Violet Theme
val SpectralVioletColorScheme = darkColorScheme(
    primary = OptiPurplePrimary,
    onPrimary = Color(0xFF1E004B),
    primaryContainer = OptiPurpleSecondary,
    onPrimaryContainer = Color(0xFFF5E8FF),
    secondary = OptiCyanAccent,
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF004D61),
    onSecondaryContainer = Color(0xFFCBE6FF),
    tertiary = OptiPinkAccent,
    onTertiary = Color(0xFF490022),
    tertiaryContainer = Color(0xFF700037),
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = OptiDarkBackground,
    onBackground = OptiOnDark,
    surface = OptiDarkSurface,
    onSurface = OptiOnDark,
    surfaceVariant = OptiDarkSurfaceVariant,
    onSurfaceVariant = OptiOnDarkMuted,
    outline = Color(0xFF4A4468),
    error = OptiAntiRed
)

// 3. Cyber Neon Theme
val CyberNeonColorScheme = darkColorScheme(
    primary = Color(0xFF06B6D4), // Cyan
    onPrimary = Color(0xFF003640),
    primaryContainer = Color(0xFF004F5D),
    onPrimaryContainer = Color(0xFFB5F0FF),
    secondary = Color(0xFFF59E0B), // Neon Amber
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF633F00),
    onSecondaryContainer = Color(0xFFFFDEAB),
    tertiary = Color(0xFFEC4899), // Hot Pink
    onTertiary = Color(0xFF4A0027),
    tertiaryContainer = Color(0xFF6E003D),
    onTertiaryContainer = Color(0xFFFFD8E6),
    background = Color(0xFF080C14),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = Color(0xFFEF4444)
)

// 4. Studio Light Theme
val StudioLightColorScheme = lightColorScheme(
    primary = Color(0xFF8A3B14),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF1A402D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEECD8),
    onSecondaryContainer = Color(0xFF042113),
    tertiary = Color(0xFF8B6C00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE17C),
    onTertiaryContainer = Color(0xFF2C2000),
    background = OptiLightBackground,
    onBackground = OptiOnLight,
    surface = OptiLightSurface,
    onSurface = OptiOnLight,
    surfaceVariant = OptiLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626)
)

// Custom theme styling extras
data class OptiHauntExtraColors(
    val copperAccent: Color,
    val chromeAccent: Color,
    val hunterGreen: Color,
    val goldFleck: Color,
    val isOled: Boolean
)

val LocalExtraColors = staticCompositionLocalOf {
    OptiHauntExtraColors(
        copperAccent = DeepCopperLight,
        chromeAccent = BrightChrome,
        hunterGreen = DarkHunterGreen,
        goldFleck = GoldFleck,
        isOled = true
    )
}

@Composable
fun OptiHauntTheme(
    selectedTheme: AppTheme = AppTheme.COPPER_HUNTER_OLED, // Default to the requested sleek modern dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        AppTheme.COPPER_HUNTER_OLED -> CopperHunterOledColorScheme
        AppTheme.SPECTRAL_VIOLET -> SpectralVioletColorScheme
        AppTheme.CYBER_NEON -> CyberNeonColorScheme
        AppTheme.STUDIO_LIGHT -> StudioLightColorScheme
    }

    val extraColors = when (selectedTheme) {
        AppTheme.COPPER_HUNTER_OLED -> OptiHauntExtraColors(
            copperAccent = DeepCopper,
            chromeAccent = BrightChrome,
            hunterGreen = DarkHunterGreen,
            goldFleck = GoldFleck,
            isOled = true
        )
        AppTheme.SPECTRAL_VIOLET -> OptiHauntExtraColors(
            copperAccent = OptiPurplePrimary,
            chromeAccent = OptiCyanAccent,
            hunterGreen = Color(0xFF1B2A4A),
            goldFleck = OptiSteerAmber,
            isOled = false
        )
        AppTheme.CYBER_NEON -> OptiHauntExtraColors(
            copperAccent = Color(0xFFF59E0B),
            chromeAccent = Color(0xFF06B6D4),
            hunterGreen = Color(0xFF064E3B),
            goldFleck = Color(0xFFFDE047),
            isOled = false
        )
        AppTheme.STUDIO_LIGHT -> OptiHauntExtraColors(
            copperAccent = Color(0xFF8A3B14),
            chromeAccent = Color(0xFF475569),
            hunterGreen = Color(0xFF1A402D),
            goldFleck = Color(0xFFD97706),
            isOled = false
        )
    }

    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
