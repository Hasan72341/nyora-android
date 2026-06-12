package com.nyora.hasan72341.history.data

import dagger.Reusable
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.db.entity.toManga
import com.nyora.hasan72341.history.domain.model.MangaWithHistory
import com.nyora.hasan72341.list.domain.ListFilterOption
import com.nyora.hasan72341.list.domain.ListSortOrder
import com.nyora.hasan72341.local.data.index.LocalMangaIndex
import com.nyora.hasan72341.local.domain.LocalObserveMapper
import com.nyora.hasan72341.mihon.parsers.model.Manga
import javax.inject.Inject

@Reusable
class HistoryLocalObserver @Inject constructor(
	localMangaIndex: LocalMangaIndex,
	private val db: MangaDatabase,
) : LocalObserveMapper<HistoryWithManga, MangaWithHistory>(localMangaIndex) {

	fun observeAll(
		order: ListSortOrder,
		filterOptions: Set<ListFilterOption>,
		limit: Int
	) = db.getHistoryDao().observeAll(order, filterOptions, limit).mapToLocal()

	override fun toManga(e: HistoryWithManga) = e.manga.toManga()

	override fun toResult(e: HistoryWithManga, manga: Manga) = MangaWithHistory(
		manga = manga,
		history = e.history.toMangaHistory(),
	)
}
