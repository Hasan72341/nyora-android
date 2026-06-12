package com.nyora.hasan72341.mihon.parsers.bitmap

interface Bitmap {

	val width: Int
	val height: Int

	fun drawBitmap(sourceBitmap: Bitmap, src: Rect, dst: Rect)
}
