package com.nyora.hasan72341.mihon.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import com.nyora.hasan72341.mihon.model.contentSource
import com.nyora.hasan72341.mihon.parsers.model.ContentPage
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

object ContentPageParceler : Parceler<ContentPage> {
	override fun create(parcel: Parcel) = ContentPage(
		id = requireNotNull(parcel.readString()),
		url = requireNotNull(parcel.readString()),
		preview = parcel.readString(),
		source = contentSource(parcel.readString()),
	)

	override fun ContentPage.write(parcel: Parcel, flags: Int) {
		parcel.writeString(id)
		parcel.writeString(url)
		parcel.writeString(preview)
		parcel.writeString(source.name)
	}
}

@Parcelize
@TypeParceler<ContentPage, ContentPageParceler>
class ParcelableContentPage(val page: ContentPage) : Parcelable

