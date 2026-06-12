package com.nyora.hasan72341.explore.ui.model

import com.nyora.hasan72341.list.ui.model.ListModel

data class ExploreButtons(
	val isRandomLoading: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ExploreButtons
	}
}
