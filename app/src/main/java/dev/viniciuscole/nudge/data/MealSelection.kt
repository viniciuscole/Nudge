package dev.viniciuscole.nudge.data

import dev.viniciuscole.nudge.data.model.SavedMeal

object MealSelection {
    fun latest(meals: List<SavedMeal>, reminderId: Long): SavedMeal? =
        meals.filter { it.reminderId == reminderId }
            .maxWithOrNull(compareBy<SavedMeal>({ it.date }, { it.id }))
}
