package com.nyora.hasan72341.settings.sources.catalog

import com.nyora.hasan72341.mihon.parsers.model.ContentType

data class SourcesCatalogFilter(
	val types: Set<ContentType>,
	val locale: String?,
	val isNewOnly: Boolean,
)
