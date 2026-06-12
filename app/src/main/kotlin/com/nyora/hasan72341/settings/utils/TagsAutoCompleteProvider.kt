package com.nyora.hasan72341.settings.utils

import javax.inject.Inject
import com.nyora.hasan72341.core.db.MangaDatabase

class TagsAutoCompleteProvider @Inject constructor(
	private val db: MangaDatabase,
) : MultiAutoCompleteTextViewPreference.AutoCompleteProvider {

	override suspend fun getSuggestions(query: String): List<String> {
		return emptyList()
	}
}
