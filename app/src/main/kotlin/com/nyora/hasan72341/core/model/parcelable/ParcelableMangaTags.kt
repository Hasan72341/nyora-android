package com.nyora.hasan72341.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import com.nyora.hasan72341.mihon.parsers.model.MangaTag

object MangaTagParceler : Parceler<MangaTag> {
	override fun create(parcel: Parcel) = MangaTag(
		title = requireNotNull(parcel.readString()),
		key = requireNotNull(parcel.readString()),
	)

	override fun MangaTag.write(parcel: Parcel, flags: Int) {
		parcel.writeString(title)
		parcel.writeString(key)
	}
}

@Parcelize
@TypeParceler<MangaTag, MangaTagParceler>
data class ParcelableMangaTags(val tags: List<MangaTag>) : Parcelable
