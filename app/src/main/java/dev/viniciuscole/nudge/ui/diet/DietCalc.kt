package dev.viniciuscole.nudge.ui.diet

import dev.viniciuscole.nudge.data.KcalGoal
import dev.viniciuscole.nudge.data.MealSelection
import dev.viniciuscole.nudge.data.model.MealTotals
import dev.viniciuscole.nudge.data.model.Nutrition
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.SavedMeal

data class DietMealRow(
    val reminderId: Long,
    val label: String,
    val hour: Int,
    val minute: Int,
    val totals: MealTotals,
    val empty: Boolean,
)

data class DietUiState(
    val rows: List<DietMealRow> = emptyList(),
    val totals: MealTotals = Nutrition.totals(emptyList()),
    val goal: Int = KcalGoal.DEFAULT,
) {
    val progress: Float get() = if (goal <= 0) 0f else (totals.kcal.toFloat() / goal).coerceIn(0f, 1f)
}

object DietCalc {
    fun build(reminders: List<Reminder>, meals: List<SavedMeal>, goal: Int): DietUiState {
        val perMeal = reminders
            .filter { it.isMeal && it.enabled }
            .sortedBy { it.hour * 60 + it.minute }
            .map { r -> r to MealSelection.latest(meals, r.id)?.ingredients.orEmpty() }
        val rows = perMeal.map { (r, ingredients) ->
            DietMealRow(r.id, r.label, r.hour, r.minute, Nutrition.totals(ingredients), ingredients.isEmpty())
        }
        return DietUiState(rows, Nutrition.totals(perMeal.flatMap { it.second }), goal)
    }
}
