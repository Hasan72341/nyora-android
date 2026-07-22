package com.nyora.hasan72341.reader.ui.pager.webtoon

import com.nyora.hasan72341.reader.domain.toChapterKey
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.nyora.hasan72341.ai.MangaTranslator
import com.nyora.hasan72341.ai.colorize.MangaColorizer
import com.nyora.hasan72341.core.exceptions.resolve.ExceptionResolver
import com.nyora.hasan72341.core.os.NetworkState
import com.nyora.hasan72341.databinding.ItemPageWebtoonBinding
import com.nyora.hasan72341.reader.domain.PageLoader
import com.nyora.hasan72341.reader.ui.config.ReaderSettings
import com.nyora.hasan72341.reader.ui.pager.BasePageHolder
import com.nyora.hasan72341.reader.ui.pager.vm.PageState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WebtoonHolder(
	owner: LifecycleOwner,
	binding: ItemPageWebtoonBinding,
	loader: PageLoader,
	readerSettingsProducer: ReaderSettings.Producer,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
	private val translator: MangaTranslator,
) : BasePageHolder<ItemPageWebtoonBinding>(
	binding = binding,
	loader = loader,
	readerSettingsProducer = readerSettingsProducer,
	networkState = networkState,
	exceptionResolver = exceptionResolver,
	lifecycleOwner = owner,
), ViewTreeObserver.OnDrawListener {

	override val ssiv = binding.ssiv

	private var scrollToRestore = 0
	private var currentUri: Uri? = null
	private var translationJob: kotlinx.coroutines.Job? = null
	private var colorizeJob: kotlinx.coroutines.Job? = null
	private var colorizedUri: Uri? = null

	init {
		bindingInfo.progressBar.setVisibilityAfterHide(View.GONE)
		binding.translationOverlay.setup(binding.ssiv)
		binding.ssiv.viewTreeObserver.addOnDrawListener(this)
	}

	override fun onDraw() {
		binding.translationOverlay.update()
	}

	override fun onStateChanged(state: PageState) {
		super.onStateChanged(state)
		val source = (state as? PageState.Shown)?.source
		when {
			source is ImageSource.Uri -> {
				currentUri = source.uri
				colorizedUri = null
				if (settings.isColorizeEnabled && settings.isColorizeAuto) {
					colorizePage()
				}
				if (settings.isAiTranslateEnabled && settings.isAiAutoTranslate) {
					translatePage()
				} else {
					binding.translationOverlay.setBlocks(emptyList())
				}
			}
			// Our own colorized bitmap is shown — keep currentUri (original file) and the overlay.
			source != null -> Unit
			else -> binding.translationOverlay.setBlocks(emptyList())
		}
	}

	override fun onConfigChanged(settings: ReaderSettings) {
		super.onConfigChanged(settings)
		val colorizeOn = settings.isColorizeEnabled && settings.isColorizeAuto
		if (!colorizeOn) {
			if (colorizedUri != null) {
				colorizeJob?.cancel()
				colorizedUri = null
				reloadImage()
			}
		} else if (colorizedUri == null && currentUri != null && viewModel.state.value is PageState.Shown) {
			colorizePage()
		}
	}

	override fun onReady() {
		binding.ssiv.colorFilter = settings.colorFilter?.toColorFilter()
		with(binding.ssiv) {
			scrollTo(
				when {
					scrollToRestore != 0 -> scrollToRestore
					itemView.top < 0 -> getScrollRange()
					else -> 0
				},
			)
			scrollToRestore = 0
		}
	}

	override fun translatePage() {
		val data = boundData ?: return
		translationJob?.cancel()
		translationJob = lifecycleScope.launch {
			val bitmap = withContext(Dispatchers.IO) { getBitmap() } ?: return@launch
			try {
				translator.translatePage(data.chapterId.toChapterKey(), data.index, bitmap).collect { blocks ->
					binding.translationOverlay.setBlocks(blocks)
				}
			} finally {
				if (!bitmap.isRecycled) {
					bitmap.recycle()
				}
			}
		}
	}

	override fun colorizePage() {
		val uri = currentUri ?: return
		if (uri == colorizedUri) return
		if (!MangaColorizer.isAvailable(context)) return
		colorizeJob?.cancel()
		colorizeJob = lifecycleScope.launch {
			val src = withContext(Dispatchers.IO) { getBitmap() } ?: return@launch
			try {
				if (!MangaColorizer.canColorize(src.width, src.height)) return@launch
				val colored = MangaColorizer.colorize(context.applicationContext, src)
				if (currentUri != uri) {
					colored.recycle()
					return@launch
				}
				colorizedUri = uri
				ssiv.setImage(ImageSource.bitmap(colored))
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				android.util.Log.e("MangaColorizer", "colorize failed", e)
			} finally {
				if (!src.isRecycled) {
					src.recycle()
				}
			}
		}
	}

	override fun onRecycled() {
		translationJob?.cancel()
		translationJob = null
		colorizeJob?.cancel()
		colorizeJob = null
		colorizedUri = null
		binding.translationOverlay.setBlocks(emptyList())
		super.onRecycled()
	}

	override fun onDetachedFromWindow() {
		translationJob?.cancel()
		translationJob = null
		colorizeJob?.cancel()
		colorizeJob = null
		super.onDetachedFromWindow()
	}

	private fun getBitmap(): Bitmap? {
		val uri = currentUri ?: run {
			android.util.Log.e("MangaTranslator", "WebtoonHolder: currentUri is null")
			return null
		}
		return try {
			if (uri.scheme == "file") {
				com.nyora.hasan72341.core.image.BitmapDecoderCompat.decode(File(uri.path!!))
			} else {
				android.util.Log.e("MangaTranslator", "WebtoonHolder: unsupported scheme ${uri.scheme}")
				null
			}
		} catch (e: Exception) {
			android.util.Log.e("MangaTranslator", "WebtoonHolder: decode error", e)
			null
		}
	}

	override fun onDestroy() {
		binding.ssiv.viewTreeObserver.removeOnDrawListener(this)
		super.onDestroy()
	}

	fun getScrollY() = binding.ssiv.getScroll()

	fun restoreScroll(scroll: Int) {
		if (binding.ssiv.isReady) {
			binding.ssiv.scrollTo(scroll)
		} else {
			scrollToRestore = scroll
		}
	}
}
