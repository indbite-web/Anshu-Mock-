package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.update.UpdateChecker

class UpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "app_updates_channel"
        const val NOTIFICATION_ID = 2002
        private const val TAG = "UpdateCheckWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing scheduled background update check")
        val checker = UpdateChecker()
        val result = checker.checkForUpdate()

        result.onSuccess { updateInfo ->
            if (updateInfo != null) {
                val prefs = context.getSharedPreferences("anshu_update_prefs", Context.MODE_PRIVATE)
                val lastNotified = prefs.getString("last_notified_version", "")
                if (lastNotified != updateInfo.versionName) {
                    sendUpdateNotification(updateInfo.versionName)
                    prefs.edit().putString("last_notified_version", updateInfo.versionName).apply()
                } else {
                    Log.d(TAG, "Already notified user for version ${updateInfo.versionName}")
                }
            } else {
                Log.d(TAG, "Background update check: app is up-to-date")
            }
        }

        result.onFailure { error ->
            Log.w(TAG, "Background update check failed", error)
        }

        return Result.success()
    }

    private fun sendUpdateNotification(versionName: String) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "App Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for new Anshu Mock app updates."
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_SHOW_UPDATE", true)
            }

            val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                pendingIntentFlags
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Anshu Mock Update Available")
                .setContentText("Anshu Mock v$versionName is available.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Anshu Mock v$versionName is available. Tap to open and update."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to send update notification", e)
        }
    }
}
