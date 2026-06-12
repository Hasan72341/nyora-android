package com.nyora.hasan72341.core.ui.util

import androidx.core.view.MenuHost
import kotlinx.coroutines.flow.FlowCollector

class MenuInvalidator(
	private val host: MenuHost,
) : FlowCollector<Any?> {

	override suspend fun emit(value: Any?) = host.invalidateMenu()
}
