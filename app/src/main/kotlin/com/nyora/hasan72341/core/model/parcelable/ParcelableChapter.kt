package com.nyora.hasan72341.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import com.nyora.hasan72341.mihon.parsers.model.MangaChapter

@Parcelize
data class ParcelableChapter(
	val chapter: MangaChapter,
) : Parcelable {

	companion object : Parceler<ParcelableChapter> {

		override fun create(parcel: Parcel) = ParcelableChapter(
			MangaChapter(
				id = requireNotNull(parcel.readString()),
				title = parcel.readString().orEmpty(),
				number = parcel.readFloat(),
				volume = parcel.readInt(),
				url = parcel.readString().orEmpty(),
				scanlator = parcel.readString(),
				uploadDate = parcel.readLong(),
				branch = parcel.readString(),
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
		}
	}
}
