package dev.viniciuscole.nudge.data.food

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodSearchTest {

    private fun food(name: String, source: String) =
        FoodItem(name, "g", 100.0, 1.0, 1.0, 1.0, source)

    @Test
    fun localResultsComeFirstAndSurviveTheCap() {
        val local = List(3) { food("local$it", FoodItem.SOURCE_LOCAL) }
        val remote = List(10) { food("remote$it", FoodItem.SOURCE_USDA) }
        val merged = FoodSearch.merge(local, remote, limit = 4)
        assertEquals(4, merged.size)
        assertEquals(listOf("local0", "local1", "local2", "remote0"), merged.map { it.name })
    }

    @Test
    fun remoteDuplicatesOfLocalNamesAreDropped() {
        val local = listOf(food("Egg", FoodItem.SOURCE_LOCAL))
        val remote = listOf(food("egg", FoodItem.SOURCE_USDA), food("Salmon", FoodItem.SOURCE_USDA))
        val merged = FoodSearch.merge(local, remote, limit = 8)
        assertEquals(listOf("Egg", "Salmon"), merged.map { it.name })
    }

    @Test
    fun remoteDuplicatesOfEachOtherAreDropped() {
        val merged = FoodSearch.merge(
            emptyList(),
            listOf(food("Tofu", FoodItem.SOURCE_USDA), food("TOFU", FoodItem.SOURCE_USDA)),
            limit = 8,
        )
        assertEquals(1, merged.size)
    }

    @Test
    fun blankQueryReturnsNothingWithoutTouchingTheNetwork() = runTest {
        val search = FoodSearch(UsdaClient(""))
        assertTrue(search.search("   ").isEmpty())
    }

    @Test
    fun disabledClientReturnsLocalResultsOnly() = runTest {
        val search = FoodSearch(UsdaClient(""))
        val results = search.search("chicken")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.source == FoodItem.SOURCE_LOCAL })
    }
}
