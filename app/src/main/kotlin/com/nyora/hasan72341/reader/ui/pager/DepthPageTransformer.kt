package com.nyora.hasan72341.reader.ui.pager

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * Cinematic depth page transformer for the manga reader.
 * 
 * The outgoing page scales down and dims while the incoming page
 * slides in from the edge — creating a layered, 3D-like effect
 * that feels premium and intentional.
 * 
 * Inspired by Apple's page transitions in Books.app.
 * Optimized for 120Hz rendering with minimal overdraw.
 */
class DepthPageTransformer : ViewPager2.PageTransformer {

    companion object {
        /** Minimum scale for the background page */
        private const val MIN_SCALE = 0.88f

        /** Minimum alpha (dim) for the background page */
        private const val MIN_ALPHA = 0.5f

        /** Elevation change for depth perception */
        private const val ELEVATION_DP = 8f
    }

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width

        when {
            position < -1f -> {
                // Page is far off-screen to the left
                page.alpha = 0f
            }

            position <= 0f -> {
                // Page is transitioning from center to left (outgoing)
                // Apply scale-down + dim effect
                val scaleFactor = MIN_SCALE + (1f - MIN_SCALE) * (1f - abs(position))
                val alphaFactor = MIN_ALPHA + (1f - MIN_ALPHA) * (1f - abs(position))

                page.alpha = alphaFactor
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor

                // Push the page down slightly for depth
                page.translationY = pageWidth * position * 0.05f

                // Reduce elevation as it goes back
                page.translationZ = -ELEVATION_DP * abs(position)
            }

            position <= 1f -> {
                // Page is transitioning from right to center (incoming)
                // Standard slide from right
                page.alpha = 1f
                page.translationX = 0f
                page.translationY = 0f
                page.scaleX = 1f
                page.scaleY = 1f

                // Incoming page has higher elevation
                page.translationZ = ELEVATION_DP * (1f - position)
            }

            else -> {
                // Page is far off-screen to the right
                page.alpha = 0f
            }
        }
    }
}
