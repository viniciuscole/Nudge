package dev.viniciuscole.nudge.ui.builder

import dev.viniciuscole.nudge.data.food.Portion
import dev.viniciuscole.nudge.data.model.Ingredient
import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientEditsTest {

    private val bread = Ingredient(1, "Pão francês", "g", 50, 300.0, 8.0, 58.6, 3.1, Portion("pão", "pães", 50.0))
    private val rice = Ingredient(2, "Rice, cooked", "g", 100, 130.0, 2.7, 28.0, 0.3)
    private val milk = Ingredient(3, "Whole milk", "ml", 10, 61.0, 2.9, 4.3, 3.2)

    @Test
    fun countTextSetsQtyInPortions() {
        assertEquals(100, IngredientEdits.setCount(bread, "2").qty)
        assertEquals(25, IngredientEdits.setCount(bread, "0,5").qty)
        assertEquals(75, IngredientEdits.setCount(bread, "1.5").qty)
    }

    @Test
    fun partialDecimalKeepsTheParsedPart() {
        assertEquals(50, IngredientEdits.setCount(bread, "1,").qty)
    }

    @Test
    fun unparseableTextLeavesQtyUnchanged() {
        assertEquals(50, IngredientEdits.setCount(bread, ",").qty)
    }

    @Test
    fun emptyTextMeansZero() {
        assertEquals(0, IngredientEdits.setCount(bread, "").qty)
    }

    @Test
    fun plusAndMinusMoveOnePortion() {
        assertEquals(100, IngredientEdits.plus(bread).qty)
        assertEquals(0, IngredientEdits.minus(bread).qty)
        assertEquals(0, IngredientEdits.minus(IngredientEdits.minus(bread)).qty)
        assertEquals(75, IngredientEdits.plus(IngredientEdits.setCount(bread, "0,5")).qty)
    }

    @Test
    fun gramsIngredientKeepsOldStepping() {
        assertEquals(110, IngredientEdits.plus(rice).qty)
        assertEquals(90, IngredientEdits.minus(rice).qty)
        assertEquals(15, IngredientEdits.plus(milk).qty)
        assertEquals(250, IngredientEdits.setGrams(rice, "250").qty)
        assertEquals(0, IngredientEdits.setGrams(rice, "").qty)
        assertEquals(2000, IngredientEdits.setGrams(rice, "9999").qty)
    }
}
