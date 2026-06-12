package com.nyora.hasan72341.mihon.parsers.core

import androidx.annotation.VisibleForTesting
import com.nyora.hasan72341.mihon.parsers.ContentLoaderContext
import com.nyora.hasan72341.mihon.parsers.InternalParsersApi
import com.nyora.hasan72341.mihon.parsers.model.Content
import com.nyora.hasan72341.mihon.parsers.model.ContentSource
import com.nyora.hasan72341.mihon.parsers.model.search.ContentSearchQuery
import com.nyora.hasan72341.mihon.parsers.model.search.SearchableField
import com.nyora.hasan72341.mihon.parsers.util.Paginator

@OptIn(InternalParsersApi::class)
@Deprecated("Too complex. Use PagedContentParser instead")
internal abstract class FlexiblePagedContentParser(
	context: ContentLoaderContext,
	source: ContentSource,
	@get:VisibleForTesting(otherwise = VisibleForTesting.PROTECTED) @JvmField public val pageSize: Int,
	searchPageSize: Int = pageSize,
) : AbstractContentParser(context, source) {

	@JvmField
	protected val paginator: Paginator = Paginator(pageSize)

	@JvmField
	protected val searchPaginator: Paginator = Paginator(searchPageSize)

	final override suspend fun getList(query: ContentSearchQuery): List<Content> {
		var containTitleNameCriteria = false
		query.criteria.forEach {
			if (it.field == SearchableField.TITLE_NAME) {
				containTitleNameCriteria = true
			}
		}

		return searchContent(
			paginator = if (containTitleNameCriteria) {
				paginator
			} else {
				searchPaginator
			},
			query = query,
		)
	}

	public abstract suspend fun getListPage(query: ContentSearchQuery, page: Int): List<Content>

	protected fun setFirstPage(firstPage: Int, firstPageForSearch: Int = firstPage) {
		paginator.firstPage = firstPage
		searchPaginator.firstPage = firstPageForSearch
	}

	private suspend fun searchContent(
		paginator: Paginator,
		query: ContentSearchQuery,
	): List<Content> {
		val offset: Int = query.offset
		val page = paginator.getPage(offset)
		val list = getListPage(query, page)
		paginator.onListReceived(offset, page, list.size)
		return list
	}
}

