package dev.viniciuscole.nudge.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.Reminder

object Notifications {

    const val CHANNEL_ALARMS = "alarms"
    const val RINGING_ID = 1001
    const val GENTLE_ID = 1002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ALARMS,
            context.getString(R.string.channel_alarms),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarms_desc)
            // the service plays the alarm tone and vibrates; the channel must stay silent
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    fun ringing(context: Context, r: Reminder): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val fullScreen = PendingIntent.getActivity(context, r.id.toInt(), AlarmActivity.intent(context, r.id), flags)
        val done = PendingIntent.getService(context, r.id.toInt() * 2, AlarmRingingService.intent(context, r.id, AlarmRingingService.ACTION_DONE), flags)
        val snooze = PendingIntent.getService(context, r.id.toInt() * 2 + 1, AlarmRingingService.intent(context, r.id, AlarmRingingService.ACTION_SNOOZE), flags)

        return NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.label)
            .setContentText(subtitle(context, r))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .addAction(0, context.getString(R.string.action_done), done)
            .addAction(0, context.getString(R.string.action_snooze), snooze)
            .build()
    }

    fun gentle(context: Context, r: Reminder): Notification {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(context, r.id.toInt(), AlarmActivity.intent(context, r.id), flags)
        val done = PendingIntent.getService(context, r.id.toInt() * 2, AlarmRingingService.intent(context, r.id, AlarmRingingService.ACTION_DONE), flags)
        val snooze = PendingIntent.getService(context, r.id.toInt() * 2 + 1, AlarmRingingService.intent(context, r.id, AlarmRingingService.ACTION_SNOOZE), flags)

        return NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.label)
            .setContentText(context.getString(R.string.ring_sub_gentle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.action_done), done)
            .addAction(0, context.getString(R.string.action_snooze), snooze)
            .build()
    }

    fun postGentle(context: Context, r: Reminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(GENTLE_ID, gentle(context, r))
    }

    fun subtitle(context: Context, r: Reminder): String {
        if (r.isMeal) return context.getString(R.string.ring_sub_meal)
        val app = NudgeApp.from(context)
        val glass = app.stats.today().waterDone + 1
        val goal = app.reminders.reminders.value.filter { it.enabled && it.isWater }.sumOf { NextFire.slotCount(it) }
        return context.getString(R.string.ring_sub_water, glass, maxOf(goal, glass))
    }
}
