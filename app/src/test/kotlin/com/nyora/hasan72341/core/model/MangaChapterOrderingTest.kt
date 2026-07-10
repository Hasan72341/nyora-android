package com.nyora.hasan72341.core.model

import com.nyora.hasan72341.mihon.parsers.model.MangaChapter
import org.junit.Assert.assertEquals
import org.junit.Test

class MangaChapterOrderingTest {

	@Test
	fun `ascending numbered chapters stay ascending`() {
		val chapters = listOf(
			chapter("1", number = 1f),
			chapter("2", number = 2f),
			chapter("3", number = 3f),
		)

		assertEquals(listOf("1", "2", "3"), chapters.toChronologicalChapterOrder().ids())
	}

	@Test
	fun `descending numbered chapters become chronological`() {
		val chapters = listOf(
			chapter("300", number = 300f),
			chapter("299", number = 299f),
			chapter("298", number = 298f),
		)

		assertEquals(listOf("298", "299", "300"), chapters.toChronologicalChapterOrder().ids())
	}

	@Test
	fun `chapter title number wins over bad positional number`() {
		val chapters = listOf(
			chapter("actual-1", title = "Chapter 1", number = 295f),
			chapter("actual-2", title = "Chapter 2", number = 294f),
			chapter("actual-3", title = "Chapter 3", number = 293f),
		)

		assertEquals(
			listOf("actual-1", "actual-2", "actual-3"),
			chapters.toChronologicalChapterOrder().ids(),
		)
	}

	@Test
	fun `date-only chapters become oldest first`() {
		val chapters = listOf(
			chapter("new", uploadDate = 30L),
			chapter("mid", uploadDate = 20L),
			chapter("old", uploadDate = 10L),
		)

		assertEquals(listOf("old", "mid", "new"), chapters.toChronologicalChapterOrder().ids())
	}

	@Test
	fun `unknown chapters preserve source order`() {
		val chapters = listOf(
			chapter("a", title = "Prologue"),
			chapter("b", title = "Special"),
			chapter("c", title = "Extra"),
		)

		assertEquals(listOf("a", "b", "c"), chapters.toChronologicalChapterOrder().ids())
	}

	private fun List<MangaChapter>.ids() = map { it.id }

	private fun chapter(
		id: String,
		title: String = "",
		number: Float = 0f,
		uploadDate: Long = 0L,
	) = MangaChapter(
		id = id,
		title = title,
		number = number,
		uploadDate = uploadDate,
	)
}
