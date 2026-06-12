package com.nyora.hasan72341.local.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.nav.router

class LocalListMenuProvider(
	private val fragment: Fragment,
	private val onImportClick: Function0<Unit>,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_local, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_filter)?.isVisible = fragment.router.isFilterSupported()
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_import -> {
				onImportClick()
				true
			}

			R.id.action_directories -> {
				fragment.router.openDirectoriesSettings()
				true
			}

			R.id.action_filter -> {
				fragment.router.showFilterSheet()
				true
			}

			else -> false
		}
	}
}
