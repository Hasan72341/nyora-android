package com.nyora.hasan72341.settings.nav.model

import androidx.annotation.StringRes
import com.nyora.hasan72341.core.prefs.NavItem
import com.nyora.hasan72341.list.ui.model.ListModel

data class NavItemConfigModel(
	val item: NavItem,
	@StringRes val disabledHintResId: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is NavItemConfigModel && other.item == item
	}
}
