package com.nyora.hasan72341.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import com.nyora.hasan72341.mihon.parsers.model.MangaPage

object MangaPageParceler : Parceler<MangaPage> {
	override fun create(parcel: Parcel) = MangaPage(
		url = requireNotNull(parcel.readString()),
		headers = HashMap<String, String>().apply {
			repeat(parcel.readInt()) {
				val k = parcel.readString() ?: return@repeat
				put(k, parcel.readString().orEmpty())
			}
		},
		source = parcel.readString()?.let { com.nyora.hasan72341.core.model.MangaSource(it) },
	)

	override fun MangaPage.write(parcel: Parcel, flags: Int) {
		parcel.writeString(url)
		parcel.writeInt(headers.size)
		for ((k, v) in headers) {
			parcel.writeString(k)
			parcel.writeString(v)
		}
		parcel.writeString(source?.name)
	}
}

@Parcelize
@TypeParceler<MangaPage, MangaPageParceler>
class ParcelableMangaPage(val page: MangaPage) : Parcelable
