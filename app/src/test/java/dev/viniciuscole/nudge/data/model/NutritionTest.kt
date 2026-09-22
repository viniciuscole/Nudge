package dev.viniciuscole.nudge.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionTest {

    private val meal = listOf(
        Ingredient(11, "Chicken breast", "g", 150, 165.0, 31.0, 0.0, 3.6),
        Ingredient(12, "Rice, cooked", "g", 180, 130.0, 2.7, 28.0, 0.3),
        Ingredient(13, "Broccoli", "g", 120, 34.0, 2.8, 7.0, 0.4),
        Ingredient(14, "Olive oil", "ml", 10, 884.0, 0.0, 0.0, 100.0),
    )

    @Test
    fun totalsMatchDesignSample() {
        val t = Nutrition.totals(meal)
        assertEquals(611, t.kcal)
        assertEquals(55, t.proteinG)
        assertEquals(59, t.carbsG)
        assertEquals(16, t.fatG)
        assertEquals(36, t.proteinPct)
        assertEquals(39, t.carbsPct)
        assertEquals(25, t.fatPct)
    }

    @Test
    fun emptyMealIsAllZero() {
        assertEquals(MealTotals(0, 0, 0, 0, 0, 0, 0), Nutrition.totals(emptyList()))
    }

    @Test
    fun ingredientKcalScalesWithQuantity() {
        assertEquals(247.5, meal[0].kcal, 0.001)
        assertEquals(88.4, meal[3].kcal, 0.001)
    }

    @Test
    fun dayShareIsRoundedPercentOfGoal() {
        assertEquals(34, Nutrition.dayShare(611))
        assertEquals(0, Nutrition.dayShare(0))
    }
}
