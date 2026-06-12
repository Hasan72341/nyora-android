package com.nyora.hasan72341.mihon.parsers.core

import com.nyora.hasan72341.mihon.parsers.ContentLoaderContext
import com.nyora.hasan72341.mihon.parsers.InternalParsersApi
import com.nyora.hasan72341.mihon.parsers.model.Content
import com.nyora.hasan72341.mihon.parsers.model.ContentListFilter
import com.nyora.hasan72341.mihon.parsers.model.ContentSource
import com.nyora.hasan72341.mihon.parsers.model.SortOrder

@InternalParsersApi
public abstract class SinglePageContentParser(
	context: ContentLoaderContext,
	source: ContentSource,
) : AbstractContentParser(context, source) {

	final override suspend fun getList(offset: Int, order: SortOrder, filter: ContentListFilter): List<Content> {
		if (offset > 0) {
			return emptyList()
		}
		return getList(order, filter)
	}

	public abstract suspend fun getList(order: SortOrder, filter: ContentListFilter): List<Content>
}

