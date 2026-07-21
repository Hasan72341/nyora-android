package com.nyora.hasan72341.local.ui

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.ErrorReporterReceiver
import com.nyora.hasan72341.core.model.parcelable.ParcelableManga
import com.nyora.hasan72341.core.ui.CoroutineIntentService
import com.nyora.hasan72341.core.util.ext.getDisplayMessage
import com.nyora.hasan72341.core.util.ext.getParcelableExtraCompat
import com.nyora.hasan72341.core.util.ext.powerManager
import com.nyora.hasan72341.core.util.ext.withPartialWakeLock
import com.nyora.hasan72341.local.data.LocalMangaRepository
import com.nyora.hasan72341.local.data.LocalStorageChanges
import com.nyora.hasan72341.local.domain.model.LocalManga
import com.nyora.hasan72341.mihon.parsers.model.Manga
import javax.inject.Inject

@AndroidEntryPoint
class LocalChaptersRemoveService : CoroutineIntentService() {

	@Inject
	lateinit var localMangaRepository: LocalMangaRepository

	@Inject
	@LocalStorageChanges
	lateinit var localStorageChanges: MutableSharedFlow<LocalManga?>

	override fun onCreate() {
		super.onCreate()
		isRunning = true
	}

	override fun onDestroy() {
		isRunning = false
		super.onDestroy()
	}

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		startForeground(this)
		val manga = intent.getParcelableExtraCompat<ParcelableManga>(EXTRA_MANGA)?.manga ?: return
		val chaptersIds = intent.getStringArrayExtra(EXTRA_CHAPTERS_IDS)?.toSet() ?: return
		powerManager.withPartialWakeLock(TAG) {
			val mangaWithChapters = localMangaRepository.getDetails(manga)
			localMangaRepository.deleteChapters(mangaWithChapters, chaptersIds)
			localStorageChanges.emit(LocalManga(localMangaRepository.getDetails(manga)))
		}
	}

	override fun IntentJobContext.onError(error: Throwable) {
		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setContentTitle(getString(R.string.error_occurred))
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setDefaults(0)
			.setSilent(true)
			.setContentText(error.getDisplayMessage(resources))
			.setSmallIcon(android.R.drawable.stat_notify_error)
			.setAutoCancel(true)
			.setContentIntent(ErrorReporterReceiver.getPendingIntent(applicationContext, error))
			.build()
		val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		nm.notify(NOTIFICATION_ID + startId, notification)
	}

	@SuppressLint("InlinedApi")
	private fun startForeground(jobContext: IntentJobContext) {
		val title = getString(R.string.local_manga_processing)
		val manager = NotificationManagerCompat.from(this)
		val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
			.setName(title)
			.setShowBadge(false)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		manager.createNotificationChannel(channel)

		val notification = NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle(title)
			.setPriority(NotificationCompat.PRIORITY_MIN)
			.setDefaults(0)
			.setSilent(true)
			.setProgress(0, 0, true)
			.setSmallIcon(android.R.drawable.stat_notify_sync)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
			.setOngoing(false)
			.build()
		jobContext.setForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
	}

	companion object {

		var isRunning: Boolean = false
			private set

		private const val CHANNEL_ID = "local_processing"
		private const val NOTIFICATION_ID = 21

		private const val EXTRA_MANGA = "manga"
		private const val EXTRA_CHAPTERS_IDS = "chapters_ids"

		private const val TAG = CHANNEL_ID

		fun start(context: Context, manga: Manga, chaptersIds: Collection<String>) {
			if (chaptersIds.isEmpty()) {
				return
			}
			val intent = Intent(context, LocalChaptersRemoveService::class.java)
			intent.putExtra(EXTRA_MANGA, ParcelableManga(manga))
			intent.putExtra(EXTRA_CHAPTERS_IDS, chaptersIds.toTypedArray())
			ContextCompat.startForegroundService(context, intent)
		}
	}
}
