package com.nyora.hasan72341.list.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ui.list.decor.AbstractSelectionItemDecoration
import com.nyora.hasan72341.core.util.ext.getItem
import com.nyora.hasan72341.core.util.ext.getThemeColor
import com.nyora.hasan72341.list.ui.model.MangaListModel
import androidx.appcompat.R as appcompatR
import com.google.android.material.R as materialR

open class MangaSelectionDecoration(context: Context) : AbstractSelectionItemDecoration() {

	protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	protected val strokeColor = context.getThemeColor(appcompatR.attr.colorPrimary, Color.RED)
	protected val fillColor = ColorUtils.setAlphaComponent(
		ColorUtils.blendARGB(strokeColor, context.getThemeColor(materialR.attr.colorSurface), 0.8f),
		0x74,
	)
	protected val defaultRadius = context.resources.getDimension(R.dimen.list_selector_corner)

	init {
		hasBackground = false
		hasForeground = true
		isIncludeDecorAndMargins = false

		paint.strokeWidth = context.resources.getDimension(R.dimen.selection_stroke_width)
	}

	override fun getItemId(parent: RecyclerView, child: View): String? {
		val holder = parent.getChildViewHolder(child) ?: return null
		val item = holder.getItem(MangaListModel::class.java) ?: return null
		return item.id
	}

	override fun onDrawForeground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State,
	) {
		val radius = (child as? CardView)?.radius ?: defaultRadius
		paint.color = fillColor
		paint.style = Paint.Style.FILL
		canvas.drawRoundRect(bounds, radius, radius, paint)
		paint.color = strokeColor
		paint.style = Paint.Style.STROKE
		canvas.drawRoundRect(bounds, radius, radius, paint)
	}
}
