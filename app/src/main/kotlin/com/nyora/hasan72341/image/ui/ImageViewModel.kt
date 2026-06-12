package com.nyora.hasan72341.image.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.SavedStateHandle
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import com.nyora.hasan72341.core.model.MangaSource
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.core.util.ext.getDrawableOrThrow
import com.nyora.hasan72341.core.util.ext.mangaSourceExtra
import com.nyora.hasan72341.core.util.ext.require
import javax.inject.Inject

@HiltViewModel
class ImageViewModel @Inject constructor(
	@ApplicationContext private val context: Context,
	private val savedStateHandle: SavedStateHandle,
	private val coil: ImageLoader,
) : BaseViewModel() {

	val onImageSaved = MutableEventFlow<Uri>()

	fun saveImage(destination: Uri) {
		launchLoadingJob(Dispatchers.IO) {
			val request = ImageRequest.Builder(context)
				.memoryCachePolicy(CachePolicy.READ_ONLY)
				.data(savedStateHandle.require<Uri>(AppRouter.KEY_DATA))
				.memoryCachePolicy(CachePolicy.DISABLED)
				.mangaSourceExtra(MangaSource(savedStateHandle[AppRouter.KEY_SOURCE]))
				.build()
			val bitmap = coil.execute(request).getDrawableOrThrow().toBitmap()
			runInterruptible(Dispatchers.IO) {
				context.contentResolver.openOutputStream(destination)?.use { output ->
					check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
				} ?: error("Cannot open output stream")
			}
			onImageSaved.call(destination)
		}
	}
}
