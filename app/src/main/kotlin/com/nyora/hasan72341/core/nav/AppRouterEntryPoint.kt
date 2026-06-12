package com.nyora.hasan72341.core.nav

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.nyora.hasan72341.core.prefs.AppSettings

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppRouterEntryPoint {

	val settings: AppSettings
}
