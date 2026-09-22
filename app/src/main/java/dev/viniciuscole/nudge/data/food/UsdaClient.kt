package dev.viniciuscole.nudge.data.food

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class UsdaClient(private val apiKey: String) {

    val enabled: Boolean get() = apiKey.isNotBlank()

    suspend fun search(query: String, pageSize: Int = 8): List<FoodItem> = withContext(Dispatchers.IO) {
        if (!enabled || query.isBlank()) return@withContext emptyList()
        val url = URL(
            "https://api.nal.usda.gov/fdc/v1/foods/search" +
                "?api_key=" + URLEncoder.encode(apiKey, "UTF-8") +
                "&query=" + URLEncoder.encode(query.trim(), "UTF-8") +
                "&pageSize=$pageSize&dataType=Foundation,SR%20Legacy",
        )
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            if (conn.responseCode != 200) return@withContext emptyList()
            parse(conn.inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        // FoodData Central nutrient numbers: 208 Energy kcal, 957/958 Atwater energy, 203 protein, 205 carbs, 204 fat
        private val ENERGY = listOf("208", "957", "958")
        private const val PROTEIN = "203"
        private const val CARBS = "205"
        private const val FAT = "204"

        fun parse(json: String): List<FoodItem> {
            val foods: JSONArray = try {
                JSONObject(json).optJSONArray("foods") ?: return emptyList()
            } catch (e: Exception) {
                return emptyList()
            }
            return (0 until foods.length()).mapNotNull { i ->
                val f = foods.optJSONObject(i) ?: return@mapNotNull null
                val nutrients = f.optJSONArray("foodNutrients") ?: JSONArray()
                val kcal = ENERGY.firstNotNullOfOrNull { n -> nutrient(nutrients, n) } ?: return@mapNotNull null
                if (kcal <= 0.0) return@mapNotNull null
                FoodItem(
                    name = prettify(f.optString("description")),
                    unit = "g",
                    kcal100 = kcal,
                    p100 = nutrient(nutrients, PROTEIN) ?: 0.0,
                    c100 = nutrient(nutrients, CARBS) ?: 0.0,
                    f100 = nutrient(nutrients, FAT) ?: 0.0,
                    source = FoodItem.SOURCE_USDA,
                )
            }
        }

        private fun nutrient(arr: JSONArray, number: String): Double? {
            for (k in 0 until arr.length()) {
                val o = arr.optJSONObject(k) ?: continue
                if (o.optString("nutrientNumber") == number) return o.optDouble("value").takeUnless { it.isNaN() }
            }
            return null
        }

        private fun prettify(raw: String): String =
            raw.trim().lowercase().replaceFirstChar { it.uppercase() }
    }
}
