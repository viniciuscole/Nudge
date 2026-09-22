package dev.viniciuscole.nudge.alarm

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class NextFireTest {

    private val lunch = Reminder(1, ReminderType.MEAL, "Lunch", hour = 12, minute = 30)
    private val water = Reminder(2, ReminderType.WATER, "Water", startHour = 8, endHour = 22, intervalMin = 90)
    private val day = LocalDate.of(2026, 9, 21)

    @Test
    fun mealLaterTodayFiresToday() {
        val now = day.atTime(9, 0)
        assertEquals(day.atTime(12, 30), NextFire.compute(lunch, now))
    }

    @Test
    fun mealAlreadyPassedFiresTomorrow() {
        val now = day.atTime(12, 30)
        assertEquals(day.plusDays(1).atTime(12, 30), NextFire.compute(lunch, now))
    }

    @Test
    fun waterPicksNextSlotInsideWindow() {
        val now = day.atTime(10, 0)
        assertEquals(day.atTime(11, 0), NextFire.compute(water, now))
    }

    @Test
    fun waterAfterWindowFiresAtTomorrowStart() {
        val now = day.atTime(22, 30)
        assertEquals(day.plusDays(1).atTime(8, 0), NextFire.compute(water, now))
    }

    @Test
    fun waterBeforeWindowFiresAtTodayStart() {
        val now = day.atTime(6, 0)
        assertEquals(day.atTime(8, 0), NextFire.compute(water, now))
    }

    @Test
    fun slotCountCoversWholeWindowInclusive() {
        assertEquals(10, NextFire.slotCount(water))
        assertEquals(15, NextFire.slotCount(water.copy(intervalMin = 60)))
    }

    @Test
    fun invertedWindowDegradesToSingleSlot() {
        val broken = water.copy(startHour = 20, endHour = 8)
        assertEquals(listOf(day.atTime(20, 0)), NextFire.slotsOn(broken, day))
    }
}
