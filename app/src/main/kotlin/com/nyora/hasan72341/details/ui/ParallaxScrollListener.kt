package com.nyora.hasan72341.details.ui

import android.view.View
import androidx.core.widget.NestedScrollView

/**
 * Parallax scroll listener for the Details page hero header.
 *
 * Drives multiple simultaneous effects as the user scrolls:
 * - Cover blur background: parallax at 0.4x scroll rate  
 * - Toolbar: fades from transparent → solid
 * - Cover image: subtle scale reduction for depth
 *
 * This creates the cinematic, Apple-like hero header effect
 * that makes the details page feel alive and premium.
 */
class ParallaxScrollListener(
    private val blurBackground: View?,
    private val toolbar: View?,
    private val coverImage: View?,
) : NestedScrollView.OnScrollChangeListener {

    companion object {
        /** Parallax rate for background (0.0 = fixed, 1.0 = scrolls with content) */
        private const val PARALLAX_RATE = 0.4f

        /** Maximum scroll distance for toolbar fade (px) */
        private const val TOOLBAR_FADE_DISTANCE = 600f

        /** Minimum cover scale at max scroll */
        private const val MIN_COVER_SCALE = 0.92f

        /** Maximum scroll distance for cover scale effect */
        private const val COVER_SCALE_DISTANCE = 400f
    }

    override fun onScrollChange(
        v: NestedScrollView,
        scrollX: Int,
        scrollY: Int,
        oldScrollX: Int,
        oldScrollY: Int,
    ) {
        val scrollFloat = scrollY.toFloat()

        // Parallax background: moves slower than content for depth
        blurBackground?.translationY = scrollFloat * PARALLAX_RATE

        // Toolbar: fade in as user scrolls down
        toolbar?.let { tb ->
            val toolbarAlpha = (scrollFloat / TOOLBAR_FADE_DISTANCE).coerceIn(0f, 1f)
            tb.alpha = toolbarAlpha
            // Set background alpha separately if toolbar has a background
            tb.background?.alpha = (toolbarAlpha * 255).toInt()
        }

        // Cover: subtle scale-down for depth perception
        coverImage?.let { cover ->
            val scaleFraction = (scrollFloat / COVER_SCALE_DISTANCE).coerceIn(0f, 1f)
            val scale = 1f - (1f - MIN_COVER_SCALE) * scaleFraction
            cover.scaleX = scale
            cover.scaleY = scale
            // Slight fade as it scrolls away
            cover.alpha = 1f - (scaleFraction * 0.3f)
        }
    }
}
