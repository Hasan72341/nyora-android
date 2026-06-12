package com.nyora.hasan72341.core.exceptions

import okio.IOException

class NoDataReceivedException(
	val url: String,
) : IOException("No data has been received from $url")
