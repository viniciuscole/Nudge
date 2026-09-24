package dev.viniciuscole.nudge.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KcalGoalTest {

    @Test
    fun parsesPlainAndGroupedNumbers() {
        assertEquals(2000, KcalGoal.parse("2000"))
        assertEquals(1800, KcalGoal.parse("1.800"))
        assertEquals(2500, KcalGoal.parse(" 2 500 "))
    }

    @Test
    fun rejectsEmptyZeroAndGarbage() {
        assertNull(KcalGoal.parse(""))
        assertNull(KcalGoal.parse("abc"))
        assertNull(KcalGoal.parse("0"))
    }

    @Test
    fun clampsToLimits() {
        assertEquals(6000, KcalGoal.parse("99999"))
        assertEquals(500, KcalGoal.parse("100"))
        assertEquals(500, KcalGoal.clamp(-5))
    }
}
