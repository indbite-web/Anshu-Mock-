package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.repository.UserPreferencesRepository
import java.util.Calendar

class StudyReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "study_reminders_channel"
        const val NOTIFICATION_ID = 1001

        private val LOCAL_MESSAGES = listOf(
            Pair("Time for a quick practice 🎯", "You haven't completed a test today. Keep your preparation on track."),
            Pair("Ready for a quick mock?", "Complete a short practice test and keep your streak going."),
            Pair("Maintain your daily momentum!", "A short practice session will strengthen your exam recall."),
            Pair("Keep your streak alive 🔥", "Take 5 minutes for a quick practice test today.")
        )
    }

    override suspend fun doWork(): Result {
        val prefsRepo = UserPreferencesRepository(context)

        // 1. Check if study reminders are enabled
        if (!prefsRepo.isStudyRemindersEnabled()) {
            return Result.success()
        }

        // 2. Check Quiet Hours
        val startHour = prefsRepo.getQuietHoursStartHour()
        val startMin = prefsRepo.getQuietHoursStartMinute()
        val endHour = prefsRepo.getQuietHoursEndHour()
        val endMin = prefsRepo.getQuietHoursEndMinute()

        if (isInQuietHours(startHour, startMin, endHour, endMin)) {
            return Result.success()
        }

        // 3. Check REAL local Room database for today's completed tests
        val startOfDayMs = getStartOfDayTimestamp()
        val db = AppDatabase.getDatabase(context)
        val completedTodayCount = db.examDao().getCompletedTestsCountSince(startOfDayMs)

        if (completedTodayCount >= 1) {
            // Already completed at least one test today - do NOT send further practice reminders today
            return Result.success()
        }

        // 4. Send local notification
        val (title, message) = LOCAL_MESSAGES.random()
        sendNotification(title, message)

        return Result.success()
    }

    private fun isInQuietHours(startH: Int, startM: Int, endH: Int, endM: Int): Boolean {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = startH * 60 + startM
        val endMinutes = endH * 60 + endM

        return if (startMinutes < endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else if (startMinutes > endMinutes) {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            false
        }
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun sendNotification(title: String, message: String) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            // Create Channel on API 26+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Study Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Practice and mock-test reminders from Anshu Mock."
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Deep link Intent to open Create Practice Test screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_NAVIGATE_ROUTE", "create_test")
            }

            val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                pendingIntentFlags
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(
                    0,
                    "Start Practice",
                    pendingIntent
                )
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Throwable) {
            android.util.Log.e("StudyReminderWorker", "Failed to send notification", e)
        }
    }
}
