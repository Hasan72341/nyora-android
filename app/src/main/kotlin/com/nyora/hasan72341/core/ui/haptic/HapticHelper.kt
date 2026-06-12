package com.nyora.hasan72341.core.ui.haptic

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Centralized haptic feedback for premium touch interactions.
 * 
 * Every meaningful user action should have a corresponding haptic:
 * - Selections → tick
 * - Confirmations → confirm  
 * - Errors → reject
 * - Navigation → context click
 */
object HapticHelper {

    /** Light tick — tab switch, chip selection, toggle */
    fun tick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /** Confirmation — favorite added, download complete, action success */
    fun confirm(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    /** Rejection — error, invalid action, delete confirmation */
    fun reject(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /** Context selection — long press, drag start, menu open */
    fun selection(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /** Toggle — switch, checkbox state change */
    fun toggle(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
        } else {
            tick(view)
        }
    }

    /** Drag threshold crossed — reorder lists, slider snaps */
    fun dragThreshold(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE)
        } else {
            tick(view)
        }
    }
}
