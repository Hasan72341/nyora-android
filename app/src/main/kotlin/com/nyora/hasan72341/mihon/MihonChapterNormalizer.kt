package com.nyora.hasan72341.mihon

/**
 * Normalizes chapter metadata returned by Mihon/Tachiyomi extensions before it
 * enters Nyora's domain model.
 *
 * Extension chapter names are not consistent: some provide a clean title, some
 * include the chapter number in the name, and some duplicate a positional
 * number before the real chapter label, for example:
 *
 *   Chapter 3 - Chapter 2 - Who Needs...
 *
 * Nyora renders the number separately in list metadata and reader state, so the
 * domain title should contain only the human chapter title.
 */
internal object MihonChapterNormalizer {

	data class Result(
		val number: Float,
		val title: String?,
	)

	private data class Prefix(
		val number: Float,
		val end: Int,
	)

	private val titleNumberPatterns = listOf(
		Regex("""(?:chapter|chap|ch\.?|episode|ep\.?)\s*[:#\-]?\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE),
		Regex("""第\s*(\d+(?:\.\d+)?)\s*(?:话|話|章|回)""", RegexOption.IGNORE_CASE),
		Regex("""(?:^|\s)(\d+(?:\.\d+)?)\s*(?:화|話|话|章|回)(?:\s|$)""", RegexOption.IGNORE_CASE),
	)

	private val leadingChapterLabel = Regex(
		"""^\s*(?:chapter|chap|ch\.?|episode|ep\.?)\s*[:#\-]?\s*(\d+(?:\.\d+)?)(?:\s*[-:–—]\s*)?""",
		RegexOption.IGNORE_CASE,
	)

	private val leadingCjkChapterLabel = Regex(
		"""^\s*第\s*(\d+(?:\.\d+)?)\s*(?:话|話|章|回)(?:\s*[-:–—]\s*)?""",
		RegexOption.IGNORE_CASE,
	)

	private val chapterUrlPatterns = listOf(
		Regex("""(?:^|[/_-])chapter[/_-](\d+(?:\.\d+)?)(?:[/_-]|$)""", RegexOption.IGNORE_CASE),
		Regex("""(?:^|[/_-])chap[/_-](\d+(?:\.\d+)?)(?:[/_-]|$)""", RegexOption.IGNORE_CASE),
		Regex("""(?:^|[/_-])ch[/_.-]?(\d+(?:\.\d+)?)(?:[/_-]|$)""", RegexOption.IGNORE_CASE),
	)

	fun normalize(name: String, url: String, sourceNumber: Float): Result {
		val cleanName = name.trim()
		val prefixes = leadingPrefixes(cleanName)
		val urlNumber = extractUrlNumber(url)
		val titleNumber = when {
			prefixes.isNotEmpty() -> prefixes.last().number
			else -> extractTitleNumber(cleanName)
		}
		val number = urlNumber
			?: titleNumber
			?: sourceNumber.takeIf { it > 0f }
			?: 0f

		val title = cleanName
			.stripLeadingLabels(prefixes)
			.normalizeSeparators()
			.takeIf { it.isNotBlank() }

		return Result(number = number, title = title)
	}

	fun extractTitleNumber(name: String): Float? {
		val trimmed = name.trim().takeIf { it.isNotEmpty() } ?: return null
		return titleNumberPatterns.firstNotNullOfOrNull { pattern ->
			pattern.find(trimmed)?.groupValues?.getOrNull(1)?.toFloatOrNull()
		} ?: trimmed.toFloatOrNull()
	}

	private fun extractUrlNumber(url: String): Float? {
		val value = url.trim().takeIf { it.isNotEmpty() } ?: return null
		return chapterUrlPatterns.firstNotNullOfOrNull { pattern ->
			pattern.find(value)?.groupValues?.getOrNull(1)?.toFloatOrNull()
		}
	}

	private fun leadingPrefixes(value: String): List<Prefix> {
		val result = ArrayList<Prefix>(2)
		var remaining = value
		var consumed = 0
		while (remaining.isNotBlank()) {
			val match = leadingChapterLabel.find(remaining) ?: leadingCjkChapterLabel.find(remaining) ?: break
			val number = match.groupValues.getOrNull(1)?.toFloatOrNull() ?: break
			consumed += match.range.last + 1
			result += Prefix(number = number, end = consumed)
			remaining = value.substring(consumed)
		}
		return result
	}

	private fun String.stripLeadingLabels(prefixes: List<Prefix>): String {
		if (prefixes.isEmpty()) {
			return this
		}
		return substring(prefixes.last().end.coerceAtMost(length))
	}

	private fun String.normalizeSeparators(): String =
		trim().trimStart('-', ':', '–', '—').trim()
}
