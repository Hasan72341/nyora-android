package com.nyora.hasan72341.favourites.ui.container

import com.nyora.hasan72341.list.ui.model.ListModel

data class FavouriteTabModel(
	val id: Long,
	val title: String?,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is FavouriteTabModel && other.id == id
	}
}
