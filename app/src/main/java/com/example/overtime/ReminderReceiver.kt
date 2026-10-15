package com.example.overtime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.overtime.data.Storage
import com.example.overtime.util.NotificationScheduler
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return

        // Проверяем — есть ли уже запись за сегодня
        val storage = Storage(context)
        val (entries, settings) = storage.load()

        if (!settings.reminderEnabled) return

        val today = LocalDate.now()
        val hasToday = entries.any { it.date == today }
        if (hasToday) {
            // Уже записал — напоминание не показываем, но переносим на завтра
            NotificationScheduler.schedule(
                context,
                settings.reminderHour,
                settings.reminderMinute
            )
            return
        }

        showNotification(context, settings.language)

        // Перепланируем на завтра (setInexactRepeating сам повторит, но на всякий случай)
        NotificationScheduler.schedule(
            context,
            settings.reminderHour,
            settings.reminderMinute
        )
    }

    private fun showNotification(context: Context, lang: String) {
        val channelId = "overtime_reminder"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (lang == "en") "Reminder" else "Напоминание",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT
        val pending = PendingIntent.getActivity(context, 0, openIntent, pendingFlags)

        val title = if (lang == "en") "Log your overtime" else "Запиши переработку"
        val text = if (lang == "en")
            "Don't forget to log today's hours and products"
        else
            "Не забудь записать сегодняшние часы и изделия"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_REMIND = "com.example.overtime.REMIND"
        const val NOTIFICATION_ID = 1001
    }
}
