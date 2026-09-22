package dev.viniciuscole.nudge.data.model

import dev.viniciuscole.nudge.data.food.FoodItem
import java.time.LocalDate
import kotlin.math.roundToInt

data class Ingredient(
    val id: Long,
    val name: String,
    val unit: String,
    val qty: Int,
    val kcal100: Double,
    val p100: Double,
    val c100: Double,
    val f100: Double,
) {
    val kcal: Double get() = kcal100 * qty / 100.0
    val proteinG: Double get() = p100 * qty / 100.0
    val carbsG: Double get() = c100 * qty / 100.0
    val fatG: Double get() = f100 * qty / 100.0
    val step: Int get() = if (unit == "ml") 5 else 10

    companion object {
        fun from(food: FoodItem, id: Long): Ingredient = Ingredient(
            id = id,
            name = food.name,
            unit = food.unit,
            qty = if (food.unit == "ml") 10 else 100,
            kcal100 = food.kcal100,
            p100 = food.p100,
            c100 = food.c100,
            f100 = food.f100,
        )
    }
}

data class MealTotals(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val proteinPct: Int,
    val carbsPct: Int,
    val fatPct: Int,
)

object Nutrition {

    fun totals(list: List<Ingredient>): MealTotals {
        val p = list.sumOf { it.proteinG }
        val c = list.sumOf { it.carbsG }
        val f = list.sumOf { it.fatG }
        val fromMacros = p * 4 + c * 4 + f * 9
        fun pct(v: Double) = if (fromMacros > 0) (v / fromMacros * 100).roundToInt() else 0
        return MealTotals(
            kcal = list.sumOf { it.kcal }.roundToInt(),
            proteinG = p.roundToInt(),
            carbsG = c.roundToInt(),
            fatG = f.roundToInt(),
            proteinPct = pct(p * 4),
            carbsPct = pct(c * 4),
            fatPct = pct(f * 9),
        )
    }

    fun dayShare(kcal: Int, dayGoal: Int = 1800): Int = (kcal * 100.0 / dayGoal).roundToInt()
}

data class SavedMeal(
    val id: Long,
    val reminderId: Long,
    val label: String,
    val date: LocalDate,
    val ingredients: List<Ingredient>,
)
