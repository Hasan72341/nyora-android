package com.nyora.hasan72341.scrobbling.common.domain

import com.nyora.hasan72341.scrobbling.anilist.data.AniListRepository
import com.nyora.hasan72341.scrobbling.common.data.ScrobblerRepository
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.mal.data.MALRepository
import com.nyora.hasan72341.scrobbling.mangabaka.data.MangaBakaRepository
import javax.inject.Inject
import javax.inject.Provider

class ScrobblerRepositoryMap @Inject constructor(
	private val aniListRepository: Provider<AniListRepository>,
	private val malRepository: Provider<MALRepository>,
	private val mangaBakaRepository: Provider<MangaBakaRepository>,
) {

	operator fun get(scrobblerService: ScrobblerService): ScrobblerRepository = when (scrobblerService) {
		ScrobblerService.ANILIST -> aniListRepository
		ScrobblerService.MAL -> malRepository
		ScrobblerService.MANGABAKA -> mangaBakaRepository
	}.get()
}
