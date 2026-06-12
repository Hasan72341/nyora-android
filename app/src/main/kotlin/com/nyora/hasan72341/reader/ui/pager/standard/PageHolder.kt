package com.nyora.hasan72341.reader.ui.pager.standard

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.RoundedCorner
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.nyora.hasan72341.R
import com.nyora.hasan72341.ai.MangaTranslator
import com.nyora.hasan72341.core.exceptions.resolve.ExceptionResolver
import com.nyora.hasan72341.core.model.ZoomMode
import com.nyora.hasan72341.core.os.NetworkState
import com.nyora.hasan72341.core.prefs.ReaderMode
import com.nyora.hasan72341.core.ui.widgets.ZoomControl
import com.nyora.hasan72341.databinding.ItemPageBinding
import com.nyora.hasan72341.reader.domain.PageLoader
import com.nyora.hasan72341.reader.ui.config.ReaderSettings
import com.nyora.hasan72341.reader.ui.pager.BasePageHolder
import com.nyora.hasan72341.reader.ui.pager.ReaderPage
import com.nyora.hasan72341.reader.ui.pager.vm.PageState
import kotlinx.coroutines.launch
import java.io.File

open class PageHolder(
	owner: LifecycleOwner,
	binding: ItemPageBinding,
	loader: PageLoader,
	readerSettingsProducer: ReaderSettings.Producer,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
	private val translator: MangaTranslator,
) : BasePageHolder<ItemPageBinding>(
	binding = binding,
	loader = loader,
	readerSettingsProducer = readerSettingsProducer,
	networkState = networkState,
	exceptionResolver = exceptionResolver,
	lifecycleOwner = owner,
), ZoomControl.ZoomControlListener, OnApplyWindowInsetsListener, ViewTreeObserver.OnDrawListener {

	override val ssiv = binding.ssiv

	private var currentUri: Uri? = null
	private var translationJob: kotlinx.coroutines.Job? = null

	init {
		ViewCompat.setOnApplyWindowInsetsListener(binding.root, this)
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

	override fun translatePage() {
		val data = boundData ?: return
		val bitmap = getBitmap()
		if (bitmap == null) {
			return
		}
		
		translationJob?.cancel()
		translationJob = lifecycleScope.launch {
			translator.translatePage(data.chapterId, data.index, bitmap).collect { blocks ->
				binding.translationOverlay.setBlocks(blocks)
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
			android.util.Log.e("MangaTranslator", "currentUri is null for page ${bindingAdapterPosition}")
			return null
		}
		android.util.Log.d("MangaTranslator", "getBitmap() for URI: $uri")
		return try {
			if (uri.scheme == "file") {
				val file = File(uri.path!!)
				if (file.exists()) {
					com.nyora.hasan72341.core.image.BitmapDecoderCompat.decode(file)
				} else {
					android.util.Log.e("MangaTranslator", "File does not exist: ${file.absolutePath}")
					null
				}
			} else {
				android.util.Log.e("MangaTranslator", "Unsupported scheme: ${uri.scheme}")
				null
			}
		} catch (e: Exception) {
			android.util.Log.e("MangaTranslator", "Error decoding bitmap", e)
			null
		}
	}

	override fun onDestroy() {
		binding.ssiv.viewTreeObserver.removeOnDrawListener(this)
		super.onDestroy()
	}

	@SuppressLint("SetTextI18n")
	override fun onBind(data: ReaderPage) {
		super.onBind(data)
		binding.textViewNumber.text = (data.index + 1).toString()
	}

	override fun onReady() {
		binding.ssiv.colorFilter = settings.colorFilter?.toColorFilter()
	}

	override fun onZoomIn() {
		scaleBy(1.2f)
	}

	override fun onZoomOut() {
		scaleBy(0.8f)
	}

	@RequiresApi(Build.VERSION_CODES.S)
	private fun updateRoundedCorners(insets: WindowInsets) {
		val corner = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)
		binding.textViewNumber.updateLayoutParams<FrameLayout.LayoutParams> {
			val baseMargin = binding.root.resources.getDimensionPixelOffset(R.dimen.margin_small)
			gravity = when {
				settings.isReaderControlAlwaysLTR -> {
					Gravity.END or Gravity.BOTTOM
				}

				settings.readerMode == ReaderMode.REVERSED -> {
					Gravity.START or Gravity.BOTTOM
				}

				else -> {
					Gravity.END or Gravity.BOTTOM
				}
			}
			setMargins(baseMargin + (corner?.radius ?: 0))
		}
	}

	private fun scaleBy(factor: Float) {
		val ssiv = binding.ssiv
		val center = ssiv.getCenter() ?: return
		val newScale = ssiv.scale * factor
		ssiv.animateScaleAndCenter(newScale, center)?.apply {
			withDuration(ssiv.resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
			withInterpolator(DecelerateInterpolator())
			start()
		}
	}

	override fun onApplyWindowInsets(
		v: View,
		insets: WindowInsetsCompat
	): WindowInsetsCompat {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			insets.toWindowInsets()?.let { updateRoundedCorners(it) }
		}
		return insets
	}
}
