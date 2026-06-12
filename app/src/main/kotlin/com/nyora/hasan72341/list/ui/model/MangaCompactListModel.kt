package com.nyora.hasan72341.list.ui.model

import com.nyora.hasan72341.core.ui.model.MangaOverride
import com.nyora.hasan72341.mihon.parsers.model.Manga

data class MangaCompactListModel(
	override val manga: Manga,
	override val override: MangaOverride?,
	val subtitle: String,
	override val counter: Int,
) : MangaListModel()
