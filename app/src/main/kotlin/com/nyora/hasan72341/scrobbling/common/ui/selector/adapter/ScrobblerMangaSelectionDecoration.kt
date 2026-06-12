package com.nyora.hasan72341.scrobbling.common.ui.selector.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.nyora.hasan72341.core.util.ext.getItem
import com.nyora.hasan72341.list.ui.MangaSelectionDecoration
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerManga

class ScrobblerMangaSelectionDecoration(context: Context) : MangaSelectionDecoration(context) {

	var checkedItemId: Long
		get() = if (selection.size == 1) {
			selection.first().toLongOrNull() ?: NO_ID
		} else {
			NO_ID
		}
		set(value) {
			clearSelection()
			if (value != NO_ID) {
				selection.add(value.toString())
			}
		}

	override fun getItemId(parent: RecyclerView, child: View): String? {
		val holder = parent.getChildViewHolder(child) ?: return null
		val item = holder.getItem(ScrobblerManga::class.java) ?: return null
		return item.id.toString()
	}

	override fun onDrawForeground(
		canvas: Canvas,
		parent: RecyclerView,
		child: View,
		bounds: RectF,
		state: RecyclerView.State,
	) {
		paint.color = strokeColor
		paint.style = Paint.Style.STROKE
		canvas.drawRoundRect(bounds, defaultRadius, defaultRadius, paint)
	}
}
