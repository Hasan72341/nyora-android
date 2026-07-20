package com.nyora.hasan72341.core

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.InvalidationTracker
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import com.nyora.hasan72341.BuildConfig
import com.nyora.hasan72341.core.db.MangaDatabase
import com.nyora.hasan72341.core.os.AppValidator
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.util.ext.processLifecycleScope
import com.nyora.hasan72341.local.data.LocalStorageChanges
import com.nyora.hasan72341.local.data.index.LocalMangaIndex
import com.nyora.hasan72341.local.domain.model.LocalManga
import com.nyora.hasan72341.mihon.MihonExtensionManager
import com.nyora.hasan72341.settings.work.WorkScheduleManager
import com.nyora.hasan72341.sync.supabase.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.internal.platform.PlatformRegistry
import org.conscrypt.Conscrypt
import java.security.Security
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
open class BaseApp : Application(), Configuration.Provider {

	@Inject
	lateinit var mihonExtensionManager: MihonExtensionManager

	@Inject
	lateinit var databaseObserversProvider: Provider<Set<@JvmSuppressWildcards InvalidationTracker.Observer>>

	@Inject
	lateinit var activityLifecycleCallbacks: Set<@JvmSuppressWildcards ActivityLifecycleCallbacks>

	@Inject
	lateinit var database: Provider<MangaDatabase>

	@Inject
	lateinit var settings: AppSettings

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	@Inject
	lateinit var appValidator: AppValidator

	@Inject
	lateinit var workScheduleManager: WorkScheduleManager

	@Inject
	lateinit var localMangaIndexProvider: Provider<LocalMangaIndex>

	@Inject
	@LocalStorageChanges
	lateinit var localStorageChanges: MutableSharedFlow<LocalManga?>

	@Inject
	lateinit var supabaseConfig: SupabaseConfig

	@Inject
	lateinit var remoteSourceGate: RemoteSourceGate

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()

	override fun onCreate() {
		super.onCreate()
		// Load the obfuscated-domain table before any parser resolves its domain (release builds
		// have their domain constants rewritten to DomainVault.d(index); see buildSrc).
		com.nyora.hasan72341.core.vault.DomainVault.init(this)
		PlatformRegistry.applicationContext = this // TODO replace with OkHttp.initialize
		AppCompatDelegate.setDefaultNightMode(settings.theme)
		
		// TLS 1.3 support for Android < 10
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			Security.insertProviderAt(Conscrypt.newProvider(), 1)
		}
		setupActivityLifecycleCallbacks()
		mihonExtensionManager.initialize()
		processLifecycleScope.launch(Dispatchers.IO) {
			setupDatabaseObservers()
			localStorageChanges.collect(localMangaIndexProvider.get())
		}
		workScheduleManager.init()
		// Self-hosted Nyora sync server (OAuth2/JWT — replaces Supabase + Google).
		// anonKey is unused by this server but kept non-blank so isConfigured stays true.
		supabaseConfig.configure(
			url = "https://sync.nyora.xyz",
			anonKey = "self-hosted"
		)
		// Remote master switch: fetch + verify the signed config and unlock/lock
		// sources accordingly (ships locked for Store review; enabled remotely).
		// Debug builds bypass the gate so the app is fully testable offline,
		// independent of the remote config. Release builds honour the switch.
		if (BuildConfig.DEBUG) {
			settings.isSourcesUnlocked = true
		} else {
			processLifecycleScope.launch(Dispatchers.IO) {
				remoteSourceGate.refresh()
			}
		}
	}

	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		// ACRA removed
	} 

	@WorkerThread
	private fun setupDatabaseObservers() {
		val tracker = database.get().invalidationTracker
		databaseObserversProvider.get().forEach {
			tracker.addObserver(it)
		}
	}

	private fun setupActivityLifecycleCallbacks() {
		activityLifecycleCallbacks.forEach {
			registerActivityLifecycleCallbacks(it)
		}
	}
}
