package com.nyora.hasan72341.reader.ui.config

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.parser.ParserMangaRepository
import com.nyora.hasan72341.mihon.parsers.config.ConfigKey
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.util.mapToArray
import com.nyora.hasan72341.mihon.parsers.util.suspendlazy.getOrNull
import com.nyora.hasan72341.mihon.parsers.util.suspendlazy.suspendLazy
import kotlin.coroutines.resume

class ImageServerDelegate(
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val mangaSource: MangaSource?,
) {

	private val repositoryLazy = suspendLazy {
		mangaRepositoryFactory.create(checkNotNull(mangaSource)) as ParserMangaRepository
	}

	suspend fun isAvailable() = withContext(Dispatchers.IO) {
		repositoryLazy.getOrNull()?.let { repository ->
			repository.getConfigKeys().any { it is ConfigKey.PreferredImageServer }
		} == true
	}

	suspend fun getValue(): String? = withContext(Dispatchers.IO) {
		repositoryLazy.getOrNull()?.let { repository ->
			val key = repository.getConfigKeys().firstNotNullOfOrNull { it as? ConfigKey.PreferredImageServer }
			if (key != null) {
				key.presetValues[repository.getConfig().get(key)]
			} else {
				null
			}
		}
	}

	suspend fun showDialog(context: Context): Boolean {
		val repository = withContext(Dispatchers.IO) {
			repositoryLazy.getOrNull()
		} ?: return false
		val key = repository.getConfigKeys().firstNotNullOfOrNull {
			it as? ConfigKey.PreferredImageServer
		} ?: return false
		val entries = key.presetValues.values.mapToArray {
			it ?: context.getString(R.string.automatic)
		}
		val entryValues = key.presetValues.keys.toTypedArray()
		val config = repository.getConfig()
		val initialValue = config.get(key)
		var currentValue = initialValue
		val changed = suspendCancellableCoroutine { cont ->
			val dialog = MaterialAlertDialogBuilder(context)
				.setTitle(R.string.image_server)
				.setCancelable(true)
				.setSingleChoiceItems(entries, entryValues.indexOf(initialValue)) { _, i ->
					currentValue = entryValues[i]
				}.setNegativeButton(android.R.string.cancel) { dialog, _ ->
					dialog.cancel()
				}.setPositiveButton(android.R.string.ok) { _, _ ->
					if (currentValue != initialValue) {
						config[key] = currentValue
						cont.resume(true)
					} else {
						cont.resume(false)
					}
				}.setOnCancelListener {
					cont.resume(false)
				}.create()
			dialog.show()
			cont.invokeOnCancellation {
				dialog.cancel()
			}
		}
		if (changed) {
			repository.invalidateCache()
		}
		return changed
	}
}
