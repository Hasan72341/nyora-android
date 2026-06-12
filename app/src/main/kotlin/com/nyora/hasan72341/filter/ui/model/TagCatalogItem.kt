package com.nyora.hasan72341.filter.ui.model

import com.nyora.hasan72341.list.ui.ListModelDiffCallback
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.model.MangaTag

data class TagCatalogItem(
	val tag: MangaTag,
	val isChecked: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is TagCatalogItem && other.tag == tag
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is TagCatalogItem && previousState.isChecked != isChecked) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
