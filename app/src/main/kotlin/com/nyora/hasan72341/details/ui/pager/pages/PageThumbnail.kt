package com.nyora.hasan72341.details.ui.pager.pages

import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.reader.ui.pager.ReaderPage

data class PageThumbnail(
	val isCurrent: Boolean,
	val page: ReaderPage,
) : ListModel {

	val number
		get() = page.index + 1

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is PageThumbnail && page == other.page
	}
}
