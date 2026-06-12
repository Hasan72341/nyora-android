package com.nyora.hasan72341.core.exceptions

import com.nyora.hasan72341.core.model.UnknownMangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.network.CloudFlareHelper

class CloudFlareBlockedException(
	override val url: String,
	source: MangaSource?,
) : CloudFlareException("Blocked by CloudFlare", CloudFlareHelper.PROTECTION_BLOCKED) {

	override val source: MangaSource = source ?: UnknownMangaSource
}
