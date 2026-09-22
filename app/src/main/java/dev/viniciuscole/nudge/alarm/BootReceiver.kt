package dev.viniciuscole.nudge.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.viniciuscole.nudge.NudgeApp

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                val app = NudgeApp.from(context)
                app.scheduler.rescheduleAll(app.reminders.reminders.value)
            }
        }
    }
}
