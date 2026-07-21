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
import com.nyora.hasan72341.core.exceptions.resolve.ExceptionResolver
import com.nyora.hasan72341.core.os.NetworkState
import com.nyora.hasan72341.databinding.ItemPageWebtoonBinding
import com.nyora.hasan72341.reader.domain.PageLoader
import com.nyora.hasan72341.reader.ui.config.ReaderSettings
import com.nyora.hasan72341.reader.ui.pager.BasePageHolder
import com.nyora.hasan72341.reader.ui.pager.vm.PageState
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
		if (state is PageState.Shown) {
			currentUri = (state.source as? ImageSource.Uri)?.uri
			if (settings.isAiTranslateEnabled && settings.isAiAutoTranslate) {
				translatePage()
			} else {
				binding.translationOverlay.setBlocks(emptyList())
			}
		} else {
			binding.translationOverlay.setBlocks(emptyList())
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

	override fun onRecycled() {
		translationJob?.cancel()
		translationJob = null
		binding.translationOverlay.setBlocks(emptyList())
		super.onRecycled()
	}

	override fun onDetachedFromWindow() {
		translationJob?.cancel()
		translationJob = null
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
