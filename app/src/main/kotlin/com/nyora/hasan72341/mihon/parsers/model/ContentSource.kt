package com.nyora.hasan72341.mihon.parsers.model

import com.nyora.hasan72341.mihon.parsers.model.MangaSource

interface ContentSource {
    val name: String
    val locale: String
    val contentType: ContentType
}
