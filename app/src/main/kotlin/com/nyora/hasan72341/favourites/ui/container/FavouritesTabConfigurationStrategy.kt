package com.nyora.hasan72341.favourites.ui.container

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.ui.util.PopupMenuMediator

class FavouritesTabConfigurationStrategy(
	private val adapter: FavouritesContainerAdapter,
	private val viewModel: FavouritesContainerViewModel,
	private val router: AppRouter,
) : TabConfigurationStrategy {

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		val item = adapter.getItem(position)
		tab.text = when (item.id) {
			FavouritesContainerViewModel.ID_DOWNLOADS -> tab.view.context.getString(R.string.downloads)
			else -> item.title ?: tab.view.context.getString(R.string.all_favourites)
		}
		tab.tag = item
		if (item.id != FavouritesContainerViewModel.ID_DOWNLOADS) {
			PopupMenuMediator(
				FavouriteTabPopupMenuProvider(tab.view.context, router, viewModel, item.id)
			).attach(tab.view)
		}
	}
}
