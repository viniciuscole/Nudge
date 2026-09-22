package dev.viniciuscole.nudge.data.food

class FoodSearch(private val usda: UsdaClient) {

    suspend fun search(query: String, limit: Int = 8): List<FoodItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val local = LocalFoods.search(q)
        if (!usda.enabled) return local
        val remote = usda.search(q)
        val seen = local.map { it.name.lowercase() }.toMutableSet()
        return (local + remote.filter { seen.add(it.name.lowercase()) }).take(limit)
    }
}
