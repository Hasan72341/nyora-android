package com.nyora.hasan72341.scrobbling.common.domain.model

import javax.inject.Qualifier

@Qualifier
annotation class ScrobblerType(
	val service: ScrobblerService
)
