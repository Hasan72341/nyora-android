package com.nyora.hasan72341.core.ui.anim

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * Spring animation presets for Nyora's premium motion system.
 *
 * Apple-grade spring physics: every UI element should feel physically grounded.
 * These presets replace standard interpolator-based animations throughout the app.
 */
object SpringPresets {

    /** Snappy response — taps, selections, small UI state changes (like Apple's default) */
    val SNAPPY = SpringConfig(stiffness = 800f, dampingRatio = 0.7f)

    /** Gentle movement — page transitions, large layout changes */
    val GENTLE = SpringConfig(stiffness = 200f, dampingRatio = 0.85f)

    /** Bouncy feedback — FAB, bottom sheet peek, playful interactions */
    val BOUNCY = SpringConfig(stiffness = 400f, dampingRatio = 0.5f)

    /** Quick settle — toolbar show/hide, nav bar, status changes */
    val QUICK = SpringConfig(stiffness = 1000f, dampingRatio = 0.9f)

    /** Slide — bottom nav, search bar, panel reveals */
    val SLIDE = SpringConfig(stiffness = 600f, dampingRatio = 0.78f)

    /** Micro — tiny feedback animations (badges, indicators) */
    val MICRO = SpringConfig(stiffness = 1200f, dampingRatio = 0.65f)
}

data class SpringConfig(
    val stiffness: Float,
    val dampingRatio: Float,
)

/**
 * Extension functions for easy spring animation on any View.
 */

fun View.springTranslationY(
    targetValue: Float,
    config: SpringConfig = SpringPresets.SNAPPY,
    onEnd: (() -> Unit)? = null,
): SpringAnimation {
    val anim = SpringAnimation(this, DynamicAnimation.TRANSLATION_Y, targetValue).apply {
        spring = SpringForce(targetValue).apply {
            stiffness = config.stiffness
            dampingRatio = config.dampingRatio
        }
        onEnd?.let { callback ->
            addEndListener { _, _, _, _ -> callback() }
        }
    }
    anim.start()
    return anim
}

fun View.springTranslationX(
    targetValue: Float,
    config: SpringConfig = SpringPresets.SNAPPY,
    onEnd: (() -> Unit)? = null,
): SpringAnimation {
    val anim = SpringAnimation(this, DynamicAnimation.TRANSLATION_X, targetValue).apply {
        spring = SpringForce(targetValue).apply {
            stiffness = config.stiffness
            dampingRatio = config.dampingRatio
        }
        onEnd?.let { callback ->
            addEndListener { _, _, _, _ -> callback() }
        }
    }
    anim.start()
    return anim
}

fun View.springScale(
    targetValue: Float,
    config: SpringConfig = SpringPresets.SNAPPY,
    onEnd: (() -> Unit)? = null,
) {
    SpringAnimation(this, DynamicAnimation.SCALE_X, targetValue).apply {
        spring = SpringForce(targetValue).apply {
            stiffness = config.stiffness
            dampingRatio = config.dampingRatio
        }
    }.start()

    SpringAnimation(this, DynamicAnimation.SCALE_Y, targetValue).apply {
        spring = SpringForce(targetValue).apply {
            stiffness = config.stiffness
            dampingRatio = config.dampingRatio
        }
        onEnd?.let { callback ->
            addEndListener { _, _, _, _ -> callback() }
        }
    }.start()
}

fun View.springAlpha(
    targetValue: Float,
    config: SpringConfig = SpringPresets.GENTLE,
    onEnd: (() -> Unit)? = null,
): SpringAnimation {
    val anim = SpringAnimation(this, DynamicAnimation.ALPHA, targetValue).apply {
        spring = SpringForce(targetValue).apply {
            stiffness = config.stiffness
            dampingRatio = config.dampingRatio
        }
        onEnd?.let { callback ->
            addEndListener { _, _, _, _ -> callback() }
        }
    }
    anim.start()
    return anim
}

/**
 * Press-and-release scale animation for touch feedback.
 * Scales down on press, springs back on release.
 */
fun View.applyPressAnimation(
    pressScale: Float = 0.96f,
    config: SpringConfig = SpringPresets.BOUNCY,
) {
    setOnTouchListener { v, event ->
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                v.springScale(pressScale, config)
            }
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                v.springScale(1.0f, config)
            }
        }
        false // Don't consume — let click listeners work
    }
}

/**
 * Stagger-animate a list of views with spring physics.
 * Each view animates with a delay relative to the previous.
 */
fun staggerSpringIn(
    views: List<View>,
    delayPerItem: Long = 50L,
    config: SpringConfig = SpringPresets.GENTLE,
    translationY: Float = 60f,
) {
    views.forEachIndexed { index, view ->
        view.alpha = 0f
        view.translationY = translationY
        view.postDelayed({
            view.springAlpha(1f, config)
            view.springTranslationY(0f, config)
        }, index * delayPerItem)
    }
}
