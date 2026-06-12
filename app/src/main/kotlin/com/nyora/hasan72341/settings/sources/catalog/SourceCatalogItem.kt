package com.nyora.hasan72341.settings.sources.catalog

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.model.MangaSource

sealed interface SourceCatalogItem : ListModel {

	data class Source(
		val source: MangaSource,
	) : SourceCatalogItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Source && other.source == source
		}
	}

	data class Hint(
		@DrawableRes val icon: Int,
		@StringRes val title: Int,
		@StringRes val text: Int,
	) : SourceCatalogItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Hint && other.title == title
		}
	}
}
