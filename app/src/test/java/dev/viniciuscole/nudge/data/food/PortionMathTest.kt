package dev.viniciuscole.nudge.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortionMathTest {

    private val bread = Portion("pão", "pães", 50.0)
    private val oil = Portion("colher de sopa", "colheres de sopa", 13.0)

    @Test
    fun countIsQtyOverPortionGrams() {
        assertEquals(2.0, PortionMath.count(100, bread), 0.0001)
        assertEquals(0.5, PortionMath.count(25, bread), 0.0001)
    }

    @Test
    fun toQtyRoundsHalfUp() {
        assertEquals(20, PortionMath.toQty(1.5, oil))
        assertEquals(50, PortionMath.toQty(1.0, bread))
    }

    @Test
    fun toQtyIsClampedToRange() {
        assertEquals(2000, PortionMath.toQty(100.0, bread))
        assertEquals(0, PortionMath.toQty(0.0, bread))
    }

    @Test
    fun parseCountAcceptsCommaAndDot() {
        assertEquals(0.5, PortionMath.parseCount("0,5")!!, 0.0001)
        assertEquals(1.5, PortionMath.parseCount("1.5")!!, 0.0001)
        assertEquals(2.0, PortionMath.parseCount(" 2 ")!!, 0.0001)
        assertEquals(1.0, PortionMath.parseCount("1,")!!, 0.0001)
    }

    @Test
    fun parseCountRejectsGarbage() {
        assertNull(PortionMath.parseCount(""))
        assertNull(PortionMath.parseCount(","))
        assertNull(PortionMath.parseCount("abc"))
        assertNull(PortionMath.parseCount("-1"))
        assertNull(PortionMath.parseCount("1,2,3"))
    }

    @Test
    fun formatUsesOneDecimalCommaAndNoTrailingZero() {
        assertEquals("2", PortionMath.format(2.0))
        assertEquals("1,5", PortionMath.format(1.5))
        assertEquals("0,5", PortionMath.format(0.5))
        assertEquals("0,3", PortionMath.format(0.333))
        assertEquals("1", PortionMath.format(1.04))
    }

    @Test
    fun nounIsSingularOnlyForExactlyOne() {
        assertEquals("pão", PortionMath.noun(1.0, bread))
        assertEquals("pães", PortionMath.noun(2.0, bread))
        assertEquals("pães", PortionMath.noun(0.5, bread))
        assertEquals("pães", PortionMath.noun(0.0, bread))
    }

    @Test
    fun labelJoinsCountAndNoun() {
        assertEquals("2 pães", PortionMath.label(2.0, bread))
        assertEquals("1,5 colheres de sopa", PortionMath.label(1.5, oil))
    }
}
