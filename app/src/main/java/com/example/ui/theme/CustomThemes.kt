package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class SoundSchedulerTheme(
    val displayName: String,
    val iconEmoji: String,
    val isDark: Boolean,
    val primary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color
) {
    OCEAN_BLUE(
        displayName = "Ocean Blue",
        iconEmoji = "🌊",
        isDark = true,
        primary = Color(0xFF4FC3F7),
        background = Color(0xFF0A1F44),
        surface = Color(0xFF132D5A),
        onPrimary = Color(0xFF0A1F44),
        onBackground = Color(0xFFE1F5FE),
        onSurface = Color(0xFFE1F5FE)
    ),
    FOREST_GREEN(
        displayName = "Forest Green",
        iconEmoji = "🌿",
        isDark = true,
        primary = Color(0xFFA8E6CF),
        background = Color(0xFF1B3A2D),
        surface = Color(0xFF264E3E),
        onPrimary = Color(0xFF1B3A2D),
        onBackground = Color(0xFFE8F5E9),
        onSurface = Color(0xFFE8F5E9)
    ),
    SOFT_ROSE(
        displayName = "Soft Rose",
        iconEmoji = "🌸",
        isDark = false,
        primary = Color(0xFFFF6B8A),
        background = Color(0xFFFFF0F3),
        surface = Color(0xFFFFE3E7),
        onPrimary = Color.White,
        onBackground = Color(0xFF4A1521),
        onSurface = Color(0xFF4A1521)
    ),
    MIDNIGHT_DARK(
        displayName = "Midnight Dark",
        iconEmoji = "🌙",
        isDark = true,
        primary = Color(0xFFBB86FC),
        background = Color(0xFF0D0D0D),
        surface = Color(0xFF1A1A1A),
        onPrimary = Color.Black,
        onBackground = Color(0xFFECE6F0),
        onSurface = Color(0xFFECE6F0)
    ),
    SUNRISE_ORANGE(
        displayName = "Sunrise Orange",
        iconEmoji = "☀️",
        isDark = false,
        primary = Color(0xFFFF7043),
        background = Color(0xFFFFF8F0),
        surface = Color(0xFFFFF1E0),
        onPrimary = Color.White,
        onBackground = Color(0xFF4E1B09),
        onSurface = Color(0xFF4E1B09)
    );

    fun toColorScheme(): ColorScheme {
        return if (isDark) {
            darkColorScheme(
                primary = primary,
                background = background,
                surface = surface,
                onPrimary = onPrimary,
                onBackground = onBackground,
                onSurface = onSurface,
                surfaceVariant = surface,
                onSurfaceVariant = onSurface
            )
        } else {
            lightColorScheme(
                primary = primary,
                background = background,
                surface = surface,
                onPrimary = onPrimary,
                onBackground = onBackground,
                onSurface = onSurface,
                surfaceVariant = surface,
                onSurfaceVariant = onSurface
            )
        }
    }
}
