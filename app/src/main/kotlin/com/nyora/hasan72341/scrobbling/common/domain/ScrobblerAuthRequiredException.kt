package com.nyora.hasan72341.scrobbling.common.domain

import okio.IOException
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService

class ScrobblerAuthRequiredException(
	val scrobbler: ScrobblerService,
) : IOException()
