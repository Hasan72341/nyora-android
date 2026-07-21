package com.nyora.hasan72341.main.ui.welcome

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import com.nyora.hasan72341.core.LocalizedAppContext
import com.nyora.hasan72341.core.model.contentTypeOrManga
import com.nyora.hasan72341.core.model.isHentai
import com.nyora.hasan72341.core.model.localeCode
import com.nyora.hasan72341.core.parser.datadriven.DataDrivenCatalogueRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.util.LocaleComparator
import com.nyora.hasan72341.core.util.ext.mapSortedByCount
import com.nyora.hasan72341.core.util.ext.sortedWithSafe
import com.nyora.hasan72341.core.util.ext.toLocale
import com.nyora.hasan72341.explore.data.MangaSourcesRepository
import com.nyora.hasan72341.filter.ui.model.FilterProperty
import com.nyora.hasan72341.mihon.parsers.model.ContentType
import com.nyora.hasan72341.mihon.parsers.util.mapToSet
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
	private val repository: MangaSourcesRepository,
	private val catalogue: DataDrivenCatalogueRepository,
	private val settings: AppSettings,
	@LocalizedAppContext context: Context,
) : BaseViewModel() {

	private var updateJob: Job

	val locales = MutableStateFlow(
		FilterProperty<Locale>(
			availableItems = listOf(Locale.ROOT),
			selectedItems = setOf(Locale.ROOT),
			isLoading = true,
			error = null,
		),
	)

	val types = MutableStateFlow(
		FilterProperty(
			availableItems = listOf(ContentType.MANGA),
			selectedItems = setOf(ContentType.MANGA),
			isLoading = true,
			error = null,
		),
	)

	init {
		updateJob = launchJob(Dispatchers.IO) {
			// Sources are the runtime data-driven catalogue, which may not have loaded yet on a fresh
			// install. Without this the locale/content-type chips derive from an empty set and render
			// blank, so fetch the catalogue before computing the options.
			if (repository.allMangaSources.isEmpty()) {
				catalogue.refresh()
				repository.assimilateFromCatalogue()
			}
			val allSources = repository.allMangaSources
			val localesGroups = allSources.groupBy { it.localeCode().toLocale() }

			val contentTypes = allSources.mapSortedByCount { it.contentTypeOrManga() }
				.ifEmpty { DEFAULT_CONTENT_TYPES }
			types.value = types.value.copy(
				availableItems = contentTypes,
				isLoading = false,
			)
			// Default to ALL languages so every (non-NSFW) source is installed by
			// default; the user can narrow the selection during onboarding.
			val selectedLocales = HashSet(localesGroups.keys).apply { add(Locale.ROOT) }
			locales.value = locales.value.copy(
				availableItems = localesGroups.keys.sortedWithSafe(LocaleComparator()),
				selectedItems = selectedLocales,
				isLoading = false,
			)
			repository.clearNewSourcesBadge()
			commit()
		}
	}

	fun setLocaleChecked(locale: Locale, isChecked: Boolean) {
		val snapshot = locales.value
		locales.value = snapshot.copy(
			selectedItems = if (isChecked) {
				snapshot.selectedItems + locale
			} else {
				snapshot.selectedItems - locale
			},
		)
		val prevJob = updateJob
		updateJob = launchJob(Dispatchers.IO) {
			prevJob.join()
			commit()
		}
	}

	fun setTypeChecked(type: ContentType, isChecked: Boolean) {
		val snapshot = types.value
		val newSelectedItems = if (isChecked) {
			val isHentai = type.isHentai()
			settings.isNsfwContentDisabled = !isHentai
			// Mutual exclusion: if checking Hentai, uncheck non-Hentai. If checking Manga, uncheck Hentai.
			setOf(type)
		} else {
			snapshot.selectedItems - type
		}

		types.value = snapshot.copy(selectedItems = newSelectedItems)
		val prevJob = updateJob
		updateJob = launchJob(Dispatchers.IO) {
			prevJob.join()
			commit()
		}
	}

	private suspend fun commit() {
		val languages = locales.value.selectedItems.mapToSet { it.language }
		val types = types.value.selectedItems
		val enabledSources = repository.allMangaSources.filterTo(HashSet()) { x ->
			x.contentTypeOrManga() in types && x.localeCode() in languages
		}
		repository.setSourcesEnabledExclusive(enabledSources)
	}

	private companion object {
		// Canonical fallback so onboarding still offers content types if the catalogue can't be
		// fetched (e.g. offline on a fresh install); once sources load, the real set replaces it.
		private val DEFAULT_CONTENT_TYPES = listOf(
			ContentType.MANGA,
			ContentType.MANHWA,
			ContentType.MANHUA,
			ContentType.COMICS,
			ContentType.NOVEL,
			ContentType.HENTAI_MANGA,
		)
	}
}
