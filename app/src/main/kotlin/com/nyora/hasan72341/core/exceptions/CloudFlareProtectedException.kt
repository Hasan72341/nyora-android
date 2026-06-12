package com.nyora.hasan72341.core.exceptions

import okhttp3.Headers
import com.nyora.hasan72341.core.model.UnknownMangaSource
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.network.CloudFlareHelper

class CloudFlareProtectedException(
	override val url: String,
	source: MangaSource?,
	@Transient val headers: Headers,
) : CloudFlareException("Protected by CloudFlare", CloudFlareHelper.PROTECTION_CAPTCHA) {

	override val source: MangaSource = source ?: UnknownMangaSource
}
