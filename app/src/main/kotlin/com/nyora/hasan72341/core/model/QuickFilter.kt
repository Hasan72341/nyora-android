package com.nyora.hasan72341.core.model

import com.nyora.hasan72341.core.ui.widgets.ChipsView
import com.nyora.hasan72341.list.domain.ListFilterOption

fun ListFilterOption.toChipModel(isChecked: Boolean) = ChipsView.ChipModel(
	title = titleText,
	titleResId = titleResId,
	icon = iconResId,
	iconData = getIconData(),
	isChecked = isChecked,
	counter = if (this is ListFilterOption.Branch) chaptersCount else 0,
	data = this,
)
