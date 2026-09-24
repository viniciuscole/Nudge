package dev.viniciuscole.nudge.ui.add

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftTest {

    private val lunch = Reminder(3, ReminderType.MEAL, "Almoço", enabled = true, insistent = true, hour = 12, minute = 30)
    private val water = Reminder(4, ReminderType.WATER, "Água", enabled = true, insistent = false, startHour = 7, endHour = 21, intervalMin = 45)

    @Test
    fun mealRoundTripsThroughDraft() {
        assertEquals(lunch, Draft.from(lunch).toReminder(lunch.id, lunch.enabled, "Refeição", "Beber água"))
    }

    @Test
    fun waterRoundTripsThroughDraft() {
        assertEquals(water, Draft.from(water).toReminder(water.id, water.enabled, "Refeição", "Beber água"))
    }

    @Test
    fun editingKeepsDisabledState() {
        val off = lunch.copy(enabled = false)
        assertFalse(Draft.from(off).toReminder(off.id, off.enabled, "Refeição", "Beber água").enabled)
    }

    @Test
    fun nonPresetIntervalMarksCustom() {
        assertTrue(Draft.from(water).customInterval)
        assertFalse(Draft.from(water.copy(intervalMin = 90)).customInterval)
    }

    @Test
    fun blankLabelFallsBackByTypeAndIntervalHasAFloor() {
        val draft = Draft(type = ReminderType.WATER, label = "  ", intervalMin = 2)
        val r = draft.toReminder(9, true, "Refeição", "Beber água")
        assertEquals("Beber água", r.label)
        assertEquals(5, r.intervalMin)
    }
}
