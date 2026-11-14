package dev.feeddo.sdk.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class FeeddoColors(
    val background: Color = Color(0xFF000000),
    val backgroundGradient: List<Color>? = null,
    val backgroundGradientStops: List<Float>? = null,
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textSecondary: Color = Color(0xFFB3B3B3),
    val cardBackground: Color = Color(0xFFFFFFFF),
    val cardText: Color = Color(0xFF000000),
    val iconColor: Color = Color(0xFF000000),
    val closeButtonColor: Color = Color(0xFFFFFFFF),
    val primary: Color = Color(0xFF2196F3),
    val surface: Color = Color(0xFFFFFFFF),
    val success: Color = Color(0xFF4CAF50),
    val error: Color = Color(0xFFF44336),
    val border: Color = Color(0xFFE0E0E0),
    val divider: Color = Color(0xFFBDBDBD),
    val appBarBackground: Color = Color(0xFF000000)
)

@Immutable
data class FeeddoTheme(
    val colors: FeeddoColors,
    val isDark: Boolean = true
) {
    companion object {
        fun dark(): FeeddoTheme = FeeddoTheme(
            colors = FeeddoColors(
                background = Color(0xFF09090B),
                backgroundGradient = null,
                backgroundGradientStops = null,
                textPrimary = Color(0xFFFAFAFA),
                textSecondary = Color(0xFFA1A1AA),
                cardBackground = Color(0xFF18181B),
                cardText = Color(0xFFFAFAFA),
                iconColor = Color(0xFFA1A1AA),
                closeButtonColor = Color(0xFFFAFAFA),
                primary = Color(0xFFFFFFFF),
                surface = Color(0xFF18181B),
                success = Color(0xFF22C55E),
                error = Color(0xFFEF4444),
                border = Color(0xFF27272A),
                divider = Color(0xFF1A1A1A),
                appBarBackground = Color(0xFF000000)
            ),
            isDark = true
        )

        fun light(): FeeddoTheme = FeeddoTheme(
            colors = FeeddoColors(
                background = Color(0xFFFBFBFB),
                backgroundGradient = null,
                backgroundGradientStops = null,
                textPrimary = Color(0xFF111827),
                textSecondary = Color(0xFF4B5563),
                cardBackground = Color(0xFFFFFFFF),
                cardText = Color(0xFF111827),
                iconColor = Color(0xFF4B5563),
                closeButtonColor = Color(0xFF111827),
                primary = Color(0xFF000000),
                surface = Color(0xFFFFFFFF),
                success = Color(0xFF10B981),
                error = Color(0xFFEF4444),
                border = Color(0xFFE5E7EB),
                divider = Color(0xFFE5E7EB),
                appBarBackground = Color(0xFFFFFFFF)
            ),
            isDark = false
        )
    }
}

