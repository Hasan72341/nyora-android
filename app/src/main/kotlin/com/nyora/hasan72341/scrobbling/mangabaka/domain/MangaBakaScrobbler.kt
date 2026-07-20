package com.nyora.hasan72341.scrobbling.mangabaka.domain

import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.scrobbling.common.domain.Scrobbler
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingStatus
import com.nyora.hasan72341.scrobbling.mangabaka.data.MangaBakaRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val RATING_MAX = 100f

@Singleton
class MangaBakaScrobbler @Inject constructor(
	private val repository: MangaBakaRepository,
	db: MangaDatabase,
	mangaRepositoryFactory: MangaRepository.Factory,
) : Scrobbler(db, ScrobblerService.MANGABAKA, repository, mangaRepositoryFactory) {

	init {
		statuses[ScrobblingStatus.PLANNED] = "plan_to_read"
		statuses[ScrobblingStatus.READING] = "reading"
		statuses[ScrobblingStatus.RE_READING] = "rereading"
		statuses[ScrobblingStatus.COMPLETED] = "completed"
		statuses[ScrobblingStatus.ON_HOLD] = "paused"
		statuses[ScrobblingStatus.DROPPED] = "dropped"
	}

	override suspend fun updateScrobblingInfo(
		mangaId: String,
		rating: Float,
		status: ScrobblingStatus?,
		comment: String?,
	) {
		val entity = db.getScrobblingDao().find(scrobblerService.id, mangaId)
		requireNotNull(entity) { "Scrobbling info for manga $mangaId not found" }
		repository.updateRate(
			rateId = entity.id,
			mangaId = entity.mangaId,
			rating = rating * RATING_MAX,
			status = statuses[status],
			comment = comment,
		)
	}
}
