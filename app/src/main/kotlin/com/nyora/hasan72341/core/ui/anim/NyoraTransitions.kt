package com.nyora.hasan72341.core.ui.anim

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import android.view.animation.PathInterpolator
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis

/**
 * Transition system for Nyora — Material Motion meets Apple-grade smoothness.
 * 
 * Usage:
 * - Activity → Activity: Use [makeContainerTransformOptions] with shared element
 * - Fragment → Fragment: Use [applyFadeThroughTransition] or [applySharedAxisTransition]
 * - Detail hero: Use [createContainerTransform] for cover image morph
 */
object NyoraTransitions {

    /** Apple's default spring-like interpolator approximation for Material transitions */
    val PREMIUM_INTERPOLATOR = PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)

    /** Standard duration for major transitions (ms) */
    const val DURATION_STANDARD = 450L

    /** Short duration for small element transitions (ms) */
    const val DURATION_SHORT = 300L

    /** Micro duration for indicator / badge animations (ms) */
    const val DURATION_MICRO = 200L

    /**
     * Create a MaterialContainerTransform for shared-element transitions.
     * Used for: grid/list item → details page cover morph.
     */
    fun createContainerTransform(
        startView: View,
        endViewId: Int,
        duration: Long = DURATION_STANDARD,
    ): MaterialContainerTransform {
        return MaterialContainerTransform().apply {
            this.startView = startView
            this.drawingViewId = endViewId
            this.duration = duration
            this.interpolator = PREMIUM_INTERPOLATOR
            scrimColor = android.graphics.Color.TRANSPARENT
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
        }
    }

    /**
     * Create ActivityOptions for a container transform shared-element launch.
     */
    fun makeContainerTransformOptions(
        activity: Activity,
        sharedElement: View,
        transitionName: String,
    ): ActivityOptionsCompat {
        ViewCompat.setTransitionName(sharedElement, transitionName)
        return ActivityOptionsCompat.makeSceneTransitionAnimation(
            activity,
            sharedElement,
            transitionName,
        )
    }

    /**
     * Apply fade-through transition for fragment tab switches.
     * Used for: bottom navigation tab changes.
     */
    fun applyFadeThroughTransition(fragment: Fragment) {
        fragment.enterTransition = MaterialFadeThrough().apply {
            duration = DURATION_SHORT
        }
        fragment.exitTransition = MaterialFadeThrough().apply {
            duration = DURATION_SHORT
        }
    }

    /**
     * Apply shared-axis transition for hierarchical navigation.
     * Used for: settings drill-down, search → results, list → detail.
     */
    fun applySharedAxisTransition(
        fragment: Fragment,
        axis: Int = MaterialSharedAxis.Z,
        forward: Boolean = true,
    ) {
        fragment.enterTransition = MaterialSharedAxis(axis, forward).apply {
            duration = DURATION_SHORT
        }
        fragment.returnTransition = MaterialSharedAxis(axis, !forward).apply {
            duration = DURATION_SHORT
        }
    }

    /**
     * Configure a FragmentTransaction with fade-through for tab switches.
     */
    fun FragmentTransaction.withFadeThrough(): FragmentTransaction {
        setCustomAnimations(
            com.google.android.material.R.anim.m3_motion_fade_enter,
            com.google.android.material.R.anim.m3_motion_fade_exit,
        )
        return this
    }

    /**
     * Configure the activity window for premium transitions.
     * Call in Activity.onCreate() before setContentView().
     */
    fun configureWindowTransitions(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            window.sharedElementsUseOverlay = true
            window.allowEnterTransitionOverlap = true
            window.allowReturnTransitionOverlap = true
        }
    }
}
