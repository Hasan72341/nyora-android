package com.nyora.hasan72341.ai.colorize

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import java.util.Collections

/**
 * On-device manga colorization via manga-colorization-v2 (ONNX, fp16) run with ONNX Runtime for
 * Android — NNAPI accelerated where available, XNNPACK CPU otherwise. Pages never leave the device.
 *
 * This is a faithful port of nyora-web/web/core/colorize/worker.js so both platforms produce the
 * same result: resize_pad to the trained resolution, a 5-channel grayscale input (channels 1-4 = 0,
 * i.e. automatic colourisation with no hint), then a **luminance-combine** — the crisp line art
 * (luminance Y) comes from the ORIGINAL full-resolution page and only the colour (Cb/Cr) comes from
 * the model, so lines stay sharp while flat regions get colour.
 */
object MangaColorizer {

	private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
	private val initMutex = Mutex()   // guards session creation
	private val runMutex = Mutex()    // one page at a time — bounds peak memory on mobile
	private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	@Volatile
	private var session: OrtSession? = null

	fun isAvailable(context: Context): Boolean = ColorizeModelManager.isDownloaded(context)

	/** A page this size is safe to colorize without risking an out-of-memory crash. */
	fun canColorize(width: Int, height: Int): Boolean =
		width > 0 && height > 0 && width.toLong() * height <= ColorizeModel.MAX_PIXELS

	private suspend fun session(context: Context): OrtSession {
		session?.let { return it }
		return initMutex.withLock {
			session?.let { return@withLock it }
			val model = ColorizeModelManager.modelFile(context)
			require(model.isFile) { "Colorizer model not downloaded" }
			// CPU only. NNAPI was tried first, but many Android NNAPI drivers mis-partition this
			// fp16 GAN (Cast + Conv graph) and silently return zero-chroma output — the page then
			// combines back to plain grayscale, i.e. "colorize does nothing". The CPU EP runs the
			// fp16 casts correctly and matches the web (wasm) result; it's slower but always right.
			session = createSession(model.absolutePath)
			session!!
		}
	}

	private fun createSession(path: String): OrtSession {
		val opts = OrtSession.SessionOptions()
		opts.setIntraOpNumThreads((Runtime.getRuntime().availableProcessors() - 1).coerceIn(1, 4))
		opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
		return env.createSession(path, opts)
	}

	/**
	 * Free the ONNX session (e.g. when the reader closes). Safe to call while a page is being
	 * colorized: the field is cleared immediately so nothing reuses it, but the native session is
	 * closed only AFTER any in-flight inference releases [runMutex] — closing it mid-run would crash.
	 */
	fun close() {
		val old = session ?: return
		session = null
		cleanupScope.launch {
			runMutex.withLock {
				try { old.close() } catch (_: Throwable) { /* ignore */ }
			}
		}
	}

	/**
	 * Colorize one page. Returns a NEW ARGB_8888 bitmap the size of [src]; [src] is left untouched
	 * (the caller owns and recycles it). Heavy work runs on [Dispatchers.Default]; pages are
	 * processed one at a time to keep peak memory bounded.
	 */
	suspend fun colorize(context: Context, src: Bitmap): Bitmap {
		require(canColorize(src.width, src.height)) {
			"Page too large to colorize (${src.width}x${src.height})"
		}
		val ort = session(context)
		return runMutex.withLock {
			withContext(Dispatchers.Default) {
				try {
					run(ort, src)
				} catch (e: OutOfMemoryError) {
					// Free the ~hundreds-of-MB session so the next page can retry from a clean slate.
					close()
					throw e
				}
			}
		}
	}

	private fun run(ort: OrtSession, src: Bitmap): Bitmap {
		val ow = src.width
		val oh = src.height
		val size = ColorizeModel.SIZE

		// resize_pad (utils/utils.py): portrait → width = SIZE; landscape → height = SIZE*1.5,
		// then pad to a multiple of 32 with white. Identical to the web worker.
		val vw: Int
		val vh: Int
		if (oh < ow) {
			vh = Math.round(size * 1.5f)
			vw = Math.ceil(ow.toDouble() / (oh.toDouble() / (size * 1.5))).toInt()
		} else {
			vw = size
			vh = Math.ceil(oh.toDouble() / (ow.toDouble() / size)).toInt()
		}
		val mw = maxOf(32, Math.ceil(vw / 32.0).toInt() * 32)
		val mh = maxOf(32, Math.ceil(vh / 32.0).toInt() * 32)
		val plane = mw * mh

		// Draw the page into the padded (white) model canvas.
		val padded = Bitmap.createBitmap(mw, mh, Bitmap.Config.ARGB_8888)
		Canvas(padded).apply {
			drawColor(Color.WHITE)
			drawBitmap(src, Rect(0, 0, ow, oh), Rect(0, 0, vw, vh), FILTER)
		}
		val padPx = IntArray(plane)
		padded.getPixels(padPx, 0, mw, 0, 0, mw, mh)
		padded.recycle()

		// grayscale → model input; channels 1-4 stay 0 (automatic colourisation).
		val input = FloatArray(5 * plane)
		for (i in 0 until plane) {
			val p = padPx[i]
			val r = (p ushr 16) and 0xFF
			val g = (p ushr 8) and 0xFF
			val b = p and 0xFF
			input[i] = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
		}

		val rgb = FloatArray(3 * plane)
		OnnxTensor.createTensor(env, FloatBuffer.wrap(input), longArrayOf(1, 5, mh.toLong(), mw.toLong())).use { tensor ->
			ort.run(Collections.singletonMap(ColorizeModel.INPUT_NAME, tensor)).use { result ->
				val out = (result.get(ColorizeModel.OUTPUT_NAME).orElseGet { result.get(0) }) as OnnxTensor
				out.floatBuffer.get(rgb) // [1,3,mh,mw] in 0..1
			}
		}

		// model colour → bitmap (mw×mh)
		val colorPx = IntArray(plane)
		for (i in 0 until plane) {
			val r = clamp255(rgb[i] * 255f)
			val g = clamp255(rgb[plane + i] * 255f)
			val b = clamp255(rgb[2 * plane + i] * 255f)
			colorPx[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
		}
		val colorBmp = Bitmap.createBitmap(mw, mh, Bitmap.Config.ARGB_8888)
		colorBmp.setPixels(colorPx, 0, mw, 0, 0, mw, mh)

		// upscale ONLY the valid (unpadded) region to the original size
		val colorFullBmp = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
		Canvas(colorFullBmp).drawBitmap(colorBmp, Rect(0, 0, vw, vh), Rect(0, 0, ow, oh), FILTER)
		colorBmp.recycle()
		val colorFull = IntArray(ow * oh)
		colorFullBmp.getPixels(colorFull, 0, ow, 0, 0, ow, oh)
		colorFullBmp.recycle()

		// original page for luminance
		val orig = IntArray(ow * oh)
		src.getPixels(orig, 0, ow, 0, 0, ow, oh)

		// combine: source Y (crisp lines) + model Cb/Cr × SAT
		val sat = ColorizeModel.SATURATION
		val outPx = IntArray(ow * oh)
		for (i in outPx.indices) {
			val o = orig[i]
			val oR = (o ushr 16) and 0xFF
			val oG = (o ushr 8) and 0xFF
			val oB = o and 0xFF
			val y = 0.299f * oR + 0.587f * oG + 0.114f * oB

			val c = colorFull[i]
			val cr = (c ushr 16) and 0xFF
			val cg = (c ushr 8) and 0xFF
			val cb = c and 0xFF
			val cbC = (-0.168736f * cr - 0.331264f * cg + 0.5f * cb) * sat
			val crC = (0.5f * cr - 0.418688f * cg - 0.081312f * cb) * sat

			val rr = clamp255(y + 1.402f * crC)
			val gg = clamp255(y - 0.344136f * cbC - 0.714136f * crC)
			val bb = clamp255(y + 1.772f * cbC)
			outPx[i] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
		}
		return Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888).apply {
			setPixels(outPx, 0, ow, 0, 0, ow, oh)
		}
	}

	private val FILTER = Paint(Paint.FILTER_BITMAP_FLAG)

	private fun clamp255(v: Float): Int {
		val i = Math.round(v)
		return if (i < 0) 0 else if (i > 255) 255 else i
	}
}
