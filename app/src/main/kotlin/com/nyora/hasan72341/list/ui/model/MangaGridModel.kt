package com.nyora.hasan72341.list.ui.model

import com.nyora.hasan72341.core.ui.model.MangaOverride
import com.nyora.hasan72341.list.domain.ReadingProgress
import com.nyora.hasan72341.list.ui.ListModelDiffCallback.Companion.PAYLOAD_ANYTHING_CHANGED
import com.nyora.hasan72341.list.ui.ListModelDiffCallback.Companion.PAYLOAD_PROGRESS_CHANGED
import com.nyora.hasan72341.mihon.parsers.model.Manga

data class MangaGridModel(
	override val manga: Manga,
	override val override: MangaOverride?,
	override val counter: Int,
	val progress: ReadingProgress?,
	val isFavorite: Boolean,
	val isSaved: Boolean,
) : MangaListModel() {

	override fun getChangePayload(previousState: ListModel): Any? = when {
		previousState !is MangaGridModel || previousState.manga != manga -> null

		previousState.progress != progress -> PAYLOAD_PROGRESS_CHANGED
		previousState.isFavorite != isFavorite ||
			previousState.isSaved != isSaved -> PAYLOAD_ANYTHING_CHANGED

		else -> super.getChangePayload(previousState)
	}
}
