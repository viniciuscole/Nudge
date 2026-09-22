package dev.viniciuscole.nudge.data.food

class FoodSearch(private val usda: UsdaClient) {

    suspend fun search(query: String, limit: Int = 8): List<FoodItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val local = LocalFoods.search(q)
        if (!usda.enabled) return local
        return merge(local, usda.search(q), limit)
    }

    companion object {
        fun merge(local: List<FoodItem>, remote: List<FoodItem>, limit: Int): List<FoodItem> {
            val seen = local.map { it.name.lowercase() }.toMutableSet()
            return (local + remote.filter { seen.add(it.name.lowercase()) }).take(limit)
        }
    }
}
