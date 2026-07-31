package com.nyora.hasan72341.scrobbling.common.domain

import androidx.annotation.FloatRange
import androidx.collection.LongSparseArray
import androidx.collection.getOrElse
import androidx.core.text.parseAsHtml
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.core.util.ext.findKeyByValue
import com.nyora.hasan72341.core.util.ext.printStackTraceDebug
import com.nyora.hasan72341.core.util.ext.sanitize
import com.nyora.hasan72341.mihon.parsers.model.Manga
import com.nyora.hasan72341.mihon.parsers.util.findById
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable
import com.nyora.hasan72341.scrobbling.common.data.ScrobblerRepository
import com.nyora.hasan72341.scrobbling.common.data.ScrobblingEntity
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerManga
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerMangaInfo
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerUser
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingInfo
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.EnumMap

abstract class Scrobbler(
	protected val db: MangaDatabase,
	val scrobblerService: ScrobblerService,
	private val repository: ScrobblerRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {

	private val infoCache = LongSparseArray<ScrobblerMangaInfo>()
	protected val statuses = EnumMap<ScrobblingStatus, String>(ScrobblingStatus::class.java)

	val user: Flow<ScrobblerUser> = flow {
		repository.cachedUser?.let {
			emit(it)
		}
		runCatchingCancellable {
			repository.loadUser()
		}.onSuccess {
			emit(it)
		}.onFailure {
			it.printStackTraceDebug("Scrobbler::user")
		}
	}

	val isEnabled: Boolean
		get() = repository.isAuthorized

	suspend fun authorize(authCode: String): ScrobblerUser {
		repository.authorize(authCode)
		return repository.loadUser()
	}

	fun logout() {
		repository.logout()
	}

	suspend fun findManga(query: String, offset: Int): List<ScrobblerManga> {
		return repository.findManga(query, offset)
	}

	suspend fun linkManga(mangaId: String, targetId: Long) {
		repository.createRate(mangaId, targetId)
	}

	suspend fun scrobble(manga: Manga, chapterId: String) {
		var chapters = manga.chapters
		if (chapters.isNullOrEmpty()) {
			chapters = mangaRepositoryFactory.create(com.nyora.hasan72341.core.model.MangaSource(manga.source.name)).getDetails(manga).chapters
		}
		requireNotNull(chapters)
		val chapter = checkNotNull(chapters.find { it.id == chapterId }) {
			"Chapter $chapterId not found in this manga"
		}
		val number = if (chapter.number > 0f) {
			chapter.number.toInt()
		} else {
			chapters = chapters.filter { x -> x.branch == chapter.branch }
			chapters.indexOf(chapter) + 1
		}
		val entity = db.getScrobblingDao().find(scrobblerService.id, manga.id) ?: return
		repository.updateRate(entity.id, entity.mangaId, number)
	}

	suspend fun getScrobblingInfoOrNull(mangaId: String): ScrobblingInfo? {
		val entity = db.getScrobblingDao().find(scrobblerService.id, mangaId) ?: return null
		return entity.toScrobblingInfo()
	}

	abstract suspend fun updateScrobblingInfo(
		mangaId: String,
		@FloatRange(from = 0.0, to = 1.0) rating: Float,
		status: ScrobblingStatus?,
		comment: String?,
	)

	fun observeScrobblingInfo(mangaId: String): Flow<ScrobblingInfo?> {
		return db.getScrobblingDao().observe(scrobblerService.id, mangaId)
			.map { it?.toScrobblingInfo() }
	}

	fun observeAllScrobblingInfo(): Flow<List<ScrobblingInfo>> {
		return db.getScrobblingDao().observe(scrobblerService.id)
			.map { entities ->
				coroutineScope {
					entities.map {
						async {
							it.toScrobblingInfo()
						}
					}.awaitAll()
				}.filterNotNull()
			}
	}

	suspend fun unregisterScrobbling(mangaId: String) {
		repository.unregister(mangaId)
	}

	protected suspend fun getMangaInfo(id: Long): ScrobblerMangaInfo {
		return repository.getMangaInfo(id)
	}

	private suspend fun ScrobblingEntity.toScrobblingInfo(): ScrobblingInfo? {
		val mangaInfo = infoCache.getOrElse(targetId) {
			runCatchingCancellable {
				getMangaInfo(targetId)
			}.onFailure {
				it.printStackTraceDebug("Scrobbler::toScrobblingInfo")
			}.onSuccess {
				infoCache.put(targetId, it)
			}.getOrNull() ?: return null
		}
		return ScrobblingInfo(
			scrobbler = scrobblerService,
			mangaId = mangaId,
			targetId = targetId,
			status = statuses.findKeyByValue(status),
			chapter = chapter,
			comment = comment,
			rating = rating,
			title = mangaInfo.name,
			coverUrl = mangaInfo.cover,
			description = mangaInfo.descriptionHtml.parseAsHtml().sanitize(),
			externalUrl = mangaInfo.url,
		)
	}
}

suspend fun Scrobbler.tryScrobble(manga: Manga, chapterId: String): Boolean {
	return runCatchingCancellable {
		scrobble(manga, chapterId)
	}.onFailure {
		it.printStackTraceDebug("Scrobbler::tryScrobble")
	}.isSuccess
}
