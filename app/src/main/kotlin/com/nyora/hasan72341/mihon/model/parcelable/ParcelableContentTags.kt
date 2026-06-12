package com.nyora.hasan72341.mihon.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import com.nyora.hasan72341.mihon.model.contentSource
import com.nyora.hasan72341.mihon.parsers.model.ContentTag
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

object ContentTagParceler : Parceler<ContentTag> {
	override fun create(parcel: Parcel) = ContentTag(
		title = requireNotNull(parcel.readString()),
		key = requireNotNull(parcel.readString()),
		source = contentSource(parcel.readString()),
	)

	override fun ContentTag.write(parcel: Parcel, flags: Int) {
		parcel.writeString(title)
		parcel.writeString(key)
		parcel.writeString(source.name)
	}
}

@Parcelize
@TypeParceler<ContentTag, ContentTagParceler>
data class ParcelableContentTags(val tags: Set<ContentTag>) : Parcelable

