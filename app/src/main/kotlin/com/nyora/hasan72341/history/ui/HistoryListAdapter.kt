package com.nyora.hasan72341.history.ui

import android.content.Context
import com.nyora.hasan72341.core.nav.router
import com.nyora.hasan72341.core.ui.list.fastscroll.FastScroller
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.MangaListAdapter
import com.nyora.hasan72341.list.ui.adapter.MangaListListener
import com.nyora.hasan72341.list.ui.adapter.historyStatsAD
import com.nyora.hasan72341.list.ui.size.ItemSizeResolver

class HistoryListAdapter(
	listener: MangaListListener,
	sizeResolver: ItemSizeResolver,
) : MangaListAdapter(listener, sizeResolver), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.HISTORY_STATS, historyStatsAD {
			(listener as? androidx.fragment.app.Fragment)?.router?.openStatistic()
		})
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
