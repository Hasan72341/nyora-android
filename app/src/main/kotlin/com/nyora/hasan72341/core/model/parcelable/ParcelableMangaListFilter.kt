package com.nyora.hasan72341.core.model.parcelable

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import com.nyora.hasan72341.core.util.ext.readEnumSet
import com.nyora.hasan72341.core.util.ext.readParcelableCompat
import com.nyora.hasan72341.core.util.ext.readSerializableCompat
import com.nyora.hasan72341.core.util.ext.writeEnumSet
import com.nyora.hasan72341.mihon.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.ContentType
import org.koitharu.kotatsu.parsers.model.Demographic
import org.koitharu.kotatsu.parsers.model.MangaState
import com.nyora.hasan72341.mihon.parsers.model.MangaTag as NyoraMangaTag
import org.koitharu.kotatsu.parsers.model.MangaTag as LibMangaTag

object MangaListFilterParceler : Parceler<MangaListFilter> {

	// MangaListFilter (typealias to the lib filter) holds LIB MangaTag, while
	// ParcelableMangaTags stores NATIVE MangaTag. Convert at the boundary.
	private fun LibMangaTag.toNyora() = NyoraMangaTag(key = key, title = title)

	private fun NyoraMangaTag.toLib() = LibMangaTag(
		title = title,
		key = key,
		source = com.nyora.hasan72341.core.model.UnknownMangaSource,
	)

	override fun MangaListFilter.write(parcel: Parcel, flags: Int) {
		parcel.writeString(query)
		parcel.writeParcelable(ParcelableMangaTags(tags.map { it.toNyora() }), 0)
		parcel.writeParcelable(ParcelableMangaTags(tagsExclude.map { it.toNyora() }), 0)
		parcel.writeSerializable(locale)
		parcel.writeSerializable(originalLocale)
		parcel.writeEnumSet(states)
		parcel.writeEnumSet(contentRating)
		parcel.writeEnumSet(types)
		parcel.writeEnumSet(demographics)
		parcel.writeInt(year)
		parcel.writeInt(yearFrom)
		parcel.writeInt(yearTo)
		parcel.writeString(author)
	}

	override fun create(parcel: Parcel) = MangaListFilter(
		query = parcel.readString(),
		tags = parcel.readParcelableCompat<ParcelableMangaTags>()?.tags?.map { it.toLib() }?.toSet().orEmpty(),
		tagsExclude = parcel.readParcelableCompat<ParcelableMangaTags>()?.tags?.map { it.toLib() }?.toSet().orEmpty(),
		locale = parcel.readSerializableCompat(),
		originalLocale = parcel.readSerializableCompat(),
		states = parcel.readEnumSet<MangaState>().orEmpty(),
		contentRating = parcel.readEnumSet<ContentRating>().orEmpty(),
		types = parcel.readEnumSet<ContentType>().orEmpty(),
		demographics = parcel.readEnumSet<Demographic>().orEmpty(),
		year = parcel.readInt(),
		yearFrom = parcel.readInt(),
		yearTo = parcel.readInt(),
		author = parcel.readString(),
	)
}

@Parcelize
@TypeParceler<MangaListFilter, MangaListFilterParceler>
data class ParcelableMangaListFilter(val filter: MangaListFilter) : Parcelable
