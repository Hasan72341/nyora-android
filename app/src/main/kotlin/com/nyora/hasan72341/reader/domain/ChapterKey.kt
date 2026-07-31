package com.nyora.hasan72341.reader.domain

/**
 * The reader keys chapters into Long-keyed maps. Native sources have numeric ids; data-driven sources
 * use non-numeric String ids, which the old `toLong()`/`toLongOrNull() ?: 0L` paths either crashed on
 * or collapsed to a single 0L slot. This maps any id to a stable, unique Long. Must be used at every
 * chapter-id -> Long conversion so the put and lookup keys always match.
 */
fun String.toChapterKey(): Long = toLongOrNull() ?: run {
	var h = 1125899906842597L
	for (c in this) h = 31 * h + c.code
	h
}
