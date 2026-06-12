package com.nyora.hasan72341.core.ui.list

import androidx.recyclerview.widget.RecyclerView

abstract class BaseListSelectionCallback(
	protected val recyclerView: RecyclerView,
) : ListSelectionController.Callback {

	override fun onSelectionChanged(controller: ListSelectionController, count: Int) {
		recyclerView.invalidateItemDecorations()
	}
}
