package com.nyora.hasan72341.details.ui.pager.pages

import android.content.Context
import com.nyora.hasan72341.core.ui.BaseListAdapter
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.core.ui.list.fastscroll.FastScroller
import com.nyora.hasan72341.list.ui.adapter.ListItemType
import com.nyora.hasan72341.list.ui.adapter.listHeaderAD
import com.nyora.hasan72341.list.ui.model.ListModel

class PageThumbnailAdapter(
	clickListener: OnListItemClickListener<PageThumbnail>,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.PAGE_THUMB, pageThumbnailAD(clickListener))
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
