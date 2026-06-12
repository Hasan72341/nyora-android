package com.nyora.hasan72341.settings.tracker.categories

import com.nyora.hasan72341.core.model.FavouriteCategory
import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener

class TrackerCategoriesConfigAdapter(
	listener: OnListItemClickListener<FavouriteCategory>,
) : BaseListAdapter<FavouriteCategory>() {

	init {
		delegatesManager.addDelegate(trackerCategoryAD(listener))
	}
}
