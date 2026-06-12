package com.nyora.hasan72341.core.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.customview.view.AbsSavedState
import androidx.dynamicanimation.animation.SpringAnimation
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.nyora.hasan72341.core.ui.anim.SpringPresets
import com.nyora.hasan72341.core.ui.anim.springTranslationY
import com.nyora.hasan72341.core.util.ext.getThemeColorStateList
import com.nyora.hasan72341.core.util.ext.measureHeight
import kotlin.math.max
import com.google.android.material.R as materialR

private const val STATE_DOWN = 1
private const val STATE_UP = 2

private const val SLIDE_UP_ANIMATION_DURATION = 225L
private const val SLIDE_DOWN_ANIMATION_DURATION = 175L

private const val MAX_ITEM_COUNT = 6

class SlidingBottomNavigationView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	@AttrRes defStyleAttr: Int = materialR.attr.bottomNavigationStyle,
	@StyleRes defStyleRes: Int = materialR.style.Widget_Design_BottomNavigationView,
) : NavigationBarView(context, attrs, defStyleAttr, defStyleRes),
	CoordinatorLayout.AttachedBehavior {

	private var currentSpringAnim: SpringAnimation? = null

	private var currentState = STATE_UP
	private var behavior = HideBottomNavigationOnScrollBehavior()

	init {
		val cornerRadiusPx = 100f * resources.displayMetrics.density
		val shapeModel = ShapeAppearanceModel.builder()
			.setAllCornerSizes(cornerRadiusPx)
			.build()
		val bgDrawable = MaterialShapeDrawable(shapeModel).apply {
			fillColor = context.getThemeColorStateList(com.google.android.material.R.attr.colorSurfaceContainerHigh)
			elevation = 4f * resources.displayMetrics.density
			strokeColor = context.getThemeColorStateList(com.google.android.material.R.attr.colorOutlineVariant)
			strokeWidth = 1f * resources.displayMetrics.density
		}
		background = bgDrawable
		elevation = 4f * resources.displayMetrics.density
		setPadding(12.dp, 0, 12.dp, 0)
	}

	private val Int.dp: Int
		get() = (this * resources.displayMetrics.density).toInt()

	var isPinned: Boolean
		get() = behavior.isPinned
		set(value) {
			behavior.isPinned = value
			if (value) {
				translationX = 0f
			}
		}

	val isShownOrShowing: Boolean
		get() = isVisible && currentState == STATE_UP

	override fun getBehavior(): CoordinatorLayout.Behavior<*> {
		return behavior
	}

	/** From BottomNavigationView **/

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val minHeightSpec = makeMinHeightSpec(heightMeasureSpec)
		super.onMeasure(widthMeasureSpec, minHeightSpec)
		if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
			setMeasuredDimension(
				measuredWidth,
				max(
					measuredHeight,
					suggestedMinimumHeight + paddingTop + paddingBottom,
				),
			)
		}
	}

	private fun makeMinHeightSpec(measureSpec: Int): Int {
		var minHeight = suggestedMinimumHeight
		if (MeasureSpec.getMode(measureSpec) != MeasureSpec.EXACTLY && minHeight > 0) {
			minHeight += paddingTop + paddingBottom

			return MeasureSpec.makeMeasureSpec(
				max(MeasureSpec.getSize(measureSpec), minHeight), MeasureSpec.AT_MOST,
			)
		}

		return measureSpec
	}

	override fun getMaxItemCount(): Int = MAX_ITEM_COUNT

	@SuppressLint("RestrictedApi")
	override fun createNavigationBarMenuView(context: Context) = BottomNavigationMenuView(context)

	/** End **/

	override fun onSaveInstanceState(): Parcelable {
		val superState = super.onSaveInstanceState()
		return SavedState(superState, currentState, translationY)
	}

	override fun onRestoreInstanceState(state: Parcelable?) {
		if (state is SavedState) {
			super.onRestoreInstanceState(state.superState)
			super.setTranslationY(state.translationY)
			currentState = state.currentState
		} else {
			super.onRestoreInstanceState(state)
		}
	}

	override fun setTranslationY(translationY: Float) {
		// Disallow translation change when state down
		if (currentState != STATE_DOWN) {
			super.setTranslationY(translationY)
		}
	}

	override fun setMinimumHeight(minHeight: Int) {
		super.setMinimumHeight(minHeight)
		getChildAt(0)?.minimumHeight = minHeight
	}

	fun show() {
		if (currentState == STATE_UP) {
			return
		}
		currentSpringAnim?.cancel()
		clearAnimation()

		currentState = STATE_UP
		currentSpringAnim = springTranslationY(0f, SpringPresets.SLIDE) {
			currentSpringAnim = null
			postInvalidate()
		}
	}

	fun hide() {
		if (currentState == STATE_DOWN) {
			return
		}
		currentSpringAnim?.cancel()
		clearAnimation()

		currentState = STATE_DOWN
		val target = measureHeight() + marginBottom
		if (target == 0) {
			return
		}
		currentSpringAnim = springTranslationY(target.toFloat(), SpringPresets.QUICK) {
			currentSpringAnim = null
			postInvalidate()
		}
	}

	fun showOrHide(show: Boolean) {
		if (show) {
			show()
		} else {
			hide()
		}
	}

	internal class SavedState : AbsSavedState {

		var currentState = STATE_UP
		var translationY = 0F

		constructor(superState: Parcelable, currentState: Int, translationY: Float) : super(superState) {
			this.currentState = currentState
			this.translationY = translationY
		}

		constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
			currentState = source.readInt()
			translationY = source.readFloat()
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			out.writeInt(currentState)
			out.writeFloat(translationY)
		}

		companion object {

			@Suppress("unused")
			@JvmField
			val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
				override fun createFromParcel(`in`: Parcel) = SavedState(`in`, SavedState::class.java.classLoader)

				override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
			}
		}
	}
}
