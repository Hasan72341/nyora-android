package com.nyora.hasan72341.explore.ui.model

import com.nyora.hasan72341.core.model.MangaSourceInfo
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.util.longHashCode

data class MangaSourceItem(
	val source: MangaSourceInfo,
	val isGrid: Boolean,
) : ListModel {

	val id: Long = source.name.longHashCode()

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaSourceItem && other.source == source
	}
}
