package com.nyora.hasan72341.tracker.ui.feed.model

import com.nyora.hasan72341.core.model.withOverride
import com.nyora.hasan72341.core.ui.model.MangaOverride
import com.nyora.hasan72341.list.ui.ListModelDiffCallback
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.ifNullOrEmpty

data class FeedItem(
	val id: Long,
	private val override: MangaOverride?,
	val manga: Manga,
	val count: Int,
	val isNew: Boolean,
) : ListModel {

	val imageUrl: String?
		get() = override?.coverUrl.ifNullOrEmpty { manga.coverUrl }

	val title: String
		get() = override?.title.ifNullOrEmpty { manga.title }

	fun toMangaWithOverride() = manga.withOverride(override)

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is FeedItem && other.id == id
	}

	override fun getChangePayload(previousState: ListModel): Any? = when {
		previousState !is FeedItem -> null
		isNew != previousState.isNew -> ListModelDiffCallback.PAYLOAD_ANYTHING_CHANGED
		else -> super.getChangePayload(previousState)
	}
}
