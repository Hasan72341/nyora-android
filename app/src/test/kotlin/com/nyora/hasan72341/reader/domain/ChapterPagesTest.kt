package com.nyora.hasan72341.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.nyora.hasan72341.core.model.TestMangaSource
import com.nyora.hasan72341.reader.ui.pager.ReaderPage
import kotlin.random.Random

class ChapterPagesTest {

	@Test
	fun getChaptersSize() {
		val pages = ChapterPages()
		pages.addFirst("1", List(12) { page("1") })
		pages.addFirst("2", List(17) { page("2") })
		assertEquals(2, pages.chaptersSize)
	}

	@Test
	fun removeFirst() {
		val pages = ChapterPages()
		pages.addLast("1", List(12) { page("1") })
		pages.addLast("2", List(17) { page("2") })
		pages.addLast("4", List(2) { page("4") })
		pages.removeFirst()
		assertEquals(2, pages.chaptersSize)
		assertEquals(17 + 2, pages.size)
	}

	@Test
	fun removeLast() {
		val pages = ChapterPages()
		pages.addLast("1", List(12) { page("1") })
		pages.addLast("2", List(17) { page("2") })
		pages.addLast("4", List(2) { page("4") })
		pages.removeLast()
		assertEquals(2, pages.chaptersSize)
		assertEquals(12 + 17, pages.size)
	}

	@Test
	fun clear() {
		val pages = ChapterPages()
		pages.addLast("1", List(12) { page("1") })
		pages.addLast("2", List(17) { page("2") })
		pages.addLast("4", List(2) { page("4") })
		pages.clear()
		assertEquals(0, pages.chaptersSize)
		assertEquals(0, pages.size)
		assertEquals(0, pages.size("1"))
		assertEquals(0, pages.size("2"))
		assertEquals(0, pages.size("4"))
	}

	@Test
	fun subList() {
		val pages = ChapterPages()
		pages.addLast("1", List(12) { page("1") })
		pages.addLast("2", List(17) { page("2") })
		pages.addFirst("4", List(2) { page("4") })
		val subList = pages.subList("2")
		assertEquals(17, subList.size)
		assertEquals("2", subList.first().chapterId)
		assertEquals("2", subList.last().chapterId)
		assertTrue(subList.all { it.chapterId == "2" })
		assertEquals(subList.size, pages.size("2"))
	}

	private fun page(chapterId: String) = ReaderPage(
		url = "http://localhost",
		chapterId = chapterId,
		index = Random.nextInt(),
		headers = emptyMap(),
		source = TestMangaSource.name,
	)
}
