package com.nyora.hasan72341.bookmarks.ui

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nyora.hasan72341.bookmarks.domain.Bookmark
import com.nyora.hasan72341.core.util.ext.getItem
import com.nyora.hasan72341.list.ui.MangaSelectionDecoration

class BookmarksSelectionDecoration(context: Context) : MangaSelectionDecoration(context) {

	override fun getItemId(parent: RecyclerView, child: View): String? {
		val holder = parent.getChildViewHolder(child) ?: return null
		val item = holder.getItem(Bookmark::class.java) ?: return null
		return item.pageId
	}
}
