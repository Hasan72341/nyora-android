package com.nyora.hasan72341.discover.ui.adapter

import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.discover.ui.model.DiscoverHeroItem
import com.nyora.hasan72341.discover.ui.model.DiscoverRailCard
import com.nyora.hasan72341.list.ui.adapter.ListHeaderClickListener
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.ListStateHolderListener
import com.nyora.hasan72341.list.ui.adapter.emptyHintAD
import com.nyora.hasan72341.list.ui.adapter.listHeaderAD
import com.nyora.hasan72341.list.ui.adapter.loadingStateAD
import com.nyora.hasan72341.explore.ui.adapter.exploreRecommendationItemAD
import com.nyora.hasan72341.list.ui.model.ListModel

class DiscoverAdapter(
	cardClick: (DiscoverRailCard) -> Unit,
	heroClick: (DiscoverHeroItem) -> Unit,
	headerClickListener: ListHeaderClickListener,
	stateHolderListener: ListStateHolderListener,
	retry: () -> Unit,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.HEADER, listHeaderAD(headerClickListener))
		addDelegate(ListItemType.DISCOVER_HERO, discoverHeroAD(heroClick))
		addDelegate(ListItemType.DISCOVER_RAIL, discoverRailAD(cardClick))
		addDelegate(ListItemType.DISCOVER_RAIL_LOADING, discoverRailLoadingAD())
		addDelegate(ListItemType.DISCOVER_RAIL_ERROR, discoverRailErrorAD(retry))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.HINT_EMPTY, emptyHintAD(stateHolderListener))
		addDelegate(ListItemType.EXPLORE_SUGGESTION, exploreRecommendationItemAD { manga, v ->
			cardClick(DiscoverRailCard(
				id = manga.id.toLongOrNull() ?: manga.id.hashCode().toLong(),
				title = manga.title,
				coverUrl = manga.coverUrl,
				source = null,
				manga = manga,
				searchQuery = null
			))
		})
	}
}
