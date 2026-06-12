package com.nyora.hasan72341.discover.ui.model

import com.nyora.hasan72341.list.ui.ListModelDiffCallback
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaSource

data class DiscoverHeroItem(
	val coverUrl: String?,
	val title: String,
	val genres: String,        // pre-joined ", " string (<=3 genres)
	val searchQuery: String,   // AniList title; hero action -> router.openSearch(searchQuery)
) : ListModel {
	override fun areItemsTheSame(other: ListModel) = other is DiscoverHeroItem
}

data class DiscoverRailCard(
	val id: Long,              // namespaced: real manga.id for source/history/updates; AniList uses -(anilistId.toLong())
	val title: String,
	val coverUrl: String?,
	val source: MangaSource?,  // null for AniList
	val manga: Manga?,         // non-null => tap opens Details; null (AniList) => tap opens Search(searchQuery)
	val searchQuery: String?,  // non-null only for AniList cards
) : ListModel {
	override fun areItemsTheSame(other: ListModel) = other is DiscoverRailCard && other.id == id
}

data class DiscoverRail(
	val railId: String,
	val items: List<DiscoverRailCard>,
) : ListModel {
	override fun areItemsTheSame(other: ListModel) = other is DiscoverRail && other.railId == railId

	override fun getChangePayload(previousState: ListModel): Any? =
		if (previousState is DiscoverRail && previousState.items != items) {
			ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		} else {
			null
		}
}

data class DiscoverRailLoading(val railId: String) : ListModel {
	override fun areItemsTheSame(other: ListModel) = other is DiscoverRailLoading && other.railId == railId
}

data class DiscoverRailError(val railId: String) : ListModel {
	override fun areItemsTheSame(other: ListModel) = other is DiscoverRailError && other.railId == railId
}

// Per-rail network state used by Item A return type and Item E combine:
sealed interface DiscoverRailState {
	data object Loading : DiscoverRailState
	data class Loaded(val items: List<DiscoverRailCard>) : DiscoverRailState
	data object Error : DiscoverRailState
}
