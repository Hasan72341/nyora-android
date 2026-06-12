package com.nyora.hasan72341.list.ui.model

import com.nyora.hasan72341.core.ui.widgets.ChipsView
import com.nyora.hasan72341.list.ui.ListModelDiffCallback

data class QuickFilter(
	val items: List<ChipsView.ChipModel>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean = other is QuickFilter

	override fun getChangePayload(previousState: ListModel) = ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
}
