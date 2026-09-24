package dev.viniciuscole.nudge.data.model

import dev.viniciuscole.nudge.data.food.LocalFoods
import dev.viniciuscole.nudge.data.food.Portion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IngredientTest {

    private fun food(name: String) = LocalFoods.ALL.first { it.name == name }

    @Test
    fun foodWithPortionStartsAtOnePortion() {
        val ing = Ingredient.from(food("Pão francês"), 1)
        assertEquals(50, ing.qty)
        assertEquals(Portion("pão", "pães", 50.0), ing.portion)
    }

    @Test
    fun foodWithoutPortionKeepsOldDefaults() {
        assertEquals(100, Ingredient.from(food("Egg"), 1).qty)
        assertEquals(10, Ingredient.from(food("Olive oil"), 1).qty)
        assertNull(Ingredient.from(food("Egg"), 1).portion)
    }

    @Test
    fun legacyIngredientWithCatalogNameGetsPortion() {
        val legacy = Ingredient(1, "Arroz branco cozido", "g", 150, 128.0, 2.5, 28.1, 0.2)
        assertEquals(45.0, legacy.withInferredPortion().portion!!.grams, 0.0)
        assertEquals(150, legacy.withInferredPortion().qty)
    }

    @Test
    fun unknownNameStaysInGrams() {
        val usda = Ingredient(1, "Quinoa, cooked", "g", 120, 120.0, 4.4, 21.3, 1.9)
        assertNull(usda.withInferredPortion().portion)
    }

    @Test
    fun existingPortionIsNotOverwritten() {
        val custom = Portion("prato", "pratos", 200.0)
        val ing = Ingredient(1, "Arroz branco cozido", "g", 200, 128.0, 2.5, 28.1, 0.2, custom)
        assertEquals(custom, ing.withInferredPortion().portion)
    }
}
