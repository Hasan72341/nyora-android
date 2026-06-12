package com.nyora.hasan72341.mihon.parsers.bitmap

data class Rect(
	val left: Int = 0,
	val top: Int = 0,
	val right: Int = 0,
	val bottom: Int = 0,
) {

	val width: Int
		get() = right - left

	val height: Int
		get() = bottom - top
}
