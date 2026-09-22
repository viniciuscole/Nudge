package dev.viniciuscole.nudge

import android.app.Application
import android.content.Context
import dev.viniciuscole.nudge.data.ReminderRepository

class NudgeApp : Application() {

    val reminders: ReminderRepository by lazy { ReminderRepository(this) }

    companion object {
        fun from(context: Context): NudgeApp = context.applicationContext as NudgeApp
    }
}
