package dev.viniciuscole.nudge.data.food

import java.util.Locale
import kotlin.math.roundToInt

data class Portion(val singular: String, val plural: String, val grams: Double)

object PortionMath {
    const val MAX_QTY = 2000

    fun count(qty: Int, portion: Portion): Double = qty / portion.grams

    fun toQty(count: Double, portion: Portion): Int =
        (count * portion.grams).roundToInt().coerceIn(0, MAX_QTY)

    fun parseCount(text: String): Double? {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned == ".") return null
        return cleaned.toDoubleOrNull()?.takeIf { it >= 0.0 && !it.isNaN() && !it.isInfinite() }
    }

    fun format(count: Double): String {
        val tenths = (count * 10).roundToInt()
        val text = if (tenths % 10 == 0) (tenths / 10).toString() else String.format(Locale.ROOT, "%.1f", tenths / 10.0)
        return text.replace('.', ',')
    }

    fun noun(count: Double, portion: Portion): String =
        if ((count * 10).roundToInt() == 10) portion.singular else portion.plural

    fun label(count: Double, portion: Portion): String = "${format(count)} ${noun(count, portion)}"
}
