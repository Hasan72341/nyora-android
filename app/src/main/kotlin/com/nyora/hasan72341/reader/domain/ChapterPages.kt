package com.nyora.hasan72341.reader.domain

import com.nyora.hasan72341.reader.ui.pager.ReaderPage

class ChapterPages private constructor(private val pages: ArrayDeque<ReaderPage>) : List<ReaderPage> by pages {

	// map chapterId to index range in the pages deque
	private val indices = LinkedHashMap<String, IntRange>()

	constructor() : this(ArrayDeque())

	val chaptersSize: Int
		get() = indices.size

	@Synchronized
	fun removeFirst() {
		val chapterId = pages.first().chapterId
		indices.remove(chapterId)
		var delta = 0
		while (pages.first().chapterId == chapterId) {
			pages.removeFirst()
			delta--
		}
		shiftIndices(delta)
	}

	@Synchronized
	fun removeLast() {
		val chapterId = pages.last().chapterId
		indices.remove(chapterId)
		while (pages.last().chapterId == chapterId) {
			pages.removeLast()
		}
	}

	@Synchronized
	fun addLast(id: String, newPages: List<ReaderPage>): Boolean {
		if (id in indices) {
			return false
		}
		indices[id] = pages.size until (pages.size + newPages.size)
		pages.addAll(newPages)
		return true
	}

	@Synchronized
	fun addFirst(id: String, newPages: List<ReaderPage>): Boolean {
		if (id in indices) {
			return false
		}
		shiftIndices(newPages.size)
		indices[id] = newPages.indices
		pages.addAll(0, newPages)
		return true
	}

	@Synchronized
	fun clear() {
		indices.clear()
		pages.clear()
	}

	fun size(id: String) = indices[id]?.run {
		endInclusive - start + 1
	} ?: 0

	fun subList(id: String): List<ReaderPage> {
		val range = indices[id] ?: return emptyList()
		return pages.subList(range.first, range.last + 1)
	}

	operator fun contains(chapterId: String) = chapterId in indices

	private fun shiftIndices(delta: Int) {
		for (key in indices.keys.toList()) {
			indices[key] = indices.getValue(key) + delta
		}
	}

	private operator fun IntRange.plus(delta: Int): IntRange {
		return IntRange(start + delta, endInclusive + delta)
	}
}
