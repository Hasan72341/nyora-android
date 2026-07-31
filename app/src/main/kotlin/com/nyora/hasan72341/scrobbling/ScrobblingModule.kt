package com.nyora.hasan72341.scrobbling

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import okhttp3.OkHttpClient
import com.nyora.hasan72341.core.network.BaseHttpClient
import com.nyora.hasan72341.scrobbling.anilist.data.AniListAuthenticator
import com.nyora.hasan72341.scrobbling.anilist.data.AniListInterceptor
import com.nyora.hasan72341.scrobbling.anilist.domain.AniListScrobbler
import com.nyora.hasan72341.scrobbling.common.data.ScrobblerStorage
import com.nyora.hasan72341.scrobbling.common.domain.Scrobbler
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerType
import com.nyora.hasan72341.scrobbling.mal.data.MALAuthenticator
import com.nyora.hasan72341.scrobbling.mal.data.MALInterceptor
import com.nyora.hasan72341.scrobbling.mal.domain.MALScrobbler
import com.nyora.hasan72341.scrobbling.mangabaka.data.MangaBakaAuthenticator
import com.nyora.hasan72341.scrobbling.mangabaka.data.MangaBakaInterceptor
import com.nyora.hasan72341.scrobbling.mangabaka.domain.MangaBakaScrobbler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScrobblingModule {

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.MAL)
	fun provideMALHttpClient(
		@BaseHttpClient baseHttpClient: OkHttpClient,
		authenticator: MALAuthenticator,
		@ScrobblerType(ScrobblerService.MAL) storage: ScrobblerStorage,
	): OkHttpClient = baseHttpClient.newBuilder().apply {
		authenticator(authenticator)
		addInterceptor(MALInterceptor(storage))
	}.build()

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.ANILIST)
	fun provideAniListHttpClient(
		@BaseHttpClient baseHttpClient: OkHttpClient,
		authenticator: AniListAuthenticator,
		@ScrobblerType(ScrobblerService.ANILIST) storage: ScrobblerStorage,
	): OkHttpClient = baseHttpClient.newBuilder().apply {
		authenticator(authenticator)
		addInterceptor(AniListInterceptor(storage))
	}.build()

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.MANGABAKA)
	fun provideMangaBakaHttpClient(
		@BaseHttpClient baseHttpClient: OkHttpClient,
		authenticator: MangaBakaAuthenticator,
		@ScrobblerType(ScrobblerService.MANGABAKA) storage: ScrobblerStorage,
	): OkHttpClient = baseHttpClient.newBuilder().apply {
		authenticator(authenticator)
		addInterceptor(MangaBakaInterceptor(storage))
	}.build()

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.ANILIST)
	fun provideAniListStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.ANILIST)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.MAL)
	fun provideMALStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.MAL)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.MANGABAKA)
	fun provideMangaBakaStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.MANGABAKA)

	@Provides
	@ElementsIntoSet
	fun provideScrobblers(
		aniListScrobbler: AniListScrobbler,
		malScrobbler: MALScrobbler,
		mangaBakaScrobbler: MangaBakaScrobbler,
	): Set<@JvmSuppressWildcards Scrobbler> = setOf(
		aniListScrobbler, malScrobbler, mangaBakaScrobbler,
	)
}
