package dev.viniciuscole.nudge.data.food

object LocalFoods {
    private fun g(name: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(name, "g", kcal, p, c, f, FoodItem.SOURCE_LOCAL)

    private fun ml(name: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(name, "ml", kcal, p, c, f, FoodItem.SOURCE_LOCAL)

    val ALL: List<FoodItem> = listOf(
        g("Chicken breast", 165.0, 31.0, 0.0, 3.6),
        g("Chicken thigh", 209.0, 26.0, 0.0, 11.0),
        g("Chickpeas, cooked", 164.0, 8.9, 27.0, 2.6),
        g("Rice, cooked", 130.0, 2.7, 28.0, 0.3),
        ml("Olive oil", 884.0, 0.0, 0.0, 100.0),
        g("Avocado", 160.0, 2.0, 9.0, 15.0),
        g("Greek yogurt", 59.0, 10.0, 3.6, 0.4),
        g("Egg", 155.0, 13.0, 1.1, 11.0),
        g("Broccoli", 34.0, 2.8, 7.0, 0.4),
        g("Almonds", 579.0, 21.0, 22.0, 50.0),
        g("Salmon", 208.0, 20.0, 0.0, 13.0),
        g("Cherry tomatoes", 18.0, 0.9, 3.9, 0.2),
        g("Spinach", 23.0, 2.9, 3.6, 0.4),
        g("Wholegrain bread", 247.0, 13.0, 41.0, 3.4),
        g("Feta", 264.0, 14.0, 4.1, 21.0),
    )

    fun search(query: String, limit: Int = 4): List<FoodItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return ALL.filter { it.name.lowercase().contains(q) }.take(limit)
    }
}
