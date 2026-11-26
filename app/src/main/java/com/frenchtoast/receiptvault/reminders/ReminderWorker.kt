package com.frenchtoast.receiptvault.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.frenchtoast.receiptvault.MainActivity
import com.frenchtoast.receiptvault.R
import com.frenchtoast.receiptvault.data.local.VaultDatabase
import com.frenchtoast.receiptvault.data.preferences.VaultPreferences
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.model.VaultItem

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = VaultPreferences(applicationContext)
        val settings = prefs.getSnapshot()
        val forceCheck = inputData.getBoolean(KEY_FORCE_CHECK, false)
        if (!settings.remindersEnabled && !forceCheck) {
            return Result.success()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        ensureNotificationChannel()

        val dao = VaultDatabase.getInstance(applicationContext).vaultItemDao()
        val items = dao.getAllSnapshot().map { it.toDomain() }
        val today = VaultDate.todayEpochDay()

        val dueAlerts = findDueAlerts(
            items = items,
            todayEpochDay = today,
            returnLeadDays = settings.returnLeadDays,
            warrantyLeadDays = settings.warrantyLeadDays,
        )

        if (dueAlerts.isEmpty()) {
            if (forceCheck) {
                postSummaryNotification(
                    title = "Toasts Receipt Vault",
                    message = "All tracked return windows and warranties are outside your alert window.",
                    notificationId = SUMMARY_NOTIFICATION_ID,
                )
            }
            return Result.success()
        }

        val summaryLines = dueAlerts.take(4).joinToString("\n")
        val extraCount = (dueAlerts.size - 4).coerceAtLeast(0)
        val body = if (extraCount > 0) "$summaryLines\n+$extraCount more in your vault" else summaryLines

        postSummaryNotification(
            title = "${dueAlerts.size} upcoming vault ${if (dueAlerts.size == 1) "deadline" else "deadlines"}",
            message = body,
            notificationId = SUMMARY_NOTIFICATION_ID,
        )
        prefs.setLastReminderEpochDay(today)
        return Result.success()
    }

    private fun findDueAlerts(
        items: List<VaultItem>,
        todayEpochDay: Long,
        returnLeadDays: Int,
        warrantyLeadDays: Int,
    ): List<String> {
        val alerts = mutableListOf<String>()
        for (item in items) {
            val rDays = item.returnDaysRemaining(todayEpochDay)
            if (rDays != null && rDays in 0L..returnLeadDays.toLong()) {
                val whenText = when (rDays) {
                    0L -> "due today"
                    1L -> "due tomorrow"
                    else -> "due in $rDays days"
                }
                alerts.add("Return: ${item.title} ($whenText)")
            }

            val wDays = item.warrantyDaysRemaining(todayEpochDay)
            if (wDays != null && wDays in 0L..warrantyLeadDays.toLong()) {
                val whenText = when (wDays) {
                    0L -> "expires today"
                    1L -> "expires tomorrow"
                    else -> "expires in $wDays days"
                }
                alerts.add("Warranty: ${item.title} ($whenText)")
            }
        }
        return alerts
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = applicationContext.getString(R.string.notification_channel_id)
            val name = applicationContext.getString(R.string.notification_channel_name)
            val descriptionText = applicationContext.getString(R.string.notification_channel_description)
            val channel = NotificationChannel(
                channelId,
                name,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = descriptionText
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun postSummaryNotification(title: String, message: String, notificationId: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = applicationContext.getString(R.string.notification_channel_id)
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message.lines().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }

    companion object {
        const val KEY_FORCE_CHECK = "force_check"
        private const val SUMMARY_NOTIFICATION_ID = 4102
    }
}
