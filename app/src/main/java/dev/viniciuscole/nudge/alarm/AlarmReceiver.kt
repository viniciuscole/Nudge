package dev.viniciuscole.nudge.alarm

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.data.model.Reminder

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val app = NudgeApp.from(context)
        val reminder = app.reminders.get(id) ?: return
        if (!reminder.enabled) return
        app.scheduler.schedule(reminder)
        if (reminder.insistent) {
            AlarmRingingService.start(context, id)
        } else {
            postGentleNotification(context, reminder)
        }
    }

    private fun postGentleNotification(context: Context, reminder: Reminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(Notifications.GENTLE_ID, Notifications.gentle(context, reminder))
    }

    companion object {
        const val ACTION_FIRE = "dev.viniciuscole.nudge.action.FIRE"
        const val EXTRA_ID = "reminder_id"
    }
}
