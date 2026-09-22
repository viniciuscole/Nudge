package dev.viniciuscole.nudge.data.json

import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.SavedMeal
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object MealJson {

    fun encode(list: List<SavedMeal>): String {
        val arr = JSONArray()
        list.forEach { arr.put(mealToJson(it)) }
        return arr.toString()
    }

    fun decode(raw: String?): List<SavedMeal> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { mealFromJson(arr.getJSONObject(it)) }
    }

    private fun mealToJson(m: SavedMeal): JSONObject = JSONObject()
        .put("id", m.id)
        .put("reminderId", m.reminderId)
        .put("label", m.label)
        .put("date", m.date.toString())
        .put("ingredients", JSONArray().also { arr -> m.ingredients.forEach { arr.put(ingredientToJson(it)) } })

    private fun mealFromJson(o: JSONObject): SavedMeal {
        val ing = o.optJSONArray("ingredients") ?: JSONArray()
        return SavedMeal(
            id = o.getLong("id"),
            reminderId = o.optLong("reminderId", -1L),
            label = o.optString("label", ""),
            date = LocalDate.parse(o.getString("date")),
            ingredients = (0 until ing.length()).map { ingredientFromJson(ing.getJSONObject(it)) },
        )
    }

    private fun ingredientToJson(i: Ingredient): JSONObject = JSONObject()
        .put("id", i.id)
        .put("name", i.name)
        .put("unit", i.unit)
        .put("qty", i.qty)
        .put("kcal100", i.kcal100)
        .put("p100", i.p100)
        .put("c100", i.c100)
        .put("f100", i.f100)

    private fun ingredientFromJson(o: JSONObject): Ingredient = Ingredient(
        id = o.getLong("id"),
        name = o.getString("name"),
        unit = o.optString("unit", "g"),
        qty = o.optInt("qty", 100),
        kcal100 = o.optDouble("kcal100", 0.0),
        p100 = o.optDouble("p100", 0.0),
        c100 = o.optDouble("c100", 0.0),
        f100 = o.optDouble("f100", 0.0),
    )
}
