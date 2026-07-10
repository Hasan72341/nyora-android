package com.nyora.hasan72341.core.model

import android.content.res.Resources
import com.nyora.hasan72341.R

private val leadingChapterTitle = Regex(
	"""^\s*(?:chapter|chap|ch\.?|episode|ep\.?|vol(?:ume)?\b|第\s*\d+(?:\.\d+)?\s*(?:话|話|章|回))""",
	RegexOption.IGNORE_CASE,
)

fun formatLocalizedChapterTitle(
	resources: Resources,
	title: String?,
	number: String?,
	volume: String?,
	index: Int = -1,
): String {
	val cleanTitle = title?.trim().takeUnless { it.isNullOrBlank() }
	val base = when {
		number != null && volume != null -> resources.getString(R.string.chapter_volume_number, volume, number)
		number != null -> resources.getString(R.string.chapter_number, number)
		index > 0 -> resources.getString(
			R.string.chapters_time_pattern,
			resources.getString(R.string.unnamed_chapter),
			index.toString(),
		)
		else -> resources.getString(R.string.unnamed_chapter)
	}
	return when {
		cleanTitle == null -> base
		leadingChapterTitle.containsMatchIn(cleanTitle) -> cleanTitle
		number != null || volume != null -> "$base - $cleanTitle"
		else -> cleanTitle
	}
}
