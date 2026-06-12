package com.nyora.hasan72341.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import com.nyora.hasan72341.core.model.MangaSource
import com.nyora.hasan72341.core.model.toMangaSourceRef
import com.nyora.hasan72341.core.util.ext.readParcelableCompat
import com.nyora.hasan72341.core.util.ext.readSerializableCompat
import com.nyora.hasan72341.core.util.ext.readStringSet
import com.nyora.hasan72341.core.util.ext.writeStringSet
import com.nyora.hasan72341.mihon.parsers.model.Manga

@Parcelize
data class ParcelableManga(
	val manga: Manga,
	private val withDescription: Boolean = true,
) : Parcelable {

	companion object : Parceler<ParcelableManga> {

		override fun ParcelableManga.write(parcel: Parcel, flags: Int) = with(manga) {
			parcel.writeString(id)
			parcel.writeString(title)
			parcel.writeStringSet(altTitles.toSet())
			parcel.writeString(url)
			parcel.writeString(publicUrl)
			parcel.writeFloat(rating)
			parcel.writeSerializable(contentRating)
			parcel.writeString(coverUrl)
			parcel.writeString(largeCoverUrl)
			parcel.writeString(description.takeIf { withDescription })
			parcel.writeParcelable(ParcelableMangaTags(tags), flags)
			parcel.writeSerializable(state)
			parcel.writeStringSet(authors.toSet())
			parcel.writeString(source.name)
		}

		override fun create(parcel: Parcel) = ParcelableManga(
			Manga(
				id = requireNotNull(parcel.readString()),
				title = requireNotNull(parcel.readString()),
				altTitles = parcel.readStringSet().toList(),
				url = requireNotNull(parcel.readString()),
				publicUrl = requireNotNull(parcel.readString()),
				rating = parcel.readFloat(),
				contentRating = parcel.readSerializableCompat(),
				coverUrl = parcel.readString().orEmpty(),
				largeCoverUrl = parcel.readString(),
				description = parcel.readString().orEmpty(),
				tags = requireNotNull(parcel.readParcelableCompat<ParcelableMangaTags>()).tags,
				state = parcel.readSerializableCompat(),
				authors = parcel.readStringSet().toList(),
				chapters = emptyList(),
				source = parcel.readString().toMangaSourceRef(),
			),
			withDescription = true,
		)
	}
}
