package org.uciasistant.feature.alerts

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.IOException
import java.util.concurrent.TimeUnit

internal object AlertReminderWork {
    const val REMINDER_ID_KEY = "reminder_id"
    private const val WORK_NAME_PREFIX = "uci-assistant-manual-reminder-"

    fun schedule(context: Context, reminder: AlertReminder) {
        val delay = (reminder.dueAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<AlertReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(REMINDER_ID_KEY to reminder.id))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName(reminder.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, reminderId: String) {
        if (isValidReminderId(reminderId)) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(workName(reminderId))
        }
    }

    private fun workName(reminderId: String) = "$WORK_NAME_PREFIX$reminderId"

    private fun isValidReminderId(id: String): Boolean =
        runCatching { java.util.UUID.fromString(id).toString() == id }.getOrDefault(false)
}

/** WorkManager provides best-effort, inexact scheduling; this is not an urgent or clinical alarm. */
class AlertReminderWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result {
        val reminderId = inputData.getString(AlertReminderWork.REMINDER_ID_KEY) ?: return Result.failure()
        val store = EncryptedAlertReminderStore(applicationContext)
        val reminder = try {
            store.readReminder(reminderId)
        } catch (_: IOException) {
            return Result.failure()
        } ?: return Result.success()

        if (reminder.dueAtMillis > System.currentTimeMillis()) return Result.retry()
        AlertReminderNotifications.show(applicationContext, reminder.id)
        return Result.success()
    }
}

const val EXTRA_OPEN_ALERTS = "org.uciasistant.feature.alerts.OPEN_ALERTS"

internal object AlertReminderNotifications {
    private const val CHANNEL_ID = "manual-reminders"
    private const val CHANNEL_NAME = "Recordatorios manuales"

    fun show(context: Context, reminderId: String) {
        val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(notificationManager)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launchIntent.putExtra(EXTRA_OPEN_ALERTS, true)
        launchIntent.data = Uri.parse("uciassistant://manual-reminders/$reminderId")
        val contentIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Recordatorio en UCI Assistant")
            .setContentText("Abre la aplicación para consultar tus recordatorios.")
            .setCategory(android.app.Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(android.app.Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(
                android.app.Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("UCI Assistant")
                    .setContentText("Recordatorio pendiente")
                    .build(),
            )
            .setContentIntent(contentIntent)
            .build()
        notificationManager.notify(reminderId.hashCode(), notification)
    }

    fun cancel(context: Context, reminderId: String) {
        context.getSystemService(android.app.NotificationManager::class.java)
            ?.cancel(reminderId.hashCode())
    }

    private fun createChannel(manager: android.app.NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                android.app.NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    android.app.NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Avisos genéricos para recordatorios manuales no clínicos."
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                },
            )
        }
    }
}
