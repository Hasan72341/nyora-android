package com.nyora.hasan72341.scrobbling.common.data

import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerManga
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerMangaInfo
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerUser

interface ScrobblerRepository {

	val oauthUrl: String

	val isAuthorized: Boolean

	val cachedUser: ScrobblerUser?

	suspend fun authorize(code: String?)

	suspend fun loadUser(): ScrobblerUser

	fun logout()

	suspend fun unregister(mangaId: String)

	suspend fun findManga(query: String, offset: Int): List<ScrobblerManga>

	suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo

	suspend fun createRate(mangaId: String, scrobblerMangaId: Long)

	suspend fun updateRate(rateId: Int, mangaId: String, chapter: Int)

	suspend fun updateRate(rateId: Int, mangaId: String, rating: Float, status: String?, comment: String?)
}
