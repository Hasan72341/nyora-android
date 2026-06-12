package com.nyora.hasan72341.core.util

import android.os.Build
import android.webkit.MimeTypeMap
import org.jetbrains.annotations.Blocking
import com.nyora.hasan72341.core.util.ext.MimeType
import com.nyora.hasan72341.core.util.ext.toMimeTypeOrNull
import com.nyora.hasan72341.mihon.parsers.util.nullIfEmpty
import com.nyora.hasan72341.mihon.parsers.util.removeSuffix
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import java.io.File
import java.nio.file.Files
import coil3.util.MimeTypeMap as CoilMimeTypeMap

object MimeTypes {

	fun getMimeTypeFromExtension(fileName: String): MimeType? {
		return CoilMimeTypeMap.getMimeTypeFromExtension(getNormalizedExtension(fileName) ?: return null)
			?.toMimeTypeOrNull()
	}

	fun getMimeTypeFromUrl(url: String): MimeType? {
		return CoilMimeTypeMap.getMimeTypeFromUrl(url)?.toMimeTypeOrNull()
	}

	fun getExtension(mimeType: MimeType?): String? {
		return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType?.toString() ?: return null)?.nullIfEmpty()
	}

	@Blocking
	fun probeMimeType(file: File): MimeType? {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			runCatchingCancellable {
				Files.probeContentType(file.toPath())?.toMimeTypeOrNull()
			}.getOrNull()?.let { return it }
		}
		return getMimeTypeFromExtension(file.name)
	}

	fun getNormalizedExtension(name: String): String? = name
		.lowercase()
		.removeSuffix('~')
		.removeSuffix(".tmp")
		.substringAfterLast('.', "")
		.takeIf { it.length in 2..5 }
}
