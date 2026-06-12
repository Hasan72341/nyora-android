package com.nyora.hasan72341.core.ui.color

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/**
 * Extracts dominant and accent colors from manga cover art.
 * Used to create per-manga adaptive theming — chips, progress bars,
 * FAB, and scrim gradients tinted to match the cover.
 *
 * This is the foundation for Nyora's signature visual identity:
 * every manga feels unique because the UI adapts to its art.
 */
object CoverColorExtractor {

    data class CoverPalette(
        @ColorInt val dominant: Int,
        @ColorInt val vibrant: Int,
        @ColorInt val muted: Int,
        @ColorInt val onDominant: Int,
        @ColorInt val scrimStart: Int,
        @ColorInt val scrimEnd: Int,
    )

    /**
     * Extract a palette from a cover bitmap.
     * Returns harmonized colors suitable for both light and dark themes.
     */
    fun extract(bitmap: Bitmap, isDarkTheme: Boolean): CoverPalette {
        val palette = Palette.from(bitmap)
            .maximumColorCount(16)
            .generate()

        val dominant = palette.getDominantColor(Color.TRANSPARENT)
        val vibrant = palette.getVibrantColor(dominant)
        val muted = palette.getMutedColor(dominant)

        // Ensure text readability on dominant color
        val onDominant = if (ColorUtils.calculateLuminance(dominant) > 0.5) {
            Color.BLACK
        } else {
            Color.WHITE
        }

        // Scrim gradient: transparent → themed surface with dominant tint
        val scrimEnd = if (isDarkTheme) {
            ColorUtils.blendARGB(Color.BLACK, dominant, 0.15f)
        } else {
            ColorUtils.blendARGB(Color.WHITE, dominant, 0.08f)
        }
        val scrimStart = ColorUtils.setAlphaComponent(scrimEnd, 0)

        return CoverPalette(
            dominant = harmonize(dominant, isDarkTheme),
            vibrant = harmonize(vibrant, isDarkTheme),
            muted = harmonize(muted, isDarkTheme),
            onDominant = onDominant,
            scrimStart = scrimStart,
            scrimEnd = scrimEnd,
        )
    }

    /**
     * Harmonize a raw palette color to work well as a UI accent.
     * Adjusts saturation and lightness to prevent harsh/ugly tints.
     */
    private fun harmonize(@ColorInt color: Int, isDarkTheme: Boolean): Int {
        if (color == Color.TRANSPARENT) return color
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)

        // Clamp saturation: not too gray, not too neon
        hsl[1] = hsl[1].coerceIn(0.3f, 0.75f)

        // Adjust lightness for theme
        if (isDarkTheme) {
            hsl[2] = hsl[2].coerceIn(0.55f, 0.75f) // Bright enough on dark bg
        } else {
            hsl[2] = hsl[2].coerceIn(0.30f, 0.50f) // Dark enough on light bg
        }

        return ColorUtils.HSLToColor(hsl)
    }
}
