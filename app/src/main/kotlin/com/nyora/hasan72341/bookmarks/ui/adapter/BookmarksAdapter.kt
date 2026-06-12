package com.nyora.hasan72341.bookmarks.ui.adapter

import android.content.Context
import com.nyora.hasan72341.bookmarks.domain.Bookmark
import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.ui.list.fastscroll.FastScroller
import com.nyora.hasan72341.list.ui.adapter.ListHeaderClickListener
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.emptyStateListAD
import com.nyora.hasan72341.list.ui.adapter.errorStateListAD
import com.nyora.hasan72341.list.ui.adapter.listHeaderAD
import com.nyora.hasan72341.list.ui.adapter.loadingFooterAD
import com.nyora.hasan72341.list.ui.adapter.loadingStateAD
import com.nyora.hasan72341.list.ui.model.ListModel

class BookmarksAdapter(
	clickListener: OnListItemClickListener<Bookmark>,
	headerClickListener: ListHeaderClickListener?,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.PAGE_THUMB, bookmarkLargeAD(clickListener))
		addDelegate(ListItemType.HEADER, listHeaderAD(headerClickListener))
		addDelegate(ListItemType.STATE_ERROR, errorStateListAD(null))
		addDelegate(ListItemType.FOOTER_LOADING, loadingFooterAD())
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(null))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
