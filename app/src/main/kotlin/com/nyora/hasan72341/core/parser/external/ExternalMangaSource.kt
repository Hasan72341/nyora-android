package com.nyora.hasan72341.core.parser.external

import android.content.Context
import com.nyora.hasan72341.mihon.parsers.model.MangaSource

data class ExternalMangaSource(
	val packageName: String,
	val authority: String,
) : MangaSource {

	override val name: String
		get() = "content:$packageName/$authority"

	private var cachedName: String? = null

	fun isAvailable(context: Context): Boolean {
		return context.packageManager.resolveContentProvider(authority, 0)?.isEnabled == true
	}

	fun resolveName(context: Context): String {
		cachedName?.let {
			return it
		}
		val pm = context.packageManager
		val info = pm.resolveContentProvider(authority, 0)
		return info?.loadLabel(pm)?.toString()?.also {
			cachedName = it
		} ?: authority
	}
}
