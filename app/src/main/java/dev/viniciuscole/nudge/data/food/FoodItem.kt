package dev.viniciuscole.nudge.data.food

data class FoodItem(
    val name: String,
    val unit: String,
    val kcal100: Double,
    val p100: Double,
    val c100: Double,
    val f100: Double,
    val source: String,
) {
    val initials: String
        get() = name.replace(Regex("[^A-Za-z ]"), "").split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }

    companion object {
        const val SOURCE_LOCAL = "local"
        const val SOURCE_USDA = "usda"
    }
}
