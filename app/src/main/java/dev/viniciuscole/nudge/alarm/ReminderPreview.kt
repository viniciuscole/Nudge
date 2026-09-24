package dev.viniciuscole.nudge.alarm

import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.data.model.Reminder

object ReminderPreview {
    fun run(app: NudgeApp, r: Reminder) {
        if (r.insistent) {
            AlarmRingingService.start(app, r.id)
            app.startActivity(AlarmActivity.intent(app, r.id))
        } else {
            Notifications.postGentle(app, r)
        }
    }
}
