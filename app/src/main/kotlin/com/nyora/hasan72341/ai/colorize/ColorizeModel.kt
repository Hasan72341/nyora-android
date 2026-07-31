package com.nyora.hasan72341.ai.colorize

/**
 * Constants for the on-device manga colorizer model — the SAME model the Nyora web app ships
 * (see nyora-web/web/core/colorize/model.js), kept byte-for-byte identical so both platforms
 * produce the same colours.
 *
 * Model: manga-colorization-v2 (qweasdd, MIT) — a GAN trained on MANGA/anime art, exported to
 * ONNX (fp16) by Faridzar. Pinned to a COMMIT SHA (not a floating branch) so the weights can't be
 * swapped out from under installed apps, and the download is checked against [MODEL_SHA256] before
 * it is ever loaded.
 *
 * I/O (verified, matches the web worker): input `input` float32 [1,5,H,W] — channel 0 = grayscale
 * in [0,1], channels 1-4 = 0 (automatic colourisation, no colour hint); output `rgb` float32
 * [1,3,H,W] in 0..1. H and W must be multiples of 32.
 */
object ColorizeModel {

	const val MODEL_URL =
		"https://huggingface.co/Faridzar/manga-colorization-v2-onnx/resolve/" +
			"5515e06d31b08ffd107af686cba5e98e95e8d4cf/manga-colorize-fp16.onnx"

	/** Expected size, ~62 MB. Used for the progress bar when the server omits Content-Length. */
	const val MODEL_BYTES = 61_650_260L

	/** Hugging Face LFS oid = the file's SHA-256. Verified before the bytes reach the ONNX parser. */
	const val MODEL_SHA256 = "39660d0047ea6f1a0ddee6aa89054997f95ea566f4d56ff762f66dbcf1a1a7ef"

	/** On-disk name under filesDir/colorize/. */
	const val MODEL_FILE = "manga-colorize-fp16.onnx"

	/** ONNX graph input/output tensor names. */
	const val INPUT_NAME = "input"
	const val OUTPUT_NAME = "rgb"

	/**
	 * Trained working resolution (short side, portrait). Running the GAN far off this design point
	 * gains nothing and shifts colours, so we stay on it — identical to the web worker.
	 */
	const val SIZE = 576

	/** Chroma boost — the raw generator is duller than the author's published samples; 1.28x lands on them. */
	const val SATURATION = 1.28f

	/**
	 * Largest source page (in pixels) we'll colorize. The luminance-combine allocates several
	 * full-resolution int buffers, so a pathologically tall webtoon strip could OOM even on a
	 * capable device. Above this the page is left as-is (B&W) rather than risking a crash.
	 */
	const val MAX_PIXELS = 10_000_000L
}
