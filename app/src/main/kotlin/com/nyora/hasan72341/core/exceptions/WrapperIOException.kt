package com.nyora.hasan72341.core.exceptions

import okio.IOException

class WrapperIOException(override val cause: Exception) : IOException(cause)
