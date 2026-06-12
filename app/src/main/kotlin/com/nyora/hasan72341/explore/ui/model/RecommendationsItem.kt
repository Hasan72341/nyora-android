package com.nyora.hasan72341.explore.ui.model

import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.MangaCompactListModel

data class RecommendationsItem(
	val manga: List<MangaCompactListModel>
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is RecommendationsItem
	}
}
