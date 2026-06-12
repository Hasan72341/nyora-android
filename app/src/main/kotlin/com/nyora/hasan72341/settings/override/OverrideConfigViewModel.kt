package com.nyora.hasan72341.settings.override

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import com.nyora.hasan72341.core.model.parcelable.ParcelableManga
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.parser.MangaDataRepository
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.ui.model.MangaOverride
import com.nyora.hasan72341.core.util.MimeTypes
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.core.util.ext.isFileUri
import com.nyora.hasan72341.core.util.ext.openSource
import com.nyora.hasan72341.core.util.ext.require
import com.nyora.hasan72341.core.util.ext.toMimeTypeOrNull
import com.nyora.hasan72341.core.util.ext.toUriOrNull
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.md5
import java.io.File
import javax.inject.Inject

private const val DIR_COVERS = "covers"

@HiltViewModel
class OverrideConfigViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	@ApplicationContext private val context: Context,
	private val dataRepository: MangaDataRepository,
) : BaseViewModel() {

	private val manga = savedStateHandle.require<ParcelableManga>(AppRouter.KEY_MANGA).manga

	val data = MutableStateFlow<Pair<Manga, MangaOverride>?>(null)
	val onSaved = MutableEventFlow<Unit>()

	init {
		launchLoadingJob(Dispatchers.IO) {
			data.value = manga to (dataRepository.getOverride(manga.id) ?: emptyOverride())
		}
	}

	fun save(title: String?) {
		launchLoadingJob(Dispatchers.IO) {
			val override = checkNotNull(data.value).second.let {
				it.copy(
					title = title,
					coverUrl = it.coverUrl?.cachedFile(),
				)
			}
			dataRepository.setOverride(manga, override)
			onSaved.call(Unit)
		}
	}

	fun updateCover(coverUri: String?) {
		val snapshot = data.value ?: return
		data.value = snapshot.first to snapshot.second.copy(
			coverUrl = coverUri,
		)
	}

	private suspend fun String.cachedFile(): String {
		val uri = toUriOrNull()
		if (uri == null || uri.isFileUri()) {
			return this
		}
		val cacheDir = context.getExternalFilesDir(DIR_COVERS) ?: return this
		val cr = context.contentResolver
		val ext = cr.getType(uri)?.toMimeTypeOrNull()?.let {
			MimeTypes.getExtension(it)
		}
		val fileName = buildString {
			append(this@cachedFile.md5())
			if (!ext.isNullOrEmpty()) {
				append('.')
				append(ext)
			}
		}
		return withContext(Dispatchers.IO) {
			val dest = File(cacheDir, fileName)
			cr.openSource(uri).use { source ->
				dest.sink().buffer().use { sink ->
					sink.writeAll(source)
				}
			}
			dest
		}.toUri().toString()
	}

	private fun emptyOverride() = MangaOverride(null, null, null)
}
