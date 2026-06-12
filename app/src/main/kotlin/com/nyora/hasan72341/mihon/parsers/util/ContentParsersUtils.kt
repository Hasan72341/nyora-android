@file:JvmName("ContentParsersUtils")

package com.nyora.hasan72341.mihon.parsers.util

import com.nyora.hasan72341.mihon.parsers.model.ContentChapter
import com.nyora.hasan72341.mihon.parsers.model.ContentListFilter
import kotlin.contracts.contract

fun ContentListFilter?.isNullOrEmpty(): Boolean {
	contract {
		returns(false) implies (this@isNullOrEmpty != null)
	}
	return this == null || this.isEmpty()
}

fun Collection<ContentChapter>.findById(chapterId: String): ContentChapter? = find { x ->
	x.id == chapterId
}
