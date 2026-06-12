package com.nyora.hasan72341.filter.ui.model

import com.nyora.hasan72341.core.ui.widgets.ChipsView
import com.nyora.hasan72341.mihon.parsers.model.SortOrder

data class FilterHeaderModel(
	val chips: Collection<ChipsView.ChipModel>,
	val sortOrder: SortOrder?,
	val isFilterApplied: Boolean,
) {

	val textSummary: String
		get() = chips.mapNotNull { if (it.isChecked) it.title else null }.joinToString()
}
