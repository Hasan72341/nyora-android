package com.nyora.hasan72341.mihon.parsers.model

import kotlinx.serialization.Serializable

/**
 * Native domain model, mirroring the canonical nyora-mac shared model
 * (com.nyora.hasan72341.shared.model.Manga). The persisted schema (String ids, List
 * collections, isNsfw/unread/progress fields) follows the nyora-mac DB. Computed helpers
 * (author, altTitle, branches, findChapterById, ...) are added on top for the Android UI
 * and are not part of the serialized shape.
 */
@Serializable
data class Manga(
	val id: String,
	val title: String,
	val altTitles: List<String> = emptyList(),
	val url: String = "",
	val publicUrl: String = "",
	val rating: Float = RATING_UNKNOWN,
	val isNsfw: Boolean = false,
	val contentRating: ContentRating? = null,
	val coverUrl: String = "",
	val largeCoverUrl: String? = null,
	val state: MangaState? = null,
	val authors: List<String> = emptyList(),
	val source: MangaSourceRef = MangaSourceRef.Unknown,
	val description: String = "",
	val tags: List<MangaTag> = emptyList(),
	val chapters: List<MangaChapter> = emptyList(),
	val unread: Int = 0,
	val progress: Float = 0f,
) {

	val author: String?
		get() = authors.firstOrNull()

	val altTitle: String?
		get() = altTitles.firstOrNull()

	fun getChapters(branch: String?): List<MangaChapter> =
		chapters.filter { it.branch == branch }

	fun findChapterById(id: String): MangaChapter? =
		chapters.find { it.id == id }

	fun requireChapterById(id: String): MangaChapter =
		findChapterById(id) ?: throw NoSuchElementException("Chapter $id not found in manga ${this.id}")

	val branches: Map<String?, Int>
		get() = chapters.groupingBy { it.branch }.eachCount()
}

@Serializable
data class MangaChapter(
	val id: String,
	val title: String,
	val number: Float = 0f,
	val volume: Int = 0,
	val url: String = "",
	val scanlator: String? = null,
	val uploadDate: Long = 0L,
	val branch: String? = null,
	val pages: List<MangaPage> = emptyList(),
	val index: Int = 0,
) {
	val name: String
		get() = title.ifEmpty { numberString().orEmpty() }

	fun numberString(): String? = if (number > 0f) {
		if (number % 1f == 0f) number.toInt().toString() else number.toString()
	} else {
		null
	}

	fun volumeString(): String? = if (volume > 0) volume.toString() else null
}

@Serializable
data class MangaPage(
	val url: String,
	val headers: Map<String, String> = emptyMap(),
	val id: String = "",
	val index: Int = 0,
	val preview: String? = null,
	@kotlinx.serialization.Transient
	val source: MangaSource? = null,
)

@Serializable
data class MangaTag(
	val key: String,
	val title: String,
)

@Serializable
enum class MangaState {
	ONGOING, FINISHED, ABANDONED, PAUSED, UPCOMING, RESTRICTED,
}

// ContentRating is defined in com.nyora.hasan72341.mihon.parsers.model.ContentRating
// RATING_UNKNOWN / YEAR_MIN / YEAR_UNKNOWN are defined in Constants.kt
