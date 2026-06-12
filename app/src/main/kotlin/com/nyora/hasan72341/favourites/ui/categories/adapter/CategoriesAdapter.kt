package com.nyora.hasan72341.favourites.ui.categories.adapter

import com.nyora.hasan72341.core.ui.ReorderableListAdapter
import com.nyora.hasan72341.favourites.ui.categories.FavouriteCategoriesListListener
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.ListStateHolderListener
import com.nyora.hasan72341.list.ui.adapter.emptyStateListAD
import com.nyora.hasan72341.list.ui.adapter.loadingStateAD
import com.nyora.hasan72341.list.ui.model.ListModel

class CategoriesAdapter(
	onItemClickListener: FavouriteCategoriesListListener,
	listListener: ListStateHolderListener,
) : ReorderableListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.CATEGORY_LARGE, categoryAD(onItemClickListener))
		addDelegate(ListItemType.NAV_ITEM, allCategoriesAD(onItemClickListener))
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(listListener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
	}
}
