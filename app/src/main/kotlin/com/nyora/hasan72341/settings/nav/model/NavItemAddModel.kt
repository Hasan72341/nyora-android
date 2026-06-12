package com.nyora.hasan72341.settings.nav.model

import com.nyora.hasan72341.list.ui.model.ListModel

data class NavItemAddModel(
	val canAdd: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean = other is NavItemAddModel
}
