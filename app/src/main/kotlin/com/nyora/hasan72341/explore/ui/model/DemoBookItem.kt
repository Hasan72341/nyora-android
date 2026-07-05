package com.nyora.hasan72341.explore.ui.model

import androidx.annotation.ColorInt
import com.nyora.hasan72341.list.ui.model.ListModel

data class DemoBookItem(
	val index: Int,
	val title: String,
	val subtitle: String,
	val glyph: String,
	@ColorInt val accentColor: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is DemoBookItem && other.index == index
	}

	override fun getChangePayload(previousState: ListModel): Any? = null
}
