package com.nyora.hasan72341.list.ui.adapter

import android.view.View
import com.nyora.hasan72341.core.ui.widgets.TipView

interface MangaListListener : MangaDetailsClickListener, ListStateHolderListener, ListHeaderClickListener,
	TipView.OnButtonClickListener, QuickFilterClickListener {

	fun onFilterClick(view: View?)
}
