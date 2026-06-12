package com.nyora.hasan72341.reader.ui

import com.nyora.hasan72341.reader.ui.pager.ReaderPage

data class ReaderContent(
	val pages: List<ReaderPage>,
	val state: ReaderState?
)