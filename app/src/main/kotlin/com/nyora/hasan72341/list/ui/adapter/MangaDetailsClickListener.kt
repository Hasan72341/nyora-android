package com.nyora.hasan72341.list.ui.adapter

import android.view.View
import com.nyora.hasan72341.core.ui.list.OnListItemClickListener
import com.nyora.hasan72341.list.ui.model.MangaListModel
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.model.MangaTag

interface MangaDetailsClickListener : OnListItemClickListener<MangaListModel> {

	fun onReadClick(manga: Manga, view: View)

	fun onTagClick(manga: Manga, tag: MangaTag, view: View)
}
