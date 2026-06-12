package com.nyora.hasan72341.details.ui.scrobbling

import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.list.ui.model.ListModel

class ScrollingInfoAdapter(
	router: AppRouter,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(scrobblingInfoAD(router))
	}
}
