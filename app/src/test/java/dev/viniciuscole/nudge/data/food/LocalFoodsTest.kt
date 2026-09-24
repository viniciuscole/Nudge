package dev.viniciuscole.nudge.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFoodsTest {

    @Test
    fun accentlessQueryFindsAccentedFood() {
        assertTrue(LocalFoods.search("feijao").any { it.name == "Feijão carioca cozido" })
        assertTrue(LocalFoods.search("cafe").any { it.name.startsWith("Café") })
        assertTrue(LocalFoods.search("macarrao").any { it.name == "Macarrão cozido" })
    }

    @Test
    fun accentedQueryStillWorks() {
        assertTrue(LocalFoods.search("feijão").any { it.name == "Feijão carioca cozido" })
    }

    @Test
    fun searchIsCaseInsensitiveAndTrimmed() {
        assertTrue(LocalFoods.search("  LEITE  ").any { it.name == "Leite integral" })
    }

    @Test
    fun englishNamesStillSearchable() {
        assertTrue(LocalFoods.search("chicken").any { it.name == "Chicken breast" })
    }

    @Test
    fun blankQueryReturnsNothing() {
        assertTrue(LocalFoods.search("   ").isEmpty())
    }

    @Test
    fun resultsAreCapped() {
        assertEquals(4, LocalFoods.search("a", limit = 4).size)
    }

    @Test
    fun everyFoodHasSaneValues() {
        LocalFoods.ALL.forEach {
            assertTrue("${it.name}: kcal", it.kcal100 > 0.0 && it.kcal100 <= 900.0)
            assertTrue("${it.name}: macros", it.p100 >= 0 && it.c100 >= 0 && it.f100 >= 0)
            assertTrue("${it.name}: unidade", it.unit == "g" || it.unit == "ml")
            assertTrue("${it.name}: iniciais", it.initials.isNotBlank())
        }
    }

    @Test
    fun portionsAttachToCatalogItems() {
        assertEquals(Portion("pão", "pães", 50.0), LocalFoods.ALL.first { it.name == "Pão francês" }.portion)
        assertEquals(Portion("copo", "copos", 200.0), LocalFoods.ALL.first { it.name == "Leite integral" }.portion)
    }

    @Test
    fun everyDefinedPortionLandsOnACatalogItem() {
        assertEquals(31, LocalFoods.ALL.count { it.portion != null })
    }

    @Test
    fun englishItemsHaveNoPortion() {
        assertEquals(null, LocalFoods.ALL.first { it.name == "Egg" }.portion)
    }

    @Test
    fun portionForLooksUpByExactName() {
        assertEquals(45.0, LocalFoods.portionFor("Arroz branco cozido")!!.grams, 0.0)
        assertEquals(null, LocalFoods.portionFor("Quinoa"))
    }
}
