package com.example.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UpdateScheduler {
    const val WORK_NAME = "anshu_update_check_work"

    fun schedule(context: Context) {
        try {
            val workRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                12, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Throwable) {
            android.util.Log.e("UpdateScheduler", "Failed to enqueue background update worker", e)
        }
    }
}
