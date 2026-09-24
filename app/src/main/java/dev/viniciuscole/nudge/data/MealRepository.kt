package dev.viniciuscole.nudge.data

import android.content.Context
import dev.viniciuscole.nudge.data.json.MealJson
import dev.viniciuscole.nudge.data.model.SavedMeal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class MealRepository(context: Context) {

    private val prefs = context.getSharedPreferences("meals", Context.MODE_PRIVATE)
    private val _meals = MutableStateFlow(loadOrReset())
    val meals: StateFlow<List<SavedMeal>> = _meals.asStateFlow()

    private fun loadOrReset(): List<SavedMeal> = try {
        MealJson.decode(prefs.getString(KEY, null))
    } catch (e: Exception) {
        prefs.edit()
            .putString("${KEY}_unreadable_${System.currentTimeMillis()}", prefs.getString(KEY, null))
            .remove(KEY)
            .commit()
        emptyList()
    }

    fun nextId(): Long {
        val seed = maxOf(prefs.getLong(KEY_SEQ, 0L), _meals.value.maxOfOrNull { it.id } ?: 0L)
        val next = seed + 1
        prefs.edit().putLong(KEY_SEQ, next).apply()
        return next
    }

    fun forReminderOn(reminderId: Long, date: LocalDate): SavedMeal? =
        _meals.value.lastOrNull { it.reminderId == reminderId && it.date == date }

    fun save(meal: SavedMeal) {
        val cur = _meals.value
        write(if (cur.any { it.id == meal.id }) cur.map { if (it.id == meal.id) meal else it } else cur + meal)
    }

    private fun write(list: List<SavedMeal>) {
        _meals.value = list
        prefs.edit().putString(KEY, MealJson.encode(list)).apply()
    }

    private companion object {
        const val KEY = "list"
        const val KEY_SEQ = "seq"
    }
}
