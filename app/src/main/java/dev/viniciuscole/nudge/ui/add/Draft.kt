package dev.viniciuscole.nudge.ui.add

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType

data class Draft(
    val type: ReminderType = ReminderType.MEAL,
    val label: String = "",
    val hour: Int = 12,
    val minute: Int = 30,
    val startHour: Int = 8,
    val endHour: Int = 22,
    val intervalMin: Int = 90,
    val customInterval: Boolean = false,
    val insistent: Boolean = true,
) {
    fun toReminder(id: Long, enabled: Boolean, defaultMealLabel: String, defaultWaterLabel: String): Reminder = Reminder(
        id = id,
        type = type,
        label = label.trim().ifEmpty { if (type == ReminderType.MEAL) defaultMealLabel else defaultWaterLabel },
        enabled = enabled,
        insistent = insistent,
        hour = hour,
        minute = minute,
        startHour = startHour,
        endHour = endHour,
        intervalMin = intervalMin.coerceAtLeast(5),
    )

    companion object {
        val PRESET_INTERVALS = listOf(30, 60, 90, 120)

        fun from(r: Reminder): Draft = Draft(
            type = r.type,
            label = r.label,
            hour = r.hour,
            minute = r.minute,
            startHour = r.startHour,
            endHour = r.endHour,
            intervalMin = r.intervalMin,
            customInterval = r.intervalMin !in PRESET_INTERVALS,
            insistent = r.insistent,
        )
    }
}
