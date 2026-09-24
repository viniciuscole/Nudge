package dev.viniciuscole.nudge.data.model

import java.time.LocalDate

data class DayStats(
    val date: LocalDate,
    val waterDone: Int = 0,
    val mealsDone: Int = 0,
)

fun DayStats.withWater(delta: Int, today: LocalDate): DayStats {
    val base = if (date == today) this else DayStats(today)
    return base.copy(waterDone = (base.waterDone + delta).coerceAtLeast(0))
}
