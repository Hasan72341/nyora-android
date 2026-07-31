package com.nyora.hasan72341.settings.sources.catalog

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.model.MangaSource

sealed interface SourceCatalogItem : ListModel {

	data class Source(
		val source: MangaSource,
		val isEnabled: Boolean,
	) : SourceCatalogItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Source && other.source == source
		}

		// only the add/remove affordance changes when a source is toggled, so rebind
		// that button in place instead of rebuilding the whole row
		override fun getChangePayload(previousState: ListModel): Any? {
			return if (previousState is Source && previousState.isEnabled != isEnabled) {
				PAYLOAD_ENABLED_CHANGED
			} else {
				super.getChangePayload(previousState)
			}
		}
	}

	companion object {

		const val PAYLOAD_ENABLED_CHANGED = "enabled_changed"
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
