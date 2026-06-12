package com.nyora.hasan72341.mihon.parsers.util

import okhttp3.HttpUrl
import com.nyora.hasan72341.mihon.parsers.model.Content
import com.nyora.hasan72341.mihon.parsers.model.ContentSource

public interface LinkResolver {
    public val link: HttpUrl
    public suspend fun getSource(): ContentSource?
    public suspend fun getContent(): Content?
}

