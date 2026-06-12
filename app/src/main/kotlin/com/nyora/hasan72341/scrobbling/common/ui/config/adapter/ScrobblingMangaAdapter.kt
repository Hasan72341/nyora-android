package com.nyora.hasan72341.scrobbling.common.ui.config.adapter

import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.emptyStateListAD
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingInfo

class ScrobblingMangaAdapter(
	clickListener: OnListItemClickListener<ScrobblingInfo>,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.HEADER, scrobblingHeaderAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(null))
		addDelegate(ListItemType.MANGA_SCROBBLING, scrobblingMangaAD(clickListener))
	}
}
