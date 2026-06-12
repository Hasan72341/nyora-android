package com.nyora.hasan72341.mihon.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import com.nyora.hasan72341.mihon.model.contentSource
import com.nyora.hasan72341.mihon.parsers.model.ContentChapter
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelableChapter(
	val chapter: ContentChapter,
) : Parcelable {

	companion object : Parceler<ParcelableChapter> {

		override fun create(parcel: Parcel) = ParcelableChapter(
			ContentChapter(
				id = parcel.readString().orEmpty(),
				title = parcel.readString().orEmpty(),
				number = parcel.readFloat(),
				volume = parcel.readInt(),
				url = parcel.readString().orEmpty(),
				scanlator = parcel.readString(),
				uploadDate = parcel.readLong(),
				branch = parcel.readString(),
				source = contentSource(parcel.readString()),
			),
		)

		override fun ParcelableChapter.write(parcel: Parcel, flags: Int) = with(chapter) {
			parcel.writeString(id)
			parcel.writeString(title)
			parcel.writeFloat(number)
			parcel.writeInt(volume)
			parcel.writeString(url)
			parcel.writeString(scanlator)
			parcel.writeLong(uploadDate)
			parcel.writeString(branch)
			parcel.writeString(source.name)
		}
	}
}

