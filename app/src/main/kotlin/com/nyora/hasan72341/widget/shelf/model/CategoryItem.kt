package com.nyora.hasan72341.widget.shelf.model

import com.nyora.hasan72341.list.ui.ListModelDiffCallback
import com.nyora.hasan72341.list.ui.model.ListModel

data class CategoryItem(
	val id: Long,
	val name: String?,
	val isSelected: Boolean
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is CategoryItem && other.id == id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is CategoryItem && previousState.isSelected != isSelected) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			null
		}
	}
}
