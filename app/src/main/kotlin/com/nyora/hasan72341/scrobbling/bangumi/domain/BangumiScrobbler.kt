package com.nyora.hasan72341.scrobbling.bangumi.domain

import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.parser.MangaRepository
import com.nyora.hasan72341.scrobbling.bangumi.data.BangumiRepository
import com.nyora.hasan72341.scrobbling.common.domain.Scrobbler
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingStatus
import javax.inject.Inject
import javax.inject.Singleton

private const val RATING_MAX = 10f

@Singleton
class BangumiScrobbler @Inject constructor(
	private val repository: BangumiRepository,
	db: MangaDatabase,
	mangaRepositoryFactory: MangaRepository.Factory,
) : Scrobbler(db, ScrobblerService.BANGUMI, repository, mangaRepositoryFactory) {

	init {
		// Bangumi collection "type" ids.
		statuses[ScrobblingStatus.PLANNED] = "1"
		statuses[ScrobblingStatus.COMPLETED] = "2"
		statuses[ScrobblingStatus.READING] = "3"
		statuses[ScrobblingStatus.RE_READING] = "3"
		statuses[ScrobblingStatus.ON_HOLD] = "4"
		statuses[ScrobblingStatus.DROPPED] = "5"
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
