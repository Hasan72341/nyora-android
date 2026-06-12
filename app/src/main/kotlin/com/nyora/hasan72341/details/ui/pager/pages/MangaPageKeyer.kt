package com.nyora.hasan72341.details.ui.pager.pages

import coil3.key.Keyer
import coil3.request.Options
import com.nyora.hasan72341.mihon.parsers.model.MangaPage

class MangaPageKeyer : Keyer<MangaPage> {

	override fun key(data: MangaPage, options: Options) = data.url
}
