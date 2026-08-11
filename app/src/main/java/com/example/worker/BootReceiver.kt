package com.example.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.UserPreferencesRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            UpdateScheduler.schedule(context)
            val prefs = UserPreferencesRepository(context)
            if (prefs.isStudyRemindersEnabled()) {
                val interval = prefs.getReminderIntervalHours()
                StudyReminderScheduler.scheduleReminder(context, interval)
            }
        }
    }
}
