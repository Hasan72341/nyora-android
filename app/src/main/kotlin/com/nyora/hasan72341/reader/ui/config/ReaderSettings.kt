package com.nyora.hasan72341.reader.ui.config

import android.graphics.Bitmap
import android.view.View
import androidx.annotation.CheckResult
import androidx.collection.scatterSetOf
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageRegionDecoder
import com.davemorrissey.labs.subscaleview.decoder.SkiaPooledImageRegionDecoder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import com.nyora.hasan72341.core.model.ZoomMode
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.ReaderBackground
import com.nyora.hasan72341.core.prefs.ReaderMode
import com.nyora.hasan72341.core.util.MediatorStateFlow
import com.nyora.hasan72341.core.util.ext.isLowRamDevice
import com.nyora.hasan72341.core.util.ext.processLifecycleScope
import com.nyora.hasan72341.reader.domain.ReaderColorFilter

data class ReaderSettings(
	val zoomMode: ZoomMode,
	val background: ReaderBackground,
	val colorFilter: ReaderColorFilter?,
	val isReaderOptimizationEnabled: Boolean,
	val bitmapConfig: Bitmap.Config,
	val isPagesNumbersEnabled: Boolean,
	val isPagesCropEnabledStandard: Boolean,
	val isPagesCropEnabledWebtoon: Boolean,
	val readerMode: ReaderMode,
	val isReaderControlAlwaysLTR: Boolean,
	val isAiTranslateEnabled: Boolean,
	val isAiAutoTranslate: Boolean,
	val isAiTranslateOffline: Boolean,
) {

	constructor(settings: AppSettings, colorFilterOverride: ReaderColorFilter?, mangaId: String = "") : this(
		zoomMode = settings.zoomMode,
		background = settings.readerBackground,
		colorFilter = colorFilterOverride?.takeUnless { it.isEmpty } ?: settings.readerColorFilter,
		isReaderOptimizationEnabled = settings.isReaderOptimizationEnabled,
		bitmapConfig = if (settings.is32BitColorsEnabled) {
			Bitmap.Config.ARGB_8888
		} else {
			Bitmap.Config.RGB_565
		},
		isPagesNumbersEnabled = settings.isPagesNumbersEnabled,
		isPagesCropEnabledStandard = settings.isPagesCropEnabled(ReaderMode.STANDARD),
		isPagesCropEnabledWebtoon = settings.isPagesCropEnabled(ReaderMode.WEBTOON),
		readerMode = settings.defaultReaderMode,
		isReaderControlAlwaysLTR = settings.isReaderControlAlwaysLTR,
		isAiTranslateEnabled = settings.isAiTranslateEnabled && settings.isAiTranslateEnabledForManga(mangaId),
		isAiAutoTranslate = settings.isAiAutoTranslate,
		isAiTranslateOffline = settings.isAiTranslateOffline,
	)

	fun applyBackground(view: View) {
		view.background = background.resolve(view.context)
		view.backgroundTintList = if (background.isLight(view.context)) {
			colorFilter?.getBackgroundTint()
		} else {
			null
		}
	}

	fun isPagesCropEnabled(isWebtoon: Boolean) = if (isWebtoon) {
		isPagesCropEnabledWebtoon
	} else {
		isPagesCropEnabledStandard
	}

	@CheckResult
	fun applyBitmapConfig(ssiv: SubsamplingScaleImageView): Boolean {
		val config = bitmapConfig
		return if (ssiv.regionDecoderFactory.bitmapConfig != config) {
			ssiv.regionDecoderFactory = if (ssiv.context.isLowRamDevice()) {
				SkiaImageRegionDecoder.Factory(config)
			} else {
				SkiaPooledImageRegionDecoder.Factory(config)
			}
			ssiv.bitmapDecoderFactory = SkiaImageDecoder.Factory(config)
			true
		} else {
			false
		}
	}

	class Producer @AssistedInject constructor(
		@Assisted private val mangaId: Flow<String>,
		private val settings: AppSettings,
		private val mangaDataRepository: MangaDataRepository,
	) : MediatorStateFlow<ReaderSettings>(ReaderSettings(settings, null)) {

		private val settingsKeys = scatterSetOf(
			AppSettings.KEY_ZOOM_MODE,
			AppSettings.KEY_PAGES_NUMBERS,
			AppSettings.KEY_READER_BACKGROUND,
			AppSettings.KEY_32BIT_COLOR,
			AppSettings.KEY_READER_OPTIMIZE,
			AppSettings.KEY_CF_CONTRAST,
			AppSettings.KEY_CF_BRIGHTNESS,
			AppSettings.KEY_CF_INVERTED,
			AppSettings.KEY_CF_GRAYSCALE,
			AppSettings.KEY_READER_CROP,
			AppSettings.KEY_READER_MODE,
			AppSettings.KEY_READER_CONTROL_LTR,
			AppSettings.KEY_AI_TRANSLATE_ENABLED,
			AppSettings.KEY_AI_AUTO_TRANSLATE,
			AppSettings.KEY_AI_TRANSLATE_OFFLINE,
		)
		private var job: Job? = null

		override fun onActive() {
			assert(job?.isActive != true)
			job?.cancel()
			job = processLifecycleScope.launch(Dispatchers.IO) {
				observeImpl()
			}
		}

		override fun onInactive() {
			job?.cancel()
			job = null
		}

		private suspend fun observeImpl() {
			combine(
				mangaId,
				mangaId.flatMapLatest { mangaDataRepository.observeColorFilter(it) },
				settings.observeChanges().filter { x -> x == null || x in settingsKeys }.onStart { emit(null) },
			) { id, mangaCf, settingsKey ->
				ReaderSettings(settings, mangaCf, id)
			}.collect {
				publishValue(it)
			}
		}

		@AssistedFactory
		interface Factory {

			fun create(mangaId: Flow<String>): Producer
		}
	}
}
