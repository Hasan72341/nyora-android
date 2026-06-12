package com.nyora.hasan72341.settings.sources.catalog

import com.nyora.hasan72341.list.ui.ListModelDiffCallback
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.model.ContentType

data class SourceCatalogPage(
	val type: ContentType,
	val items: List<SourceCatalogItem>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is SourceCatalogPage && other.type == type
	}

	override fun getChangePayload(previousState: ListModel): Any {
		return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
	}
}
