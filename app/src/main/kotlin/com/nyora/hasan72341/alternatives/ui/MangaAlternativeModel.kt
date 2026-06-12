package com.nyora.hasan72341.alternatives.ui

import com.nyora.hasan72341.core.model.chaptersCount
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.MangaGridModel
import com.nyora.hasan72341.mihon.parsers.model.Manga

data class MangaAlternativeModel(
	val mangaModel: MangaGridModel,
	private val referenceChapters: Int,
) : ListModel {

	val manga: Manga
		get() = mangaModel.manga

	val chaptersCount = manga.chaptersCount()

	val chaptersDiff: Int
		get() = if (referenceChapters == 0 || chaptersCount == 0) 0 else chaptersCount - referenceChapters

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaAlternativeModel && other.manga.id == manga.id
	}

	override fun getChangePayload(previousState: ListModel): Any? = if (previousState is MangaAlternativeModel) {
		mangaModel.getChangePayload(previousState.mangaModel)
	} else {
		null
	}
}
