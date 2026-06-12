package com.nyora.hasan72341.reader.ui.pager.reversed

import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.reader.ui.ReaderState
import com.nyora.hasan72341.reader.ui.pager.BasePagerReaderFragment
import com.nyora.hasan72341.reader.ui.pager.ReaderPage
import javax.inject.Inject

@AndroidEntryPoint
class ReversedReaderFragment : BasePagerReaderFragment() {

	@Inject
	lateinit var settings: AppSettings

	override fun onCreateAdvancedTransformer(): ViewPager2.PageTransformer = ReversedPageAnimTransformer()

	override fun onCreateAdapter() = ReversedPagesAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		readerSettingsProducer = viewModel.readerSettingsProducer,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
		translator = translator,
	)

	override fun onWheelScroll(axisValue: Float) {
		val value = if (settings.isReaderControlAlwaysLTR) -axisValue else axisValue
		super.onWheelScroll(value)
	}

	override fun switchPageBy(delta: Int) {
		super.switchPageBy(-delta)
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		super.switchPageTo(reversed(position), smooth)
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		super.onPagesChanged(pages.reversed(), pendingState)
	}

	override fun notifyPageChanged(page: Int) {
		val pos = reversed(page)
		viewModel.onCurrentPageChanged(pos, pos)
	}

	private fun reversed(position: Int): Int {
		return ((readerAdapter?.itemCount ?: 0) - position - 1).coerceAtLeast(0)
	}
}
