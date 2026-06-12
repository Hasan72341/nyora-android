package com.nyora.hasan72341.mihon

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.nyora.hasan72341.core.network.MangaHttpClient
import com.nyora.hasan72341.core.network.webview.WebViewExecutor
import com.nyora.hasan72341.mihon.compat.MihonInjektBridge
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MihonModule {
    @Provides
    @Singleton
    fun provideMihonInjektBridge(
        @ApplicationContext context: Context,
        @MangaHttpClient okHttpClient: OkHttpClient,
        cookieJar: CookieJar,
        webViewExecutor: WebViewExecutor,
    ): MihonInjektBridge {
        return try {
            MihonInjektBridge(
                context = context,
                httpClient = okHttpClient,
                cookieJar = cookieJar,
                webViewExecutor = webViewExecutor,
            )
        } catch (e: Throwable) {
            Log.e("MihonModule", "CRITICAL ERROR: Failed to create MihonInjektBridge!", e)
            // Still need to return something or Dagger will fail. 
            // In case of fatal libs issue (NoClassDefFound), this might still crash later, 
            // but let's try to catch it here.
            throw e
        }
    }

    @Provides
    @Singleton
    fun provideMihonExtensionLoader(
        @ApplicationContext context: Context,
        injektBridge: dagger.Lazy<MihonInjektBridge>,
    ): MihonExtensionLoader {
        return MihonExtensionLoader(context,injektBridge)
    }

    @Provides
    @Singleton
    fun provideMihonExtensionManager(
        @ApplicationContext context: Context,
        loader: MihonExtensionLoader,
    ): MihonExtensionManager {
        return MihonExtensionManager(context, loader)
    }
}
