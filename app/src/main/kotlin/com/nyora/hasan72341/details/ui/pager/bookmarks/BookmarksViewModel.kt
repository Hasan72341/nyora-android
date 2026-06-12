package com.nyora.hasan72341.details.ui.pager.bookmarks

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.nyora.hasan72341.R
import com.nyora.hasan72341.bookmarks.domain.Bookmark
import com.nyora.hasan72341.bookmarks.domain.BookmarksRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.observeAsStateFlow
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.ui.util.ReversibleAction
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.core.util.ext.requireValue
import com.nyora.hasan72341.details.data.MangaDetails
import com.nyora.hasan72341.list.ui.model.EmptyState
import com.nyora.hasan72341.list.ui.model.ListHeader
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.list.ui.model.LoadingState
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.reader.ui.PageSaveHelper
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
	private val bookmarksRepository: BookmarksRepository,
	settings: AppSettings,
) : BaseViewModel(), FlowCollector<MangaDetails?> {

	private val manga = MutableStateFlow<Manga?>(null)
	val onActionDone = MutableEventFlow<ReversibleAction>()

	val gridScale = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_GRID_SIZE_PAGES,
		valueProducer = { gridSizePages / 100f },
	)

	val content: StateFlow<List<ListModel>> = manga.filterNotNull().flatMapLatest { m ->
		bookmarksRepository.observeBookmarks(m)
			.map { mapList(m, it) }
	}.withErrorHandling()
		.filterNotNull()
		.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Lazily, listOf(LoadingState))

	override suspend fun emit(value: MangaDetails?) {
		manga.value = value?.toManga()
	}

	fun removeBookmarks(ids: Set<Long>) {
		launchJob(Dispatchers.IO) {
			val handle = bookmarksRepository.removeBookmarks(ids.mapTo(HashSet()) { it.toString() })
			onActionDone.call(ReversibleAction(R.string.bookmarks_removed, handle))
		}
	}

	fun savePages(pageSaveHelper: PageSaveHelper, ids: Set<Long>) {
		launchLoadingJob(Dispatchers.IO) {
			val m = manga.requireValue()
			val tasks = content.value.mapNotNull {
				if (it !is Bookmark || it.pageId.toLongOrNull() !in ids) return@mapNotNull null
				PageSaveHelper.Task(
					manga = m,
					chapterId = it.chapterId.toLong(),
					pageNumber = it.page + 1,
					page = it.toMangaPage(),
				)
			}
			val dest = pageSaveHelper.save(tasks)
			val msg = if (dest.size == 1) R.string.page_saved else R.string.pages_saved
			onActionDone.call(ReversibleAction(msg, null))
		}
	}

	private fun mapList(manga: Manga, bookmarks: List<Bookmark>): List<ListModel>? {
		val chapters = manga.chapters ?: return null
		val bookmarksMap = bookmarks.groupBy { it.chapterId }
		val result = ArrayList<ListModel>(bookmarks.size + bookmarksMap.size)
		for (chapter in chapters) {
			val b = bookmarksMap[chapter.id]
			if (b.isNullOrEmpty()) {
				continue
			}
			result += ListHeader(chapter)
			result.addAll(b)
		}
		if (result.isEmpty()) {
			result.add(
				EmptyState(
					icon = 0,
					textPrimary = R.string.no_bookmarks_yet,
					textSecondary = R.string.no_bookmarks_summary,
					actionStringRes = 0,
				),
			)
		}
		return result
	}
}
