package com.nyora.hasan72341.scrobbling.common.ui.selector.adapter

import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.ListStateHolderListener
import com.nyora.hasan72341.list.ui.adapter.loadingFooterAD
import com.nyora.hasan72341.list.ui.adapter.loadingStateAD
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerManga

class ScrobblerSelectorAdapter(
	clickListener: OnListItemClickListener<ScrobblerManga>,
	stateHolderListener: ListStateHolderListener,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.MANGA_SCROBBLING, scrobblingMangaAD(clickListener))
		addDelegate(ListItemType.FOOTER_LOADING, loadingFooterAD())
		addDelegate(ListItemType.HINT_EMPTY, scrobblerHintAD(stateHolderListener))
	}
}
