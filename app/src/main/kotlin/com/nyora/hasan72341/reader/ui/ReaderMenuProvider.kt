package com.nyora.hasan72341.reader.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import com.nyora.hasan72341.R

	class ReaderMenuProvider(
	private val viewModel: ReaderViewModel,
	private val onTranslatePage: () -> Unit,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_reader, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_ai_translate -> {
				onTranslatePage()
				true
			}

			R.id.action_info -> {
				// TODO
				true
			}

			else -> false
		}
	}
}
