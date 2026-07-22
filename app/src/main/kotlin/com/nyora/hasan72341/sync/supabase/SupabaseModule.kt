package com.nyora.hasan72341.sync.supabase

import android.content.Context
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.network.BaseHttpClient
import com.nyora.hasan72341.core.parser.datadriven.DataDrivenCatalogueRepository
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.explore.data.MangaSourcesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseSync(
        @ApplicationContext context: Context,
        database: MangaDatabase,
        @BaseHttpClient http: OkHttpClient,
        config: SupabaseConfig,
        settings: AppSettings,
        catalogue: DataDrivenCatalogueRepository,
        mangaSourcesRepository: MangaSourcesRepository,
    ): SupabaseSync = SupabaseSync(context, database, http, config, settings, catalogue, mangaSourcesRepository)
}
