package com.nyora.hasan72341.download.ui.dialog

import androidx.collection.ArraySet
import androidx.collection.LongLongMap
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import com.nyora.hasan72341.mihon.parsers.util.mapNotNullToSet

interface ChaptersSelectMacro {

	fun getChaptersIds(mangaId: Long, chapters: List<MangaChapter>): Set<Long>?

	class WholeManga(
		val chaptersCount: Int,
	) : ChaptersSelectMacro {

		override fun getChaptersIds(mangaId: Long, chapters: List<MangaChapter>): Set<Long>? = null
	}

	class WholeBranch(
		val branches: Map<String?, Int>,
		val selectedBranch: String?,
	) : ChaptersSelectMacro {

		val chaptersCount: Int = branches[selectedBranch] ?: 0

		override fun getChaptersIds(
			mangaId: Long,
			chapters: List<MangaChapter>
		): Set<Long> = chapters.mapNotNullToSet { c ->
			if (c.branch == selectedBranch) {
				c.id.toLongOrNull()
			} else {
				null
			}
		}

		fun copy(branch: String?) = WholeBranch(branches, branch)
	}

	class FirstChapters(
		val chaptersCount: Int,
		val maxAvailableCount: Int,
		val branch: String?,
	) : ChaptersSelectMacro {

		override fun getChaptersIds(mangaId: Long, chapters: List<MangaChapter>): Set<Long> {
			val result = ArraySet<Long>(minOf(chaptersCount, chapters.size))
			for (c in chapters) {
				if (c.branch == branch) {
					c.id.toLongOrNull()?.let { result.add(it) }
					if (result.size >= chaptersCount) {
						break
					}
				}
			}
			return result
		}

		fun copy(count: Int) = FirstChapters(count, maxAvailableCount, branch)
	}

	class UnreadChapters(
		val chaptersCount: Int,
		val maxAvailableCount: Int,
		private val currentChaptersIds: LongLongMap,
	) : ChaptersSelectMacro {

		override fun getChaptersIds(mangaId: Long, chapters: List<MangaChapter>): Set<Long>? {
			if (chapters.isEmpty()) {
				return null
			}
			val currentChapterId = currentChaptersIds.getOrDefault(mangaId, chapters.first().id.toLongOrNull() ?: -1L)
			var branch: String? = null
			var isAdding = false
			val result = ArraySet<Long>(minOf(chaptersCount, chapters.size))
			for (c in chapters) {
				if (!isAdding) {
					if (c.id.toLongOrNull() == currentChapterId) {
						branch = c.branch
						isAdding = true
					}
				}
				if (isAdding) {
					if (c.branch == branch) {
						c.id.toLongOrNull()?.let { result.add(it) }
						if (result.size >= chaptersCount) {
							break
						}
					}
				}
			}
			return result
		}

		fun copy(count: Int) = UnreadChapters(count, maxAvailableCount, currentChaptersIds)
	}
}
