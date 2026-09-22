package dev.viniciuscole.nudge.data.model

enum class ReminderType { MEAL, WATER }

data class Reminder(
    val id: Long,
    val type: ReminderType,
    val label: String,
    val enabled: Boolean = true,
    val insistent: Boolean = true,
    val hour: Int = 12,
    val minute: Int = 30,
    val startHour: Int = 8,
    val endHour: Int = 22,
    val intervalMin: Int = 90,
) {
    val isMeal: Boolean get() = type == ReminderType.MEAL
    val isWater: Boolean get() = type == ReminderType.WATER
}
