package dev.viniciuscole.nudge.data.json

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderJsonTest {

    private val sample = listOf(
        Reminder(1, ReminderType.MEAL, "Breakfast", hour = 8, minute = 0),
        Reminder(2, ReminderType.WATER, "Drink water", enabled = false, insistent = false, startHour = 6, endHour = 23, intervalMin = 45),
    )

    @Test
    fun roundTripPreservesEveryField() {
        val decoded = ReminderJson.decode(ReminderJson.encode(sample))
        assertEquals(sample, decoded)
    }

    @Test
    fun nullOrBlankDecodesToEmpty() {
        assertTrue(ReminderJson.decode(null).isEmpty())
        assertTrue(ReminderJson.decode("  ").isEmpty())
    }

    @Test
    fun missingOptionalFieldsUseDefaults() {
        val decoded = ReminderJson.decode("""[{"id":7,"type":"MEAL","label":"Lunch"}]""")
        assertEquals(Reminder(7, ReminderType.MEAL, "Lunch"), decoded.single())
    }

    @Test
    fun waterSchedulingFieldsSurviveRoundTrip() {
        val water = ReminderJson.decode(ReminderJson.encode(sample)).first { it.type == ReminderType.WATER }
        assertEquals(6, water.startHour)
        assertEquals(23, water.endHour)
        assertEquals(45, water.intervalMin)
    }
}
