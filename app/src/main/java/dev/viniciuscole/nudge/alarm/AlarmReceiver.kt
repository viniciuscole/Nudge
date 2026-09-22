package dev.viniciuscole.nudge.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.viniciuscole.nudge.NudgeApp

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val app = NudgeApp.from(context)
        val reminder = app.reminders.get(id) ?: return
        if (!reminder.enabled) return
        app.scheduler.schedule(reminder)
        AlarmRingingService.start(context, id)
    }

    companion object {
        const val ACTION_FIRE = "dev.viniciuscole.nudge.action.FIRE"
        const val EXTRA_ID = "reminder_id"
    }
}
