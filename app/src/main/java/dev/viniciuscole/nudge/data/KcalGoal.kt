package dev.viniciuscole.nudge.data

object KcalGoal {
    const val DEFAULT = 1800
    const val MIN = 500
    const val MAX = 6000

    fun clamp(v: Int): Int = v.coerceIn(MIN, MAX)

    fun parse(text: String): Int? =
        text.filter(Char::isDigit).take(5).toIntOrNull()?.takeIf { it > 0 }?.let(::clamp)
}
