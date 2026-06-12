package com.nyora.hasan72341.sync.supabase

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class SupabaseSyncWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted params: WorkerParameters,
	private val sync: SupabaseSync,
	private val config: SupabaseConfig,
) : CoroutineWorker(context, params) {

	override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
		if (!config.isAuthenticated) return@withContext Result.retry()
		try {
			sync.syncNow()
			Result.success()
		} catch (e: Exception) {
			android.util.Log.e("SupabaseSyncWorker", "Sync failed", e)
			Result.retry()
		}
	}

	companion object {
		private const val UNIQUE_WORK_NAME = "supabase_sync"
		private const val UNIQUE_MUTATION_WORK_NAME = "supabase_sync_mutation"

		private val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.setRequiresBatteryNotLow(true)
			.build()

		fun enqueue(context: Context) {
			val request = OneTimeWorkRequestBuilder<SupabaseSyncWorker>()
				.setConstraints(constraints)
				.setInputData(workDataOf())
				.build()
			WorkManager.getInstance(context).enqueueUniqueWork(
				UNIQUE_MUTATION_WORK_NAME,
				ExistingWorkPolicy.REPLACE,
				request
			)
		}

		fun schedulePeriodic(context: Context) {
			val request = PeriodicWorkRequestBuilder<SupabaseSyncWorker>(
				1, TimeUnit.HOURS,
				15, TimeUnit.MINUTES
			)
				.setConstraints(constraints)
				.build()
			WorkManager.getInstance(context).enqueueUniquePeriodicWork(
				UNIQUE_WORK_NAME,
				ExistingPeriodicWorkPolicy.KEEP,
				request
			)
		}

		fun cancel(context: Context) {
			WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
		}
	}
}
