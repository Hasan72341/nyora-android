package com.nyora.hasan72341.mihon.parsers.config

interface ContentSourceConfig {
	operator fun <T> get(key: ConfigKey<T>): T
}
