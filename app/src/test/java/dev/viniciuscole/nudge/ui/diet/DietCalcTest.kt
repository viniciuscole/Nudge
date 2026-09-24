package dev.viniciuscole.nudge.ui.diet

import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import dev.viniciuscole.nudge.data.model.SavedMeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DietCalcTest {

    private val today = LocalDate.of(2026, 9, 23)
    private fun meal(id: Long, h: Int, enabled: Boolean = true) = Reminder(id, ReminderType.MEAL, "r$id", enabled = enabled, hour = h, minute = 0)
    private fun saved(id: Long, reminderId: Long, kcal100: Double, qty: Int, date: LocalDate = today) =
        SavedMeal(id, reminderId, "m", date, listOf(Ingredient(1, "x", "g", qty, kcal100, 10.0, 10.0, 10.0)))

    @Test
    fun sumsActiveMealReminders() {
        val s = DietCalc.build(listOf(meal(1, 8), meal(2, 12)), listOf(saved(10, 1, 200.0, 100), saved(11, 2, 200.0, 50)), 1800)
        assertEquals(300, s.totals.kcal)
        assertEquals(2, s.rows.size)
    }

    @Test
    fun disabledAndWaterRemindersAreExcluded() {
        val water = Reminder(3, ReminderType.WATER, "água")
        val s = DietCalc.build(listOf(meal(1, 8), meal(2, 12, enabled = false), water), listOf(saved(10, 1, 100.0, 100), saved(11, 2, 500.0, 100)), 1800)
        assertEquals(listOf(1L), s.rows.map { it.reminderId })
        assertEquals(100, s.totals.kcal)
    }

    @Test
    fun emptyMealCountsZero() {
        val s = DietCalc.build(listOf(meal(1, 8), meal(2, 12)), listOf(saved(10, 1, 100.0, 100)), 1800)
        val empty = s.rows.first { it.reminderId == 2L }
        assertTrue(empty.empty)
        assertEquals(0, empty.totals.kcal)
        assertEquals(100, s.totals.kcal)
    }

    @Test
    fun noMealsGivesZeros() {
        val s = DietCalc.build(emptyList(), emptyList(), 1800)
        assertTrue(s.rows.isEmpty())
        assertEquals(0, s.totals.kcal)
        assertEquals(0, s.totals.proteinPct + s.totals.carbsPct + s.totals.fatPct)
        assertEquals(0f, s.progress, 0f)
    }

    @Test
    fun rowsAreSortedByTime() {
        val s = DietCalc.build(listOf(meal(1, 19), meal(2, 8)), emptyList(), 1800)
        assertEquals(listOf(2L, 1L), s.rows.map { it.reminderId })
    }

    @Test
    fun usesTheLatestMealFromAnyDay() {
        val s = DietCalc.build(listOf(meal(1, 8)), listOf(saved(10, 1, 100.0, 100, today.minusDays(2)), saved(11, 1, 300.0, 100, today.minusDays(1))), 1800)
        assertEquals(300, s.totals.kcal)
    }

    @Test
    fun progressIsCappedAtOne() {
        val s = DietCalc.build(listOf(meal(1, 8)), listOf(saved(10, 1, 3000.0, 100)), 1800)
        assertEquals(1f, s.progress, 0f)
    }
}
