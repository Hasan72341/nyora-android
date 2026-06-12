package com.nyora.hasan72341.core.nav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.nyora.hasan72341.core.model.parcelable.ParcelableManga
import com.nyora.hasan72341.core.nav.AppRouter.Companion.KEY_ID
import com.nyora.hasan72341.core.nav.AppRouter.Companion.KEY_MANGA
import com.nyora.hasan72341.core.util.ext.getParcelableCompat
import com.nyora.hasan72341.core.util.ext.getParcelableExtraCompat
import com.nyora.hasan72341.mihon.parsers.model.Manga
class MangaIntent private constructor(
	@JvmField val manga: Manga?,
	@JvmField val id: String?,
	@JvmField val uri: Uri?,
) {

	constructor(intent: Intent?) : this(
		manga = intent?.getParcelableExtraCompat<ParcelableManga>(KEY_MANGA)?.manga,
		id = intent?.getStringExtra(KEY_ID),
		uri = intent?.data,
	)

	constructor(savedStateHandle: SavedStateHandle) : this(
		manga = savedStateHandle.get<ParcelableManga>(KEY_MANGA)?.manga,
		id = savedStateHandle[KEY_ID],
		uri = savedStateHandle[AppRouter.KEY_DATA],
	)

	constructor(args: Bundle?) : this(
		manga = args?.getParcelableCompat<ParcelableManga>(KEY_MANGA)?.manga,
		id = args?.getString(KEY_ID),
		uri = null,
	)

	val mangaId: String?
		get() = id ?: manga?.id ?: uri?.getQueryParameter("id")

	companion object {

		fun of(manga: Manga) = MangaIntent(manga, manga.id, null)
	}
}
