package com.nyora.hasan72341.tracker.ui.feed.model

import com.nyora.hasan72341.list.ui.ListModelDiffCallback
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.MangaListModel

data class UpdatedMangaHeader(
	val list: List<MangaListModel>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is UpdatedMangaHeader
	}

	override fun getChangePayload(previousState: ListModel): Any {
		return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
	}
}
