package com.nyora.hasan72341.favourites.ui.categories.select.model

import com.google.android.material.checkbox.MaterialCheckBox.CheckedState
import com.nyora.hasan72341.core.model.FavouriteCategory
import com.nyora.hasan72341.list.ui.ListModelDiffCallback
import com.nyora.hasan72341.list.ui.model.ListModel

data class MangaCategoryItem(
	val category: FavouriteCategory,
	@CheckedState val checkedState: Int,
	val isTrackerEnabled: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaCategoryItem && other.category.id == category.id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is MangaCategoryItem && previousState.checkedState != checkedState) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
