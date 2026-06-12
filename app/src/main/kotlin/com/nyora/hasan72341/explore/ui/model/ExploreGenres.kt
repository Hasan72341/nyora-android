package com.nyora.hasan72341.explore.ui.model

import androidx.annotation.StringRes
import com.nyora.hasan72341.list.ui.model.ListModel

data class GenreSection(
	val genres: List<GenreTile>,
) : ListModel {
	override fun areItemsTheSame(other: ListModel): Boolean = other is GenreSection
}

data class GenreTile(
	@StringRes val titleRes: Int,
	val query: String,            // English token passed to TAG search
) : ListModel {
	override fun areItemsTheSame(other: ListModel): Boolean = other is GenreTile && other.query == query
}

data object ExploreSearchHero : ListModel {
	override fun areItemsTheSame(other: ListModel): Boolean = other is ExploreSearchHero
}
