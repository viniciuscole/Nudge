package dev.viniciuscole.nudge.ui.format

import android.content.res.Resources
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.Reminder
import java.util.Locale

object ReminderFormat {

    fun time(hour: Int, minute: Int): String = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    fun hourOnly(hour: Int): String = String.format(Locale.ROOT, "%d:00", hour)

    fun interval(res: Resources, minutes: Int): String = when {
        minutes < 60 -> res.getString(R.string.interval_minutes, minutes)
        minutes % 60 == 0 -> res.getString(R.string.interval_hours, minutes / 60)
        else -> res.getString(R.string.interval_hours_minutes, minutes / 60, minutes % 60)
    }

    fun schedule(res: Resources, r: Reminder): String {
        val base = if (r.isMeal) {
            res.getString(R.string.schedule_daily, time(r.hour, r.minute))
        } else {
            res.getString(R.string.schedule_water, hourOnly(r.startHour), hourOnly(r.endHour), interval(res, r.intervalMin))
        }
        return if (r.enabled) base else "$base ${res.getString(R.string.schedule_off_suffix)}"
    }
}
