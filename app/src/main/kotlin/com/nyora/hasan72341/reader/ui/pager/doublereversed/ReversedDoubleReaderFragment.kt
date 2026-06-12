package com.nyora.hasan72341.reader.ui.pager.doublereversed

import com.nyora.hasan72341.reader.ui.ReaderState
import com.nyora.hasan72341.reader.ui.pager.ReaderPage
import com.nyora.hasan72341.reader.ui.pager.doublepage.DoubleReaderFragment

class ReversedDoubleReaderFragment : DoubleReaderFragment() {

	override fun switchPageBy(delta: Int) {
		super.switchPageBy(-delta)
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		super.switchPageTo(reversed(position), smooth)
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		super.onPagesChanged(pages.reversed(), pendingState)
	}

	override fun notifyPageChanged(lowerPos: Int, upperPos: Int) {
		viewModel.onCurrentPageChanged(reversed(upperPos), reversed(lowerPos))
	}

	private fun reversed(position: Int): Int {
		return ((readerAdapter?.itemCount ?: 0) - position - 1).coerceAtLeast(0)
	}
}
