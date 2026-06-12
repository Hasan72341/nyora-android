package com.nyora.hasan72341.local.domain

import com.nyora.hasan72341.core.util.MultiMutex
import com.nyora.hasan72341.mihon.parsers.model.Manga
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaLock @Inject constructor() : MultiMutex<Manga>()
