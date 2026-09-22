package dev.viniciuscole.nudge.data.model

import java.time.LocalDate

data class DayStats(
    val date: LocalDate,
    val waterDone: Int = 0,
    val mealsDone: Int = 0,
)
