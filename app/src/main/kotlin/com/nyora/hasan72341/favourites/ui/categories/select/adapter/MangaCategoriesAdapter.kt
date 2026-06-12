package com.nyora.hasan72341.favourites.ui.categories.select.adapter

import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.favourites.ui.categories.select.model.MangaCategoryItem
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.emptyStateListAD
import com.nyora.hasan72341.list.ui.adapter.loadingStateAD
import com.nyora.hasan72341.list.ui.model.ListModel

class MangaCategoriesAdapter(
	clickListener: OnListItemClickListener<MangaCategoryItem>,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.NAV_ITEM, mangaCategoryAD(clickListener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(null))
	}
}
