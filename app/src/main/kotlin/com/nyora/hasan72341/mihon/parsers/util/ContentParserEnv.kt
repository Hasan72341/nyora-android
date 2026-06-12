package com.nyora.hasan72341.mihon.parsers.util

import com.nyora.hasan72341.mihon.parsers.ContentParser
import com.nyora.hasan72341.mihon.parsers.ErrorMessages
import com.nyora.hasan72341.mihon.parsers.InternalParsersApi
import com.nyora.hasan72341.mihon.parsers.core.AbstractContentParser
import com.nyora.hasan72341.mihon.parsers.exception.ParseException
import com.nyora.hasan72341.mihon.parsers.model.Content
import com.nyora.hasan72341.mihon.parsers.model.ContentChapter
import com.nyora.hasan72341.mihon.parsers.model.ContentPage
import com.nyora.hasan72341.mihon.parsers.model.ContentRating
import com.nyora.hasan72341.mihon.parsers.model.ContentState
import com.nyora.hasan72341.mihon.parsers.model.ContentTag
import com.nyora.hasan72341.mihon.parsers.model.ContentType
import com.nyora.hasan72341.mihon.parsers.model.Demographic
import okhttp3.HttpUrl
import org.jsoup.nodes.Element


/**
 * Create a unique id for [Content]/[ContentChapter]/[ContentPage].
 * @param url must be relative url, without a domain
 * @see [Content.id]
 * @see [ContentChapter.id]
 * @see [ContentPage.id]
 */
@InternalParsersApi
public fun ContentParser.generateUid(url: String): Long {
	var h = LONG_HASH_SEED
	source.name.forEach { c ->
		h = 31 * h + c.code
	}
	url.forEach { c ->
		h = 31 * h + c.code
	}
	return h
}

/**
 * Create a unique id for [Content]/[ContentChapter]/[ContentPage].
 * @param id an internal identifier
 * @see [Content.id]
 * @see [ContentChapter.id]
 * @see [ContentPage.id]
 */
@InternalParsersApi
public fun ContentParser.generateUid(id: Long): Long {
	var h = LONG_HASH_SEED
	source.name.forEach { c ->
		h = 31 * h + c.code
	}
	h = 31 * h + id
	return h
}

@InternalParsersApi
public fun Element.parseFailed(message: String? = null): Nothing {
	throw ParseException(message, ownerDocument()?.location() ?: baseUri(), null)
}

@InternalParsersApi
public fun Set<ContentTag>?.oneOrThrowIfMany(): ContentTag? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_GENRES_NOT_SUPPORTED,
)

@InternalParsersApi
public fun Set<ContentState>?.oneOrThrowIfMany(): ContentState? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_STATES_NOT_SUPPORTED,
)

@InternalParsersApi
public fun Set<ContentType>?.oneOrThrowIfMany(): ContentType? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_CONTENT_TYPES_NOT_SUPPORTED,
)

@InternalParsersApi
public fun Set<Demographic>?.oneOrThrowIfMany(): Demographic? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_DEMOGRAPHICS_NOT_SUPPORTED,
)

@InternalParsersApi
public fun Set<ContentRating>?.oneOrThrowIfMany(): ContentRating? = oneOrThrowIfMany(
	ErrorMessages.FILTER_MULTIPLE_CONTENT_RATING_NOT_SUPPORTED,
)

private fun <T> Set<T>?.oneOrThrowIfMany(msg: String): T? = when {
	isNullOrEmpty() -> null
	size == 1 -> first()
	else -> throw IllegalArgumentException(msg)
}

@InternalParsersApi
public fun AbstractContentParser.getDomain(subdomain: String): String {
	val domain = domain
	return subdomain + "." + domain.removePrefix("www.")
}

@InternalParsersApi
public fun ContentParser.urlBuilder(subdomain: String? = null): HttpUrl.Builder {
	return HttpUrl.Builder()
		.scheme(SCHEME_HTTPS)
		.host(if (subdomain == null) domain else "$subdomain.$domain")
}

