package com.nyora.hasan72341.mihon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MihonChapterNormalizerTest {

	@Test
	fun `uses url chapter number like web parser`() {
		val result = MihonChapterNormalizer.normalize(
			name = "Chapter 3 - Chapter 2 - Who Needs a System",
			url = "/series/nano-machine/chapter-2",
			sourceNumber = 3f,
		)

		assertEquals(2f, result.number)
		assertEquals("Who Needs a System", result.title)
	}

	@Test
	fun `uses last repeated leading chapter label when source number is positional`() {
		val result = MihonChapterNormalizer.normalize(
			name = "Chapter 3 - Chapter 2 - Who Needs a System",
			url = "/series/nano-machine/anything",
			sourceNumber = 3f,
		)

		assertEquals(2f, result.number)
		assertEquals("Who Needs a System", result.title)
	}

	@Test
	fun `single chapter prefix becomes clean title and number`() {
		val result = MihonChapterNormalizer.normalize(
			name = "Chapter 5 - I Want to Take a Break",
			url = "/read/story/5",
			sourceNumber = -1f,
		)

		assertEquals(5f, result.number)
		assertEquals("I Want to Take a Break", result.title)
	}

	@Test
	fun `plain numbered chapter keeps no redundant title`() {
		val result = MihonChapterNormalizer.normalize(
			name = "Chapter 12",
			url = "/chapter-12",
			sourceNumber = -1f,
		)

		assertEquals(12f, result.number)
		assertNull(result.title)
	}

	@Test
	fun `plain title falls back to source number`() {
		val result = MihonChapterNormalizer.normalize(
			name = "Special Extra",
			url = "/special-extra",
			sourceNumber = 14f,
		)

		assertEquals(14f, result.number)
		assertEquals("Special Extra", result.title)
	}
}
