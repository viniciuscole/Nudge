package dev.viniciuscole.nudge.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsdaClientTest {

    private val sample = """
        {"foods":[
          {"fdcId":1,"description":"CHICKEN, BROILERS OR FRYERS, BREAST, MEAT ONLY, RAW",
           "foodNutrients":[
             {"nutrientNumber":"208","nutrientName":"Energy","unitName":"KCAL","value":120.0},
             {"nutrientNumber":"203","nutrientName":"Protein","unitName":"G","value":22.5},
             {"nutrientNumber":"204","nutrientName":"Total lipid (fat)","unitName":"G","value":2.6},
             {"nutrientNumber":"205","nutrientName":"Carbohydrate, by difference","unitName":"G","value":0.0}
           ]},
          {"fdcId":2,"description":"Olive oil",
           "foodNutrients":[
             {"nutrientNumber":"957","nutrientName":"Energy (Atwater General Factors)","unitName":"KCAL","value":884.0},
             {"nutrientNumber":"204","nutrientName":"Total lipid (fat)","unitName":"G","value":100.0}
           ]},
          {"fdcId":3,"description":"Water","foodNutrients":[]}
        ]}
    """.trimIndent()

    @Test
    fun parsesNutrientsPer100gAndCapitalizesName() {
        val items = UsdaClient.parse(sample)
        val chicken = items.first { it.name.startsWith("Chicken") }
        assertEquals("Chicken, broilers or fryers, breast, meat only, raw", chicken.name)
        assertEquals(120.0, chicken.kcal100, 0.001)
        assertEquals(22.5, chicken.p100, 0.001)
        assertEquals(0.0, chicken.c100, 0.001)
        assertEquals(2.6, chicken.f100, 0.001)
        assertEquals("g", chicken.unit)
        assertEquals(FoodItem.SOURCE_USDA, chicken.source)
    }

    @Test
    fun fallsBackToAtwaterEnergyAndDropsFoodsWithoutEnergy() {
        val items = UsdaClient.parse(sample)
        assertEquals(2, items.size)
        assertEquals(884.0, items.first { it.name == "Olive oil" }.kcal100, 0.001)
        assertTrue(items.none { it.name == "Water" })
    }

    @Test
    fun malformedJsonYieldsEmptyList() {
        assertTrue(UsdaClient.parse("not json").isEmpty())
        assertTrue(UsdaClient.parse("{}").isEmpty())
    }
}
