package com.nyora.hasan72341.core.parser.favicon

import android.net.Uri
import com.nyora.hasan72341.mihon.parsers.model.MangaSource

const val URI_SCHEME_FAVICON = "favicon"

fun MangaSource.faviconUri(): Uri = Uri.fromParts(URI_SCHEME_FAVICON, name, null)