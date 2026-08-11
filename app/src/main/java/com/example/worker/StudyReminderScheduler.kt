package com.example.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object StudyReminderScheduler {
    const val WORK_NAME = "study_reminder_periodic_work"

    fun scheduleReminder(context: Context, intervalHours: Int) {
        val safeHours = maxOf(1, intervalHours)
        val workRequest = PeriodicWorkRequestBuilder<StudyReminderWorker>(
            safeHours.toLong(), TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
