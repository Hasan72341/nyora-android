package com.nyora.hasan72341.reader.domain

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

data class ReaderColorFilter(
	val brightness: Float,
	val contrast: Float,
	val isInverted: Boolean,
	val isGrayscale: Boolean,
	val isBookBackground: Boolean,
	val multitonePreset: Preset = Preset.NONE,
) {

	val isEmpty: Boolean
		get() = multitonePreset == Preset.NONE && !isGrayscale && !isInverted && !isBookBackground && brightness == 0f && contrast == 0f

	fun toColorFilter(): ColorMatrixColorFilter {
		val cm = ColorMatrix()
		
		if (multitonePreset != Preset.NONE) {
			cm.applyMultitone(multitonePreset)
		} else if (isGrayscale) {
			cm.grayscale()
		}
		
		if (isInverted) {
			cm.inverted()
		}
		
		cm.setBrightness(brightness)
		cm.setContrast(contrast)
		
		if (isBookBackground) {
			cm.addBookEffect()
		}
		
		return ColorMatrixColorFilter(cm)
	}

	fun getBackgroundTint(): ColorStateList? = if (isBookBackground) {
		val color = Color.rgb(255, 255, (255 * BOOK_BLUE_FACTOR).toInt())
		ColorStateList.valueOf(color)
	} else {
		null
	}

	private fun ColorMatrix.setBrightness(brightness: Float) {
		val scale = brightness + 1f
		val matrix = ColorMatrix()
		matrix.setScale(scale, scale, scale, 1f)
		postConcat(matrix)
	}

	private fun ColorMatrix.setContrast(contrast: Float) {
		val scale = contrast + 1f
		val translate = (-.5f * scale + .5f) * 255f
		val array = floatArrayOf(
			scale, 0f, 0f, 0f, translate,
			0f, scale, 0f, 0f, translate,
			0f, 0f, scale, 0f, translate,
			0f, 0f, 0f, 1f, 0f,
		)
		val matrix = ColorMatrix(array)
		postConcat(matrix)
	}

	private fun ColorMatrix.inverted() {
		val matrix = floatArrayOf(
			-1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
			0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 0.0f, 1.0f, 0.0f,
		)
		postConcat(ColorMatrix(matrix))
	}

	private fun ColorMatrix.grayscale() {
		setSaturation(0f)
	}

	private fun ColorMatrix.applyMultitone(preset: Preset) {
		setSaturation(0f)
		
		if (preset.contrastOffset != 0f) {
			setContrast(preset.contrastOffset)
		}

		if (preset.matrixArray != null) {
			postConcat(ColorMatrix(preset.matrixArray))
			return
		}
		
		val dark = preset.darkColor
		val light = preset.lightColor
		
		val rDark = Color.red(dark) / 255f
		val gDark = Color.green(dark) / 255f
		val bDark = Color.blue(dark) / 255f
		
		val rLight = Color.red(light) / 255f
		val gLight = Color.green(light) / 255f
		val bLight = Color.blue(light) / 255f
		
		val rScale = rLight - rDark
		val gScale = gLight - gDark
		val bScale = bLight - bDark
		
		val multitoneMatrix = floatArrayOf(
			rScale, 0f, 0f, 0f, rDark * 255f,
			0f, gScale, 0f, 0f, gDark * 255f,
			0f, 0f, bScale, 0f, bDark * 255f,
			0f, 0f, 0f, 1f, 0f
		)
		
		postConcat(ColorMatrix(multitoneMatrix))
	}

	private fun ColorMatrix.addBookEffect() {
		val removeBlueMatrix = floatArrayOf(
			1f, 0f, 0f, 0f, 0f,
			0f, 1f, 0f, 0f, 0f,
			0f, 0f, BOOK_BLUE_FACTOR, 0f, 0f,
			0f, 0f, 0f, 1f, 0f,
		)
		postConcat(ColorMatrix(removeBlueMatrix))
	}

	enum class Preset(
		val id: Int,
		val darkColor: Int,
		val lightColor: Int,
		val contrastOffset: Float = 0f,
		val matrixArray: FloatArray? = null
	) {
		NONE(0, 0, 0),
		
		// Duotones (1 - 5)
		DUOTONE_SEPIA(1, Color.rgb(28, 22, 14), Color.rgb(252, 245, 229), 0.05f),
		DUOTONE_SLATE(2, Color.rgb(15, 23, 42), Color.rgb(241, 245, 249), -0.05f),
		DUOTONE_CYBERPUNK(3, Color.rgb(46, 8, 84), Color.rgb(224, 247, 250), 0.18f),
		DUOTONE_EMERALD(4, Color.rgb(12, 35, 24), Color.rgb(232, 245, 233), 0.02f),
		DUOTONE_TERRACOTTA(5, Color.rgb(46, 26, 22), Color.rgb(251, 239, 239), 0.06f),
		
		// Tritones (6 - 10)
		TRITONE_VINTAGE(6, 0, 0, 0f, floatArrayOf(
			1.4f, 0f, 0f, 0f, 10f,
			1.1f, 0f, 0f, 0f, 25f,
			0.7f, 0f, 0f, 0f, 50f,
			0f, 0f, 0f, 1f, 0f
		)),
		TRITONE_COPPER(7, 0, 0, 0f, floatArrayOf(
			1.3f, 0f, 0f, 0f, 30f,
			0.9f, 0f, 0f, 0f, 15f,
			0.6f, 0f, 0f, 0f, 10f,
			0f, 0f, 0f, 1f, 0f
		)),
		TRITONE_SUNSET(8, 0, 0, 0.05f, floatArrayOf(
			1.5f, 0f, 0f, 0f, 30f,
			0.8f, 0f, 0f, 0f, 10f,
			0.6f, 0f, 0f, 0f, 15f,
			0f, 0f, 0f, 1f, 0f
		)),
		TRITONE_BIOLUM(9, 0, 0, -0.02f, floatArrayOf(
			0.6f, 0f, 0f, 0f, 10f,
			1.2f, 0f, 0f, 0f, 28f,
			1.4f, 0f, 0f, 0f, 42f,
			0f, 0f, 0f, 1f, 0f
		)),
		TRITONE_SAGE(10, 0, 0, 0.04f, floatArrayOf(
			0.9f, 0f, 0f, 0f, 28f,
			1.3f, 0f, 0f, 0f, 30f,
			0.8f, 0f, 0f, 0f, 26f,
			0f, 0f, 0f, 1f, 0f
		)),
		
		// Quadratones (11 - 15)
		QUAD_ARCADE(11, 0, 0, 0.1f, floatArrayOf(
			1.5f, 0f, 0f, 0f, 11f,
			0.9f, 0f, 0f, 0f, 15f,
			1.3f, 0f, 0f, 0f, 25f,
			0f, 0f, 0f, 1f, 0f
		)),
		QUAD_CANDY(12, 0, 0, -0.05f, floatArrayOf(
			1.1f, 0f, 0f, 0f, 30f,
			1.2f, 0f, 0f, 0f, 27f,
			1.4f, 0f, 0f, 0f, 75f,
			0f, 0f, 0f, 1f, 0f
		)),
		QUAD_LAVENDER(13, 0, 0, 0.02f, floatArrayOf(
			1.0f, 0f, 0f, 0f, 30f,
			1.1f, 0f, 0f, 0f, 27f,
			1.5f, 0f, 0f, 0f, 75f,
			0f, 0f, 0f, 1f, 0f
		)),
		QUAD_AUTUMN(14, 0, 0, 0.08f, floatArrayOf(
			1.4f, 0f, 0f, 0f, 28f,
			1.1f, 0f, 0f, 0f, 25f,
			0.6f, 0f, 0f, 0f, 22f,
			0f, 0f, 0f, 1f, 0f
		)),
		QUAD_AURORA(15, 0, 0, -0.03f, floatArrayOf(
			0.5f, 0f, 0f, 0f, 2f,
			1.4f, 0f, 0f, 0f, 44f,
			1.1f, 0f, 0f, 0f, 34f,
			0f, 0f, 0f, 1f, 0f
		)),
		
		// Pentatones (16 - 20)
		PENTA_SYNTHWAVE(16, 0, 0, 0f, floatArrayOf(
			1.6f, 0f, 0f, 0f, 26f,
			1.1f, 0f, 0f, 0f, 11f,
			1.3f, 0f, 0f, 0f, 46f,
			0f, 0f, 0f, 1f, 0f
		)),
		PENTA_RAINBOW(17, 0, 0, 0f, floatArrayOf(
			1.5f, 0f, 0f, 0f, 36f,
			1.2f, 0f, 0f, 0f, 0f,
			0.9f, 0f, 0f, 0f, 70f,
			0f, 0f, 0f, 1f, 0f
		)),
		PENTA_CANOPY(18, 0, 0, 0.04f, floatArrayOf(
			0.7f, 0f, 0f, 0f, 6f,
			1.4f, 0f, 0f, 0f, 20f,
			0.9f, 0f, 0f, 0f, 13f,
			0f, 0f, 0f, 1f, 0f
		)),
		PENTA_CYBERCITY(19, 0, 0, 0.12f, floatArrayOf(
			1.4f, 0f, 0f, 0f, 30f,
			0.9f, 0f, 0f, 0f, 17f,
			1.3f, 0f, 0f, 0f, 42f,
			0f, 0f, 0f, 1f, 0f
		)),
		PENTA_PASTEL(20, 0, 0, -0.06f, floatArrayOf(
			1.1f, 0f, 0f, 0f, 30f,
			1.3f, 0f, 0f, 0f, 27f,
			1.4f, 0f, 0f, 0f, 75f,
			0f, 0f, 0f, 1f, 0f
		)),
		
		// Hexatones (21 - 25)
		HEX_ABYSS(21, 0, 0, -0.05f, floatArrayOf(
			0.7f, 0f, 0f, 0f, 3f,
			1.1f, 0f, 0f, 0f, 7f,
			1.5f, 0f, 0f, 0f, 24f,
			0f, 0f, 0f, 1f, 0f
		)),
		HEX_THERMAL(22, 0, 0, 0.15f, floatArrayOf(
			1.7f, 0f, 0f, 0f, 15f,
			1.1f, 0f, 0f, 0f, 5f,
			0.6f, 0f, 0f, 0f, 5f,
			0f, 0f, 0f, 1f, 0f
		)),
		HEX_SUNSET(23, 0, 0, 0.08f, floatArrayOf(
			1.5f, 0f, 0f, 0f, 30f,
			1.0f, 0f, 0f, 0f, 27f,
			0.8f, 0f, 0f, 0f, 75f,
			0f, 0f, 0f, 1f, 0f
		)),
		HEX_MEADOW(24, 0, 0, 0.03f, floatArrayOf(
			0.8f, 0f, 0f, 0f, 13f,
			1.4f, 0f, 0f, 0f, 19f,
			0.9f, 0f, 0f, 0f, 13f,
			0f, 0f, 0f, 1f, 0f
		)),
		HEX_VAPORWAVE(25, 0, 0, 0.05f, floatArrayOf(
			1.5f, 0f, 0f, 0f, 24f,
			0.7f, 0f, 0f, 0f, 2f,
			1.3f, 0f, 0f, 0f, 44f,
			0f, 0f, 0f, 1f, 0f
		)),
		
		// Heptatones (26 - 30)
		HEPTA_NEBULA(26, 0, 0, 0.02f, floatArrayOf(
			1.2f, 0f, 0f, 0f, 11f,
			0.9f, 0f, 0f, 0f, 9f,
			1.5f, 0f, 0f, 0f, 26f,
			0f, 0f, 0f, 1f, 0f
		)),
		HEPTA_GLITCH(27, 0, 0, 0.14f, floatArrayOf(
			1.6f, 0f, 0f, 0f, 3f,
			1.3f, 0f, 0f, 0f, 7f,
			1.4f, 0f, 0f, 0f, 18f,
			0f, 0f, 0f, 1f, 0f
		)),
		HEPTA_VINTAGE(28, 0, 0, 0.06f, floatArrayOf(
			1.4f, 0f, 0f, 0f, 28f,
			1.1f, 0f, 0f, 0f, 22f,
			0.7f, 0f, 0f, 0f, 14f,
			0f, 0f, 0f, 1f, 0f
		)),
		HEPTA_JUNGLE(29, 0, 0, 0.04f, floatArrayOf(
			0.7f, 0f, 0f, 0f, 2f,
			1.4f, 0f, 0f, 0f, 44f,
			0.9f, 0f, 0f, 0f, 35f,
			0f, 0f, 0f, 1f, 0f
		)),
		HEPTA_TWILIGHT(30, 0, 0, 0.01f, floatArrayOf(
			1.1f, 0f, 0f, 0f, 23f,
			0.9f, 0f, 0f, 0f, 36f,
			1.4f, 0f, 0f, 0f, 84f,
			0f, 0f, 0f, 1f, 0f
		)),
		
		// Octatones (31 - 35)
		OCTA_ACID(31, 0, 0, 0.15f, floatArrayOf(
			1.6f, 0f, 0f, 0f, 30f,
			1.3f, 0f, 0f, 0f, 5f,
			1.0f, 0f, 0f, 0f, 43f,
			0f, 0f, 0f, 1f, 0f
		)),
		OCTA_PRISM(32, 0, 0, 0.08f, floatArrayOf(
			1.4f, 0f, 0f, 0f, 9f,
			1.3f, 0f, 0f, 0f, 5f,
			1.5f, 0f, 0f, 0f, 20f,
			0f, 0f, 0f, 1f, 0f
		)),
		OCTA_EMERALD(33, 0, 0, 0.05f, floatArrayOf(
			0.6f, 0f, 0f, 0f, 2f,
			1.5f, 0f, 0f, 0f, 44f,
			0.9f, 0f, 0f, 0f, 35f,
			0f, 0f, 0f, 1f, 0f
		)),
		OCTA_SAKURA(34, 0, 0, -0.04f, floatArrayOf(
			1.2f, 0f, 0f, 0f, 31f,
			0.9f, 0f, 0f, 0f, 22f,
			1.4f, 0f, 0f, 0f, 75f,
			0f, 0f, 0f, 1f, 0f
		)),
		OCTA_ARCTIC(35, 0, 0, 0.02f, floatArrayOf(
			0.8f, 0f, 0f, 0f, 8f,
			1.2f, 0f, 0f, 0f, 20f,
			1.5f, 0f, 0f, 0f, 38f,
			0f, 0f, 0f, 1f, 0f
		));

		companion object {
			fun fromId(id: Int): Preset = entries.firstOrNull { it.id == id } ?: NONE
		}
	}

	companion object {

		private const val BOOK_BLUE_FACTOR = 0.92f

		val EMPTY = ReaderColorFilter(
			brightness = 0.0f,
			contrast = 0.0f,
			isInverted = false,
			isGrayscale = false,
			isBookBackground = false,
			multitonePreset = Preset.NONE,
		)
	}
}
