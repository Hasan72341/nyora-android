package com.nyora.hasan72341.reader.ui.pager

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.nyora.hasan72341.mihon.parsers.model.MangaPage

@Parcelize
data class ReaderPage(
	val url: String,
	val chapterId: String,
	val index: Int,
	val headers: Map<String, String>,
	val source: String? = null,
) : Parcelable {

	constructor(page: MangaPage, index: Int, chapterId: String) : this(
		url = page.url,
		chapterId = chapterId,
		index = index,
		headers = page.headers ?: emptyMap(),
		source = page.source?.name,
	)

	fun toMangaPage() = MangaPage(
		url = url,
		headers = headers,
		source = source?.let { com.nyora.hasan72341.core.model.MangaSource(it) },
	)
}
