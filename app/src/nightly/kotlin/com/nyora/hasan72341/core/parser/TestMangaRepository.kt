package com.nyora.hasan72341.core.parser

import com.nyora.hasan72341.core.cache.MemoryContentCache
import com.nyora.hasan72341.core.model.TestMangaSource
import com.nyora.hasan72341.mihon.parsers.MangaLoaderContext

@Suppress("unused")
class TestMangaRepository(
	private val loaderContext: MangaLoaderContext,
	cache: MemoryContentCache
) : EmptyMangaRepository(TestMangaSource)
