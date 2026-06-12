package com.nyora.hasan72341.widget.shelf

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService
import coil3.ImageLoader
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.favourites.domain.FavouritesRepository
import com.nyora.hasan72341.history.data.HistoryRepository
import javax.inject.Inject

@AndroidEntryPoint
class ShelfWidgetService : RemoteViewsService() {

	@Inject
	lateinit var favouritesRepository: FavouritesRepository

	@Inject
	lateinit var historyRepository: HistoryRepository

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var coilLazy: Lazy<ImageLoader>

	override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
		val widgetId = intent.getIntExtra(
			AppWidgetManager.EXTRA_APPWIDGET_ID,
			AppWidgetManager.INVALID_APPWIDGET_ID,
		)
		return ShelfListFactory(applicationContext, favouritesRepository, historyRepository, coilLazy, settings, widgetId)
	}
}
