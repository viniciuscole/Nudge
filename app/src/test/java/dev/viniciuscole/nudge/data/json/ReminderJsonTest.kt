package dev.viniciuscole.nudge.data.json

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderJsonTest {

    private val sample = listOf(
        Reminder(1, ReminderType.MEAL, "Breakfast", hour = 8, minute = 0),
        Reminder(2, ReminderType.WATER, "Drink water", enabled = false, insistent = false, startHour = 8, endHour = 22, intervalMin = 90),
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
}
