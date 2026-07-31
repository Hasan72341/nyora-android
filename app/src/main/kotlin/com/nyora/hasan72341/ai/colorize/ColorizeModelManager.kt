package com.nyora.hasan72341.ai.colorize

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Downloads, caches and verifies the on-device colorizer model — the Android counterpart of
 * nyora-web/web/core/colorize/engine.js. The model is big (~62 MB) so it is downloaded EXPLICITLY
 * from Settings (with a progress bar) and the Colorize toggle stays locked until it's present, so
 * there's no surprise multi-tens-of-MB download mid-read. The bytes are SHA-256 verified before
 * they're ever handed to the ONNX parser.
 */
object ColorizeModelManager {

	private const val DIR = "colorize"

	fun modelFile(context: Context): File =
		File(File(context.filesDir, DIR).apply { mkdirs() }, ColorizeModel.MODEL_FILE)

	/** Cheap presence check used to gate the toggle; the full SHA is verified on load / after download. */
	fun isDownloaded(context: Context): Boolean {
		val f = modelFile(context)
		return f.isFile && f.length() == ColorizeModel.MODEL_BYTES
	}

	fun sizeOnDisk(context: Context): Long = modelFile(context).let { if (it.isFile) it.length() else 0L }

	fun delete(context: Context): Boolean = modelFile(context).delete()

	/**
	 * Download the model to [modelFile] with SHA-256 verification and progress (0..100). Writes to a
	 * `.part` file and atomically renames on success, so a killed/failed download never looks
	 * "downloaded" and never lands corrupt bytes that would only fail later inside the colorizer.
	 */
	suspend fun download(context: Context, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
		val target = modelFile(context)
		if (isDownloaded(context) && verify(target)) {
			onProgress(100)
			return@withContext
		}
		val part = File(target.parentFile, target.name + ".part")
		part.delete()

		val client = OkHttpClient.Builder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.build()
		val request = Request.Builder().url(ColorizeModel.MODEL_URL).build()

		val digest = MessageDigest.getInstance("SHA-256")
		client.newCall(request).execute().use { res ->
			if (!res.isSuccessful) throw IOException("Model download failed (${res.code})")
			val body = res.body ?: throw IOException("Empty download response")
			val total = body.contentLength().takeIf { it > 0L } ?: ColorizeModel.MODEL_BYTES
			body.byteStream().use { input ->
				part.outputStream().use { out ->
					val buf = ByteArray(64 * 1024)
					var got = 0L
					var lastPct = -1
					while (true) {
						coroutineContext.ensureActive()
						val n = input.read(buf)
						if (n < 0) break
						out.write(buf, 0, n)
						digest.update(buf, 0, n)
						got += n
						val pct = ((got * 100) / total).toInt().coerceIn(0, 100)
						if (pct != lastPct) {
							lastPct = pct
							onProgress(pct)
						}
					}
				}
			}
		}

		if (toHex(digest.digest()) != ColorizeModel.MODEL_SHA256) {
			part.delete()
			throw IOException("Model failed its integrity check — download rejected")
		}
		target.delete()
		if (!part.renameTo(target)) {
			part.delete()
			throw IOException("Could not finalize the downloaded model")
		}
		onProgress(100)
	}

	/** Full SHA-256 verification of an on-disk file against [ColorizeModel.MODEL_SHA256]. */
	suspend fun verify(file: File): Boolean = withContext(Dispatchers.IO) {
		if (!file.isFile) return@withContext false
		val digest = MessageDigest.getInstance("SHA-256")
		file.inputStream().use { input ->
			val buf = ByteArray(64 * 1024)
			while (true) {
				val n = input.read(buf)
				if (n < 0) break
				digest.update(buf, 0, n)
			}
		}
		toHex(digest.digest()) == ColorizeModel.MODEL_SHA256
	}

	/**
	 * Whether this device can run the colorizer without excessive lag. The model + per-page buffers
	 * are memory-heavy, so low-RAM / old devices are excluded — the feature stays off there rather
	 * than hanging the reader. (The app itself still supports Android 6; only colorize is gated.)
	 */
	fun isDeviceSupported(context: Context): Boolean {
		val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return true
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && am.isLowRamDevice) return false
		val mi = ActivityManager.MemoryInfo()
		am.getMemoryInfo(mi)
		return mi.totalMem >= 2_500L * 1024 * 1024 // ~2.5 GB total RAM floor
	}

	private fun toHex(bytes: ByteArray): String {
		val sb = StringBuilder(bytes.size * 2)
		for (b in bytes) {
			val v = b.toInt() and 0xFF
			sb.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
		}
		return sb.toString()
	}

	private val HEX = "0123456789abcdef".toCharArray()
}
