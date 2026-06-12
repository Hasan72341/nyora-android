package com.nyora.hasan72341.scrobbling.mal.domain

import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.scrobbling.common.domain.Scrobbler
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingStatus
import com.nyora.hasan72341.scrobbling.mal.data.MALRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val RATING_MAX = 10f

@Singleton
class MALScrobbler @Inject constructor(
	private val repository: MALRepository,
	db: MangaDatabase,
	mangaRepositoryFactory: MangaRepository.Factory,
) : Scrobbler(db, ScrobblerService.MAL, repository, mangaRepositoryFactory) {

	init {
		statuses[ScrobblingStatus.PLANNED] = "plan_to_read"
		statuses[ScrobblingStatus.READING] = "reading"
		statuses[ScrobblingStatus.COMPLETED] = "completed"
		statuses[ScrobblingStatus.ON_HOLD] = "on_hold"
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
