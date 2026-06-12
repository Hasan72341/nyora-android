package com.nyora.hasan72341.reader.ui.pager.reversed

import android.graphics.PointF
import android.view.Gravity
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.nyora.hasan72341.ai.MangaTranslator
import com.nyora.hasan72341.core.exceptions.resolve.ExceptionResolver
import com.nyora.hasan72341.core.model.ZoomMode
import com.nyora.hasan72341.core.os.NetworkState
import com.nyora.hasan72341.databinding.ItemPageBinding
import com.nyora.hasan72341.reader.domain.PageLoader
import com.nyora.hasan72341.reader.ui.config.ReaderSettings
import com.nyora.hasan72341.reader.ui.pager.standard.PageHolder

class ReversedPageHolder(
	owner: LifecycleOwner,
	binding: ItemPageBinding,
	loader: PageLoader,
	readerSettingsProducer: ReaderSettings.Producer,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
	translator: MangaTranslator,
) : PageHolder(
	owner = owner,
	binding = binding,
	loader = loader,
	readerSettingsProducer = readerSettingsProducer,
	networkState = networkState,
	exceptionResolver = exceptionResolver,
	translator = translator,
) {

	init {
		(binding.textViewNumber.layoutParams as FrameLayout.LayoutParams)
			.gravity = Gravity.START or Gravity.BOTTOM
	}

	override fun onReady() {
		with(binding.ssiv) {
			maxScale = 2f * maxOf(
				width / sWidth.toFloat(),
				height / sHeight.toFloat(),
			)
			binding.ssiv.colorFilter = settings.colorFilter?.toColorFilter()
			when (settings.zoomMode) {
				ZoomMode.FIT_CENTER -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
					resetScaleAndCenter()
				}

				ZoomMode.FIT_HEIGHT -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
					minScale = height / sHeight.toFloat()
					setScaleAndCenter(
						minScale,
						PointF(sWidth.toFloat(), sHeight / 2f),
					)
				}

				ZoomMode.FIT_WIDTH -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CUSTOM
					minScale = width / sWidth.toFloat()
					setScaleAndCenter(
						minScale,
						PointF(sWidth / 2f, 0f),
					)
				}

				ZoomMode.KEEP_START -> {
					minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
					setScaleAndCenter(
						maxScale,
						PointF(sWidth.toFloat(), 0f),
					)
				}
			}
		}
	}
}
