package com.nyora.hasan72341.settings.work

interface PeriodicWorkScheduler {

	suspend fun schedule()

	suspend fun unschedule()

	suspend fun isScheduled(): Boolean
}
