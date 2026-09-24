package dev.viniciuscole.nudge.data.json

import dev.viniciuscole.nudge.data.food.Portion
import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.SavedMeal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MealJsonTest {

    @Test
    fun readsTheFormatAlreadyStoredOnDevices() {
        val stored = """[{"id":3,"reminderId":1,"label":"Almoço","date":"2026-09-22","ingredients":[""" +
            """{"id":1,"name":"Arroz branco cozido","unit":"g","qty":150,"kcal100":128.0,"p100":2.5,"c100":28.1,"f100":0.2},""" +
            """{"id":2,"name":"Leite integral","unit":"ml","qty":200,"kcal100":61.0,"p100":2.9,"c100":4.3,"f100":3.2}]}]"""
        val expected = listOf(
            SavedMeal(
                id = 3,
                reminderId = 1,
                label = "Almoço",
                date = LocalDate.of(2026, 9, 22),
                ingredients = listOf(
                    Ingredient(1, "Arroz branco cozido", "g", 150, 128.0, 2.5, 28.1, 0.2),
                    Ingredient(2, "Leite integral", "ml", 200, 61.0, 2.9, 4.3, 3.2),
                ),
            ),
        )
        assertEquals(expected, MealJson.decode(stored))
    }

    @Test
    fun roundTripPreservesEveryField() {
        val meals = MealJson.decode(MealJson.encode(listOf(
            SavedMeal(9, 4, "Jantar", LocalDate.of(2026, 1, 31), listOf(Ingredient(5, "Ovo cozido", "g", 100, 146.0, 13.3, 0.6, 9.5))),
        )))
        assertEquals(1, meals.size)
        assertEquals("Ovo cozido", meals[0].ingredients.single().name)
        assertEquals(LocalDate.of(2026, 1, 31), meals[0].date)
    }

    @Test
    fun ingredientWithPortionRoundTrips() {
        val bread = Portion("pão", "pães", 50.0)
        val meal = SavedMeal(1, 2, "Café", LocalDate.of(2026, 9, 23), listOf(Ingredient(1, "Pão francês", "g", 100, 300.0, 8.0, 58.6, 3.1, bread)))
        assertEquals(bread, MealJson.decode(MealJson.encode(listOf(meal))).single().ingredients.single().portion)
    }

    @Test
    fun legacyIngredientDecodesWithoutPortion() {
        val stored = """[{"id":1,"reminderId":1,"label":"x","date":"2026-09-22","ingredients":[{"id":1,"name":"Pão francês","unit":"g","qty":50,"kcal100":300.0,"p100":8.0,"c100":58.6,"f100":3.1}]}]"""
        assertEquals(null, MealJson.decode(stored).single().ingredients.single().portion)
    }

    @Test
    fun partialOrZeroPortionIsIgnored() {
        val noPlural = """[{"id":1,"reminderId":1,"label":"x","date":"2026-09-22","ingredients":[{"id":1,"name":"a","unit":"g","qty":50,"kcal100":1.0,"p100":0.0,"c100":0.0,"f100":0.0,"portionSingular":"pão","portionGrams":50.0}]}]"""
        val zero = """[{"id":1,"reminderId":1,"label":"x","date":"2026-09-22","ingredients":[{"id":1,"name":"a","unit":"g","qty":50,"kcal100":1.0,"p100":0.0,"c100":0.0,"f100":0.0,"portionSingular":"pão","portionPlural":"pães","portionGrams":0.0}]}]"""
        assertEquals(null, MealJson.decode(noPlural).single().ingredients.single().portion)
        assertEquals(null, MealJson.decode(zero).single().ingredients.single().portion)
    }
}
