package dev.viniciuscole.nudge.ui.builder

import dev.viniciuscole.nudge.data.food.PortionMath
import dev.viniciuscole.nudge.data.model.Ingredient

object IngredientEdits {

    fun setCount(ing: Ingredient, text: String): Ingredient {
        val portion = ing.portion ?: return ing
        val count = if (text.isBlank()) 0.0 else PortionMath.parseCount(text) ?: return ing
        return ing.copy(qty = PortionMath.toQty(count, portion))
    }

    fun setGrams(ing: Ingredient, text: String): Ingredient {
        val v = text.filter(Char::isDigit).take(4).toIntOrNull() ?: 0
        return ing.copy(qty = v.coerceIn(0, PortionMath.MAX_QTY))
    }

    fun plus(ing: Ingredient): Ingredient {
        val portion = ing.portion ?: return ing.copy(qty = (ing.qty + ing.step).coerceAtMost(PortionMath.MAX_QTY))
        return ing.copy(qty = PortionMath.toQty(PortionMath.count(ing.qty, portion) + 1.0, portion))
    }

    fun minus(ing: Ingredient): Ingredient {
        val portion = ing.portion ?: return ing.copy(qty = (ing.qty - ing.step).coerceAtLeast(0))
        return ing.copy(qty = PortionMath.toQty((PortionMath.count(ing.qty, portion) - 1.0).coerceAtLeast(0.0), portion))
    }
}
