package com.nyora.hasan72341.scrobbling

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import okhttp3.OkHttpClient
import com.nyora.hasan72341.BuildConfig
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.network.BaseHttpClient
import com.nyora.hasan72341.core.network.CurlLoggingInterceptor
import com.nyora.hasan72341.scrobbling.anilist.data.AniListAuthenticator
import com.nyora.hasan72341.scrobbling.anilist.data.AniListInterceptor
import com.nyora.hasan72341.scrobbling.anilist.domain.AniListScrobbler
import com.nyora.hasan72341.scrobbling.common.data.ScrobblerStorage
import com.nyora.hasan72341.scrobbling.common.domain.Scrobbler
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerType
import com.nyora.hasan72341.scrobbling.kitsu.data.KitsuAuthenticator
import com.nyora.hasan72341.scrobbling.kitsu.data.KitsuInterceptor
import com.nyora.hasan72341.scrobbling.kitsu.data.KitsuRepository
import com.nyora.hasan72341.scrobbling.kitsu.domain.KitsuScrobbler
import com.nyora.hasan72341.scrobbling.mal.data.MALAuthenticator
import com.nyora.hasan72341.scrobbling.mal.data.MALInterceptor
import com.nyora.hasan72341.scrobbling.mal.domain.MALScrobbler
import com.nyora.hasan72341.scrobbling.shikimori.data.ShikimoriAuthenticator
import com.nyora.hasan72341.scrobbling.shikimori.data.ShikimoriInterceptor
import com.nyora.hasan72341.scrobbling.shikimori.domain.ShikimoriScrobbler
import com.nyora.hasan72341.scrobbling.bangumi.data.BangumiAuthenticator
import com.nyora.hasan72341.scrobbling.bangumi.data.BangumiInterceptor
import com.nyora.hasan72341.scrobbling.bangumi.domain.BangumiScrobbler
import com.nyora.hasan72341.scrobbling.mangabaka.data.MangaBakaAuthenticator
import com.nyora.hasan72341.scrobbling.mangabaka.data.MangaBakaInterceptor
import com.nyora.hasan72341.scrobbling.mangabaka.domain.MangaBakaScrobbler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScrobblingModule {

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.SHIKIMORI)
	fun provideShikimoriHttpClient(
		@BaseHttpClient baseHttpClient: OkHttpClient,
		authenticator: ShikimoriAuthenticator,
		@ScrobblerType(ScrobblerService.SHIKIMORI) storage: ScrobblerStorage,
	): OkHttpClient = baseHttpClient.newBuilder().apply {
		authenticator(authenticator)
		addInterceptor(ShikimoriInterceptor(storage))
	}.build()

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
	@ScrobblerType(ScrobblerService.BANGUMI)
	fun provideBangumiHttpClient(
		@BaseHttpClient baseHttpClient: OkHttpClient,
		authenticator: BangumiAuthenticator,
		@ScrobblerType(ScrobblerService.BANGUMI) storage: ScrobblerStorage,
	): OkHttpClient = baseHttpClient.newBuilder().apply {
		authenticator(authenticator)
		addInterceptor(BangumiInterceptor(storage))
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
	fun provideKitsuRepository(
		@ApplicationContext context: Context,
		@ScrobblerType(ScrobblerService.KITSU) storage: ScrobblerStorage,
		database: MangaDatabase,
		authenticator: KitsuAuthenticator,
	): KitsuRepository {
		val okHttp = OkHttpClient.Builder().apply {
			authenticator(authenticator)
			addInterceptor(KitsuInterceptor(storage))
			if (BuildConfig.DEBUG) {
				addInterceptor(CurlLoggingInterceptor())
			}
		}.build()
		return KitsuRepository(context, okHttp, storage, database)
	}

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.ANILIST)
	fun provideAniListStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.ANILIST)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.SHIKIMORI)
	fun provideShikimoriStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.SHIKIMORI)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.MAL)
	fun provideMALStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.MAL)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.KITSU)
	fun provideKitsuStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.KITSU)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.BANGUMI)
	fun provideBangumiStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.BANGUMI)

	@Provides
	@Singleton
	@ScrobblerType(ScrobblerService.MANGABAKA)
	fun provideMangaBakaStorage(
		@ApplicationContext context: Context,
	): ScrobblerStorage = ScrobblerStorage(context, ScrobblerService.MANGABAKA)

	@Provides
	@ElementsIntoSet
	fun provideScrobblers(
		shikimoriScrobbler: ShikimoriScrobbler,
		aniListScrobbler: AniListScrobbler,
		malScrobbler: MALScrobbler,
		kitsuScrobbler: KitsuScrobbler,
		bangumiScrobbler: BangumiScrobbler,
		mangaBakaScrobbler: MangaBakaScrobbler,
	): Set<@JvmSuppressWildcards Scrobbler> = setOf(
		shikimoriScrobbler, aniListScrobbler, malScrobbler, kitsuScrobbler, bangumiScrobbler, mangaBakaScrobbler,
	)
}
