package dev.viniciuscole.nudge.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayStatsTest {

    private val today = LocalDate.of(2026, 9, 23)

    @Test
    fun addsAGlassOnTheSameDay() {
        assertEquals(DayStats(today, 3, 1), DayStats(today, 2, 1).withWater(1, today))
    }

    @Test
    fun undoAtZeroStaysZero() {
        assertEquals(DayStats(today, 0, 1), DayStats(today, 0, 1).withWater(-1, today))
    }

    @Test
    fun newDayStartsFromZero() {
        assertEquals(DayStats(today, 1, 0), DayStats(today.minusDays(1), 5, 2).withWater(1, today))
    }

    @Test
    fun undoRightAfterMidnightNeverGoesNegative() {
        assertEquals(DayStats(today, 0, 0), DayStats(today.minusDays(1), 5, 2).withWater(-1, today))
    }
}
