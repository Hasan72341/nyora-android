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
			// Nothing pre-selected: leaving it empty means "all languages" (see commit()).
			selectedItems = emptySet(),
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
			// No language is pre-selected. Leaving the selection empty enables every language
			// (commit() treats "none chosen" as "all"); the user can narrow it by picking languages.
			val availableLocales = localesGroups.keys
				.ifEmpty { DEFAULT_LOCALES }
				.sortedWithSafe(LocaleComparator())
			locales.value = locales.value.copy(
				availableItems = availableLocales,
				selectedItems = emptySet(),
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
		// Non-empty language codes only; empty selection => "all languages".
		val languages = locales.value.selectedItems.mapToSet { it.language }
			.filterTo(HashSet()) { it.isNotEmpty() }
		val types = types.value.selectedItems
		// Persist the choice so sources ADDED LATER (a pasted catalogue repo) honour it too, not just
		// the ones present now. Empty = all languages.
		settings.enabledSourceLanguages = languages
		val enabledSources = repository.allMangaSources.filterTo(HashSet()) { x ->
			x.contentTypeOrManga() in types && (languages.isEmpty() || x.localeCode() in languages)
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

		// Canonical language list for onboarding when no catalogue is loaded yet (the app ships
		// source-less). The choice is saved and applied to sources added later via a catalogue repo.
		private val DEFAULT_LOCALES: List<Locale> = listOf(
			"en", "ja", "ko", "zh", "es", "pt", "fr", "de", "ru", "id", "it", "ar", "tr", "vi", "th", "pl",
		).map { Locale.forLanguageTag(it) }
	}
}
