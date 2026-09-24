package dev.viniciuscole.nudge.data

import dev.viniciuscole.nudge.data.model.SavedMeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MealSelectionTest {

    private val today = LocalDate.of(2026, 9, 23)
    private fun meal(id: Long, reminderId: Long, date: LocalDate) = SavedMeal(id, reminderId, "x", date, emptyList())

    @Test
    fun yesterdaysMealIsFoundToday() {
        val meals = listOf(meal(1, 7, today.minusDays(1)))
        assertEquals(1L, MealSelection.latest(meals, 7)!!.id)
    }

    @Test
    fun latestDateWinsEvenWithLowerId() {
        val meals = listOf(meal(9, 7, today.minusDays(3)), meal(2, 7, today))
        assertEquals(2L, MealSelection.latest(meals, 7)!!.id)
    }

    @Test
    fun sameDateHigherIdWins() {
        val meals = listOf(meal(3, 7, today), meal(5, 7, today))
        assertEquals(5L, MealSelection.latest(meals, 7)!!.id)
    }

    @Test
    fun otherRemindersAreIgnored() {
        val meals = listOf(meal(1, 8, today))
        assertNull(MealSelection.latest(meals, 7))
    }
}
