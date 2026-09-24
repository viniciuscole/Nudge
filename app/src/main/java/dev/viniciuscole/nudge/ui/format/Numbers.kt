package dev.viniciuscole.nudge.ui.format

import java.text.NumberFormat
import java.util.Locale

object Numbers {
    fun grouped(v: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(v)
}
