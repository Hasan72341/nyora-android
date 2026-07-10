package com.nyora.hasan72341.ai.engine

import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrProvider @Inject constructor() {
	private val jaRec = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
	private val zhRec = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
	private val koRec = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
	private val enRec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

	suspend fun runEnsembleOcr(image: InputImage): TextResult = coroutineScope {
		val ja = async { runCatching { jaRec.process(image).await() } }
		val zh = async { runCatching { zhRec.process(image).await() } }
		val ko = async { runCatching { koRec.process(image).await() } }
		val en = async { runCatching { enRec.process(image).await() } }

		val results = listOf(ja.await(), zh.await(), ko.await(), en.await())
			.mapNotNull { it.getOrNull() }
		if (results.isEmpty()) {
			return@coroutineScope TextResult(emptyList(), "en")
		}
		
		// Pick the engine that found the most meaningful CJK content
		val best = results.maxByOrNull { result ->
			calculateTextScore(result.text)
		} ?: results.first()

		TextResult(
			blocks = best.textBlocks.map { it.toMangaBlock() },
			language = detectScript(best.text)
		)
	}

	private fun calculateTextScore(text: String): Int {
		if (text.isBlank()) return 0
		var score = text.length
		// Bonus for actual CJK characters to avoid Latin fallback being picked for CJK
		val cjkCount = text.count { it.code in 0x4E00..0x9FFF || it.code in 0x3040..0x30FF || it.code in 0xAC00..0xD7AF }
		return score + (cjkCount * 5)
	}

	private fun detectScript(text: String): String {
		return when {
			text.any { it.code in 0xAC00..0xD7AF } -> "ko"
			text.any { it.code in 0x3040..0x30FF } -> "ja"
			text.any { it.code in 0x4E00..0x9FFF } -> "zh"
			else -> "en"
		}
	}

	private fun Text.TextBlock.toMangaBlock() = MangaBlock(
		text = text,
		boundingBox = boundingBox ?: Rect(),
		cornerPoints = cornerPoints?.toList() ?: emptyList()
	)

	data class TextResult(val blocks: List<MangaBlock>, val language: String)
	data class MangaBlock(val text: String, val boundingBox: Rect, val cornerPoints: List<android.graphics.Point>)
}
