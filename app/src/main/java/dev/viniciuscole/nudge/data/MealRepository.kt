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
    private val _meals = MutableStateFlow(MealJson.decode(prefs.getString(KEY, null)))
    val meals: StateFlow<List<SavedMeal>> = _meals.asStateFlow()

    fun nextId(): Long = (_meals.value.maxOfOrNull { it.id } ?: 0L) + 1

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
    }
}
