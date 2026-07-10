package com.nyora.hasan72341.core.model

import com.nyora.hasan72341.mihon.parsers.model.MangaChapter

/**
 * Returns chapters in chronological reading order, independent of how a source chooses to
 * present its chapter list. Many sources expose newest-first lists (300, 299, ...), while
 * the reader controls, progress, and prefetch logic must move through 1, 2, 3, ...
 */
fun List<MangaChapter>.toChronologicalChapterOrder(): List<MangaChapter> {
	if (size < 2) {
		return this
	}
	val keys = mapIndexed { index, chapter -> ChapterOrderKey.from(chapter, index) }
	val numberedCount = keys.count { it.number != null }
	if (numberedCount >= 2) {
		return withIndex().sortedWith(
			compareBy<IndexedValue<MangaChapter>>(
				{ keys[it.index].volume ?: 0 },
				{ keys[it.index].number ?: Float.MAX_VALUE },
				{ keys[it.index].uploadDate.takeIf { date -> date > 0L } ?: Long.MAX_VALUE },
				{ keys[it.index].originalIndex },
			),
		).map { it.value }
	}
	val datedCount = keys.count { it.uploadDate > 0L }
	if (datedCount >= 2) {
		return withIndex().sortedWith(
			compareBy<IndexedValue<MangaChapter>>(
				{ keys[it.index].uploadDate.takeIf { date -> date > 0L } ?: Long.MAX_VALUE },
				{ keys[it.index].originalIndex },
			),
		).map { it.value }
	}
	return this
}

private data class ChapterOrderKey(
	val volume: Int?,
	val number: Float?,
	val uploadDate: Long,
	val originalIndex: Int,
) {
	companion object {
		fun from(chapter: MangaChapter, originalIndex: Int) = ChapterOrderKey(
			volume = chapter.volume.takeIf { it > 0 },
			number = chapter.title.parseChapterNumber() ?: chapter.number.takeIf { it > 0f },
			uploadDate = chapter.uploadDate,
			originalIndex = originalIndex,
		)
	}
}

private fun String?.parseChapterNumber(): Float? {
	val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
	CHAPTER_PATTERNS.firstNotNullOfOrNull { pattern ->
		pattern.find(value)?.groupValues?.getOrNull(1)?.toFloatOrNull()
	}?.let {
		return it
	}
	return value.toFloatOrNull()
}

private val CHAPTER_PATTERNS = listOf(
	Regex("""(?:chapter|chap|ch\.?|episode|ep\.?)\s*[:#\-]?\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE),
	Regex("""第\s*(\d+(?:\.\d+)?)\s*(?:话|話|章|回)""", RegexOption.IGNORE_CASE),
	Regex("""(?:^|\s)(\d+(?:\.\d+)?)\s*(?:화|話|话|章|回)(?:\s|$)""", RegexOption.IGNORE_CASE),
)
