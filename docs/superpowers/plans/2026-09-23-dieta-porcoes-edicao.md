# Dieta fixa, porções, edição de lembretes e tela da dieta — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the daily diet persistent across days, enter food in household portions, edit reminders, set a calorie goal, and see the whole diet's totals on one screen — without losing any data already stored on the phone.

**Architecture:** All changes are additive. Portions are a display/entry layer over the unchanged gram quantity (`Ingredient.qty: Int`), persisted as three optional JSON fields. "Fixed meal" is a read change (`latestFor` instead of "today's"), not a data migration. New pure objects (`PortionMath`, `IngredientEdits`, `MealSelection`, `KcalGoal`, `DietCalc`, `Draft.from/toReminder`, `DayStats.withWater`) carry all logic so it is unit-testable without Android; ViewModels and screens stay thin.

**Tech Stack:** Kotlin 2.1.21, Jetpack Compose (BOM 2025.06.00) + Material 3, Navigation Compose, SharedPreferences + `org.json`, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-09-23-dieta-porcoes-edicao-design.md`

## Global Constraints

- **No data loss.** No migration and no rewrite of stored data. Only OPTIONAL fields may be added to stored JSON.
- `ReminderJsonTest.readsTheFormatAlreadyStoredOnDevices` and `MealJsonTest.readsTheFormatAlreadyStoredOnDevices` must pass **without any modification**. If either needs to change to pass, STOP and report — the design is wrong.
- `app/build.gradle.kts` `signingConfigs` block and `keystore/debug.keystore` must not change.
- `Ingredient.qty` stays `Int` grams/ml. Portions never change how calories/macros are computed.
- minSdk 26 / targetSdk 35 / compileSdk 35. No new Gradle dependencies.
- Every new user-visible string goes into BOTH `app/src/main/res/values/strings.xml` and `app/src/main/res/values-pt-rBR/strings.xml`, with identical keys and identical format specifiers.
- Code comments only for hacks — things impossible to understand without the comment (owner's rule). Everything else uncommented.
- Build/test commands, run from the worktree root, exactly in this form (no `export`, no wrapper scripts — the sandbox refuses them):
  `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
  `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew assembleDebug testDebugUnitTest`
- Worktree: `/home/vinia/vinicius/nudge/.claude/worktrees/diet-portions-edit`, branch `worktree-diet-portions-edit`. `local.properties` already present (gitignored).
- Commits: plain `git commit` (never `--author`, `-c user.name`, `-c user.email`); message ends with the line `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Suite before this plan: 38 tests passing.

## Review Focus

1. **Typing a partial decimal in a portion field** ("1," / "," / empty) — the typed text is kept, the quantity never jumps to a surprising value, empty means 0. Pinned by `IngredientEditsTest` in Task 4.
2. **Saved meals containing ingredients not in the catalog** (USDA items, the 15 English items) — they keep showing and editing in grams, unchanged. Pinned by `IngredientTest.unknownNameStaysInGrams` in Task 2 and `IngredientEditsTest.gramsIngredientKeepsOldStepping` in Task 4.
3. **Editing a reminder that is switched off** — saving must not switch it back on. Pinned by `DraftTest.editingKeepsDisabledState` in Task 5.
4. **Diet screen with no meal reminders, or only empty meals** — shows zeros, no division by zero, no NaN percentages. Pinned by `DietCalcTest.noMealsGivesZeros` and `emptyMealCountsZero` in Task 6.
5. **Undo on the water counter at 0 or right after midnight** — never negative; a new day starts from 0. Pinned by `DayStatsTest` in Task 7.

## File Structure

```
app/src/main/java/dev/viniciuscole/nudge/
  data/food/Portion.kt            NEW  Portion + PortionMath (pure)
  data/food/FoodItem.kt           MOD  + portion: Portion?
  data/food/LocalFoods.kt         MOD  + PORTIONS map, portionFor(name)
  data/model/Meal.kt              MOD  Ingredient + portion, from(), withInferredPortion()
  data/json/MealJson.kt           MOD  optional portion fields
  data/KcalGoal.kt                NEW  goal default/limits/parse (pure)
  data/SettingsRepository.kt      NEW  kcal goal persistence
  data/MealSelection.kt           NEW  latest meal per reminder (pure)
  data/MealRepository.kt          MOD  latestFor() replaces forReminderOn()
  data/model/DayStats.kt          MOD  + withWater()
  data/DayStatsRepository.kt      MOD  + addWater()
  NudgeApp.kt                     MOD  + settings
  alarm/Notifications.kt          MOD  + postGentle()
  alarm/AlarmReceiver.kt          MOD  uses Notifications.postGentle()
  alarm/ReminderPreview.kt        NEW  "test alarm" action
  ui/format/Numbers.kt            NEW  locale-grouped integers
  ui/components/MacroBreakdown.kt NEW  macro bar + legend (shared)
  ui/builder/IngredientEdits.kt   NEW  qty/portion edits (pure)
  ui/builder/MealBuilderViewModel.kt MOD
  ui/builder/MealBuilderScreen.kt MOD
  ui/add/Draft.kt                 NEW  Draft moved here + from()/toReminder()
  ui/add/AddReminderViewModel.kt  MOD  edit mode
  ui/add/AddReminderScreen.kt     MOD  edit mode UI
  ui/diet/DietCalc.kt             NEW  diet totals (pure)
  ui/diet/DietViewModel.kt        NEW
  ui/diet/DietScreen.kt           NEW
  ui/home/HomeViewModel.kt        MOD  water log/undo, no preview
  ui/home/HomeScreen.kt           MOD  tap map, snackbar, diet link
  ui/NudgeNavHost.kt              MOD  edit + diet routes
app/src/test/java/dev/viniciuscole/nudge/
  data/food/PortionMathTest.kt, data/model/IngredientTest.kt, data/KcalGoalTest.kt,
  data/MealSelectionTest.kt, ui/builder/IngredientEditsTest.kt, ui/add/DraftTest.kt,
  ui/diet/DietCalcTest.kt, data/model/DayStatsTest.kt, StringsParityTest.kt   NEW
  data/food/LocalFoodsTest.kt, data/json/MealJsonTest.kt, data/model/NutritionTest.kt  MOD (new tests only)
```

---

### Task 1: Portion model, portion math, catalog portions

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/food/Portion.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/data/food/FoodItem.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/data/food/LocalFoods.kt`
- Test: `app/src/test/java/dev/viniciuscole/nudge/data/food/PortionMathTest.kt` (new)
- Test: `app/src/test/java/dev/viniciuscole/nudge/data/food/LocalFoodsTest.kt` (add tests only)

**Interfaces:**
- Produces: `data class Portion(singular: String, plural: String, grams: Double)`; `object PortionMath { const val MAX_QTY = 2000; fun count(qty: Int, portion: Portion): Double; fun toQty(count: Double, portion: Portion): Int; fun parseCount(text: String): Double?; fun format(count: Double): String; fun noun(count: Double, portion: Portion): String; fun label(count: Double, portion: Portion): String }`; `FoodItem.portion: Portion?` (new last constructor param, default `null`); `LocalFoods.portionFor(name: String): Portion?`.

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/dev/viniciuscole/nudge/data/food/PortionMathTest.kt`:
```kotlin
package dev.viniciuscole.nudge.data.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortionMathTest {

    private val bread = Portion("pão", "pães", 50.0)
    private val oil = Portion("colher de sopa", "colheres de sopa", 13.0)

    @Test
    fun countIsQtyOverPortionGrams() {
        assertEquals(2.0, PortionMath.count(100, bread), 0.0001)
        assertEquals(0.5, PortionMath.count(25, bread), 0.0001)
    }

    @Test
    fun toQtyRoundsHalfUp() {
        assertEquals(20, PortionMath.toQty(1.5, oil))
        assertEquals(50, PortionMath.toQty(1.0, bread))
    }

    @Test
    fun toQtyIsClampedToRange() {
        assertEquals(2000, PortionMath.toQty(100.0, bread))
        assertEquals(0, PortionMath.toQty(0.0, bread))
    }

    @Test
    fun parseCountAcceptsCommaAndDot() {
        assertEquals(0.5, PortionMath.parseCount("0,5")!!, 0.0001)
        assertEquals(1.5, PortionMath.parseCount("1.5")!!, 0.0001)
        assertEquals(2.0, PortionMath.parseCount(" 2 ")!!, 0.0001)
        assertEquals(1.0, PortionMath.parseCount("1,")!!, 0.0001)
    }

    @Test
    fun parseCountRejectsGarbage() {
        assertNull(PortionMath.parseCount(""))
        assertNull(PortionMath.parseCount(","))
        assertNull(PortionMath.parseCount("abc"))
        assertNull(PortionMath.parseCount("-1"))
        assertNull(PortionMath.parseCount("1,2,3"))
    }

    @Test
    fun formatUsesOneDecimalCommaAndNoTrailingZero() {
        assertEquals("2", PortionMath.format(2.0))
        assertEquals("1,5", PortionMath.format(1.5))
        assertEquals("0,5", PortionMath.format(0.5))
        assertEquals("0,3", PortionMath.format(0.333))
        assertEquals("1", PortionMath.format(1.04))
    }

    @Test
    fun nounIsSingularOnlyForExactlyOne() {
        assertEquals("pão", PortionMath.noun(1.0, bread))
        assertEquals("pães", PortionMath.noun(2.0, bread))
        assertEquals("pães", PortionMath.noun(0.5, bread))
        assertEquals("pães", PortionMath.noun(0.0, bread))
    }

    @Test
    fun labelJoinsCountAndNoun() {
        assertEquals("2 pães", PortionMath.label(2.0, bread))
        assertEquals("1,5 colheres de sopa", PortionMath.label(1.5, oil))
    }
}
```

Append these tests inside the class in `app/src/test/java/dev/viniciuscole/nudge/data/food/LocalFoodsTest.kt` (do not change existing tests):
```kotlin
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: compilation FAILS with `Unresolved reference 'Portion'` / `'PortionMath'` / `'portion'`.

- [ ] **Step 3: Implement**

`app/src/main/java/dev/viniciuscole/nudge/data/food/Portion.kt`:
```kotlin
package dev.viniciuscole.nudge.data.food

import java.util.Locale
import kotlin.math.roundToInt

data class Portion(val singular: String, val plural: String, val grams: Double)

object PortionMath {
    const val MAX_QTY = 2000

    fun count(qty: Int, portion: Portion): Double = qty / portion.grams

    fun toQty(count: Double, portion: Portion): Int =
        (count * portion.grams).roundToInt().coerceIn(0, MAX_QTY)

    fun parseCount(text: String): Double? {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned == ".") return null
        return cleaned.toDoubleOrNull()?.takeIf { it >= 0.0 && !it.isNaN() && !it.isInfinite() }
    }

    fun format(count: Double): String {
        val tenths = (count * 10).roundToInt()
        val text = if (tenths % 10 == 0) (tenths / 10).toString() else String.format(Locale.ROOT, "%.1f", tenths / 10.0)
        return text.replace('.', ',')
    }

    fun noun(count: Double, portion: Portion): String =
        if ((count * 10).roundToInt() == 10) portion.singular else portion.plural

    fun label(count: Double, portion: Portion): String = "${format(count)} ${noun(count, portion)}"
}
```

`app/src/main/java/dev/viniciuscole/nudge/data/food/FoodItem.kt` — add one constructor parameter after `source`:
```kotlin
    val source: String,
    val portion: Portion? = null,
) {
```

`app/src/main/java/dev/viniciuscole/nudge/data/food/LocalFoods.kt` — three changes:

1. Replace the two builder functions with versions that attach the portion:
```kotlin
    private fun g(name: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(name, "g", kcal, p, c, f, FoodItem.SOURCE_LOCAL, PORTIONS[name])

    private fun ml(name: String, kcal: Double, p: Double, c: Double, f: Double) =
        FoodItem(name, "ml", kcal, p, c, f, FoodItem.SOURCE_LOCAL, PORTIONS[name])
```

2. Insert this map **directly after those two functions and BEFORE `val ALL`**:
```kotlin
    // Must stay above ALL: object properties initialise in declaration order, and ALL reads this map.
    private val PORTIONS: Map<String, Portion> = mapOf(
        "Pão francês" to Portion("pão", "pães", 50.0),
        "Pão de forma integral" to Portion("fatia", "fatias", 25.0),
        "Pão de queijo" to Portion("unidade", "unidades", 20.0),
        "Tapioca (goma)" to Portion("colher de sopa", "colheres de sopa", 15.0),
        "Aveia em flocos" to Portion("colher de sopa", "colheres de sopa", 15.0),
        "Arroz branco cozido" to Portion("colher de servir", "colheres de servir", 45.0),
        "Arroz integral cozido" to Portion("colher de servir", "colheres de servir", 45.0),
        "Feijão carioca cozido" to Portion("concha", "conchas", 80.0),
        "Feijão preto cozido" to Portion("concha", "conchas", 80.0),
        "Lentilha cozida" to Portion("concha", "conchas", 80.0),
        "Ovo cozido" to Portion("ovo", "ovos", 50.0),
        "Ovo frito" to Portion("ovo", "ovos", 50.0),
        "Leite integral" to Portion("copo", "copos", 200.0),
        "Leite desnatado" to Portion("copo", "copos", 200.0),
        "Iogurte natural" to Portion("pote", "potes", 170.0),
        "Iogurte grego" to Portion("pote", "potes", 100.0),
        "Queijo minas frescal" to Portion("fatia", "fatias", 30.0),
        "Queijo mussarela" to Portion("fatia", "fatias", 15.0),
        "Requeijão cremoso" to Portion("colher de sopa", "colheres de sopa", 30.0),
        "Manteiga" to Portion("colher de chá", "colheres de chá", 5.0),
        "Banana prata" to Portion("banana", "bananas", 70.0),
        "Maçã" to Portion("maçã", "maçãs", 130.0),
        "Laranja" to Portion("laranja", "laranjas", 150.0),
        "Azeite de oliva" to Portion("colher de sopa", "colheres de sopa", 13.0),
        "Óleo de soja" to Portion("colher de sopa", "colheres de sopa", 13.0),
        "Açúcar refinado" to Portion("colher de chá", "colheres de chá", 5.0),
        "Mel" to Portion("colher de sopa", "colheres de sopa", 20.0),
        "Castanha-do-pará" to Portion("unidade", "unidades", 4.0),
        "Whey protein (pó)" to Portion("scoop", "scoops", 30.0),
        "Café coado sem açúcar" to Portion("xícara", "xícaras", 50.0),
        "Suco de laranja natural" to Portion("copo", "copos", 200.0),
    )
```

3. Add below `fold(...)`:
```kotlin
    fun portionFor(name: String): Portion? = PORTIONS[name]
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL. `everyDefinedPortionLandsOnACatalogItem` returning 0 (or an NPE) means `PORTIONS` was placed below `ALL`; returning <31 means a name in the map does not match the catalog exactly — fix the name, not the expected count.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/viniciuscole/nudge/data/food app/src/test/java/dev/viniciuscole/nudge/data/food
git commit -m "feat: household portions for catalog foods

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Portion on stored ingredients (optional JSON fields) + inference

**Files:**
- Modify: `app/src/main/java/dev/viniciuscole/nudge/data/model/Meal.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/data/json/MealJson.kt`
- Test: `app/src/test/java/dev/viniciuscole/nudge/data/model/IngredientTest.kt` (new)
- Test: `app/src/test/java/dev/viniciuscole/nudge/data/json/MealJsonTest.kt` (add tests only — `readsTheFormatAlreadyStoredOnDevices` must stay byte-for-byte unchanged)

**Interfaces:**
- Consumes: `Portion`, `PortionMath.toQty`, `LocalFoods.portionFor` (Task 1).
- Produces: `Ingredient(..., portion: Portion? = null)` (new last param); `Ingredient.withInferredPortion(): Ingredient`; `Ingredient.from(food, id)` starts at one portion when the food has one.

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/dev/viniciuscole/nudge/data/model/IngredientTest.kt`:
```kotlin
package dev.viniciuscole.nudge.data.model

import dev.viniciuscole.nudge.data.food.LocalFoods
import dev.viniciuscole.nudge.data.food.Portion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IngredientTest {

    private fun food(name: String) = LocalFoods.ALL.first { it.name == name }

    @Test
    fun foodWithPortionStartsAtOnePortion() {
        val ing = Ingredient.from(food("Pão francês"), 1)
        assertEquals(50, ing.qty)
        assertEquals(Portion("pão", "pães", 50.0), ing.portion)
    }

    @Test
    fun foodWithoutPortionKeepsOldDefaults() {
        assertEquals(100, Ingredient.from(food("Egg"), 1).qty)
        assertEquals(10, Ingredient.from(food("Olive oil"), 1).qty)
        assertNull(Ingredient.from(food("Egg"), 1).portion)
    }

    @Test
    fun legacyIngredientWithCatalogNameGetsPortion() {
        val legacy = Ingredient(1, "Arroz branco cozido", "g", 150, 128.0, 2.5, 28.1, 0.2)
        assertEquals(45.0, legacy.withInferredPortion().portion!!.grams, 0.0)
        assertEquals(150, legacy.withInferredPortion().qty)
    }

    @Test
    fun unknownNameStaysInGrams() {
        val usda = Ingredient(1, "Quinoa, cooked", "g", 120, 120.0, 4.4, 21.3, 1.9)
        assertNull(usda.withInferredPortion().portion)
    }

    @Test
    fun existingPortionIsNotOverwritten() {
        val custom = Portion("prato", "pratos", 200.0)
        val ing = Ingredient(1, "Arroz branco cozido", "g", 200, 128.0, 2.5, 28.1, 0.2, custom)
        assertEquals(custom, ing.withInferredPortion().portion)
    }
}
```

Append inside the class in `MealJsonTest.kt` (new tests only; add the import `import dev.viniciuscole.nudge.data.food.Portion`):
```kotlin
    @Test
    fun ingredientWithPortionRoundTrips() {
        val bread = Portion("pão", "pães", 50.0)
        val meal = SavedMeal(1, 2, "Café", LocalDate.of(2026, 9, 23), listOf(Ingredient(1, "Pão francês", "g", 100, 300.0, 8.0, 58.6, 3.1, bread)))
        assertEquals(bread, MealJson.decode(MealJson.encode(listOf(meal))).single().ingredients.single().portion)
    }

    @Test
    fun legacyIngredientDecodesWithoutPortion() {
        val stored = """[{"id":1,"reminderId":1,"label":"x","date":"2026-09-22","ingredients":[{"id":1,"name":"Pão francês","unit":"g","qty":50,"kcal100":300.0,"p100":8.0,"c100":58.6,"f100":3.1}]}]"""
        assertEquals(null, MealJson.decode(stored).single().ingredients.single().portion)
    }

    @Test
    fun partialOrZeroPortionIsIgnored() {
        val noPlural = """[{"id":1,"reminderId":1,"label":"x","date":"2026-09-22","ingredients":[{"id":1,"name":"a","unit":"g","qty":50,"kcal100":1.0,"p100":0.0,"c100":0.0,"f100":0.0,"portionSingular":"pão","portionGrams":50.0}]}]"""
        val zero = """[{"id":1,"reminderId":1,"label":"x","date":"2026-09-22","ingredients":[{"id":1,"name":"a","unit":"g","qty":50,"kcal100":1.0,"p100":0.0,"c100":0.0,"f100":0.0,"portionSingular":"pão","portionPlural":"pães","portionGrams":0.0}]}]"""
        assertEquals(null, MealJson.decode(noPlural).single().ingredients.single().portion)
        assertEquals(null, MealJson.decode(zero).single().ingredients.single().portion)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: compilation FAILS (`withInferredPortion` / 9-arg `Ingredient` unresolved).

- [ ] **Step 3: Implement**

`data/model/Meal.kt` — `Ingredient` gets a last parameter and two changes; add imports `dev.viniciuscole.nudge.data.food.LocalFoods`, `dev.viniciuscole.nudge.data.food.Portion`, `dev.viniciuscole.nudge.data.food.PortionMath`:
```kotlin
data class Ingredient(
    val id: Long,
    val name: String,
    val unit: String,
    val qty: Int,
    val kcal100: Double,
    val p100: Double,
    val c100: Double,
    val f100: Double,
    val portion: Portion? = null,
) {
    val kcal: Double get() = kcal100 * qty / 100.0
    val proteinG: Double get() = p100 * qty / 100.0
    val carbsG: Double get() = c100 * qty / 100.0
    val fatG: Double get() = f100 * qty / 100.0
    val step: Int get() = if (unit == "ml") 5 else 10

    fun withInferredPortion(): Ingredient =
        if (portion != null) this else copy(portion = LocalFoods.portionFor(name))

    companion object {
        fun from(food: FoodItem, id: Long): Ingredient = Ingredient(
            id = id,
            name = food.name,
            unit = food.unit,
            qty = food.portion?.let { PortionMath.toQty(1.0, it) } ?: if (food.unit == "ml") 10 else 100,
            kcal100 = food.kcal100,
            p100 = food.p100,
            c100 = food.c100,
            f100 = food.f100,
            portion = food.portion,
        )
    }
}
```

`data/json/MealJson.kt` — add import `dev.viniciuscole.nudge.data.food.Portion`, then:
```kotlin
    private fun ingredientToJson(i: Ingredient): JSONObject = JSONObject()
        .put("id", i.id)
        .put("name", i.name)
        .put("unit", i.unit)
        .put("qty", i.qty)
        .put("kcal100", i.kcal100)
        .put("p100", i.p100)
        .put("c100", i.c100)
        .put("f100", i.f100)
        .also { o ->
            i.portion?.let { p -> o.put("portionSingular", p.singular).put("portionPlural", p.plural).put("portionGrams", p.grams) }
        }

    private fun ingredientFromJson(o: JSONObject): Ingredient = Ingredient(
        id = o.getLong("id"),
        name = o.getString("name"),
        unit = o.optString("unit", "g"),
        qty = o.optInt("qty", 100),
        kcal100 = o.optDouble("kcal100", 0.0),
        p100 = o.optDouble("p100", 0.0),
        c100 = o.optDouble("c100", 0.0),
        f100 = o.optDouble("f100", 0.0),
        portion = portionFromJson(o),
    )

    private fun portionFromJson(o: JSONObject): Portion? {
        if (!o.has("portionSingular") || !o.has("portionPlural") || !o.has("portionGrams")) return null
        val grams = o.optDouble("portionGrams", 0.0)
        if (grams.isNaN() || grams <= 0.0) return null
        return Portion(o.getString("portionSingular"), o.getString("portionPlural"), grams)
    }
```

`withInferredPortion()` is deliberately NOT called by `MealJson.decode` — decode must return exactly what is stored, or the frozen-format test breaks. Inference happens in the builder (Task 4).

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, including the untouched `readsTheFormatAlreadyStoredOnDevices` in both JSON test classes.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/viniciuscole/nudge/data app/src/test/java/dev/viniciuscole/nudge/data
git commit -m "feat: store an optional portion on ingredients

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Configurable calorie goal

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/KcalGoal.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/SettingsRepository.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/format/Numbers.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/NudgeApp.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/builder/MealBuilderViewModel.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/builder/MealBuilderScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-pt-rBR/strings.xml`
- Test: `app/src/test/java/dev/viniciuscole/nudge/data/KcalGoalTest.kt` (new); `NutritionTest.kt` (add one test)

**Interfaces:**
- Produces: `object KcalGoal { const val DEFAULT = 1800; const val MIN = 500; const val MAX = 6000; fun clamp(v: Int): Int; fun parse(text: String): Int? }`; `class SettingsRepository(context) { val kcalGoal: StateFlow<Int>; fun setKcalGoal(v: Int) }`; `NudgeApp.settings: SettingsRepository`; `object Numbers { fun grouped(v: Int): String }`; `BuilderUiState.kcalGoal: Int`; string `goal_note` now takes `(%1$d percent, %2$s formatted goal)`.

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/dev/viniciuscole/nudge/data/KcalGoalTest.kt`:
```kotlin
package dev.viniciuscole.nudge.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KcalGoalTest {

    @Test
    fun parsesPlainAndGroupedNumbers() {
        assertEquals(2000, KcalGoal.parse("2000"))
        assertEquals(1800, KcalGoal.parse("1.800"))
        assertEquals(2500, KcalGoal.parse(" 2 500 "))
    }

    @Test
    fun rejectsEmptyZeroAndGarbage() {
        assertNull(KcalGoal.parse(""))
        assertNull(KcalGoal.parse("abc"))
        assertNull(KcalGoal.parse("0"))
    }

    @Test
    fun clampsToLimits() {
        assertEquals(6000, KcalGoal.parse("99999"))
        assertEquals(500, KcalGoal.parse("100"))
        assertEquals(500, KcalGoal.clamp(-5))
    }
}
```

Append inside `NutritionTest`:
```kotlin
    @Test
    fun dayShareUsesTheGivenGoal() {
        assertEquals(45, Nutrition.dayShare(900, 2000))
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: compilation FAILS (`Unresolved reference 'KcalGoal'`). (`dayShareUsesTheGivenGoal` already compiles — `dayShare` has had a `dayGoal` parameter since the start; it will pass once the build compiles.)

- [ ] **Step 3: Implement**

`data/KcalGoal.kt`:
```kotlin
package dev.viniciuscole.nudge.data

object KcalGoal {
    const val DEFAULT = 1800
    const val MIN = 500
    const val MAX = 6000

    fun clamp(v: Int): Int = v.coerceIn(MIN, MAX)

    fun parse(text: String): Int? =
        text.filter(Char::isDigit).take(5).toIntOrNull()?.takeIf { it > 0 }?.let(::clamp)
}
```

`data/SettingsRepository.kt`:
```kotlin
package dev.viniciuscole.nudge.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _kcalGoal = MutableStateFlow(load())
    val kcalGoal: StateFlow<Int> = _kcalGoal.asStateFlow()

    fun setKcalGoal(v: Int) {
        val goal = KcalGoal.clamp(v)
        _kcalGoal.value = goal
        prefs.edit().putInt(KEY_GOAL, goal).apply()
    }

    private fun load(): Int = try {
        KcalGoal.clamp(prefs.getInt(KEY_GOAL, KcalGoal.DEFAULT))
    } catch (e: Exception) {
        KcalGoal.DEFAULT
    }

    private companion object {
        const val KEY_GOAL = "kcal_goal"
    }
}
```

`ui/format/Numbers.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.format

import java.text.NumberFormat
import java.util.Locale

object Numbers {
    fun grouped(v: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(v)
}
```

`NudgeApp.kt` — add below `val meals`:
```kotlin
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
```
with import `dev.viniciuscole.nudge.data.SettingsRepository`.

`MealBuilderViewModel.kt`:
- In `BuilderUiState` add the field `val kcalGoal: Int = KcalGoal.DEFAULT,` and change `dayShare` to `val dayShare: Int get() = Nutrition.dayShare(totals.kcal, kcalGoal)`. Import `dev.viniciuscole.nudge.data.KcalGoal`.
- At the end of `init { ... }` add:
```kotlin
        app.settings.kcalGoal
            .onEach { goal -> _state.update { it.copy(kcalGoal = goal) } }
            .launchIn(viewModelScope)
```

`MealBuilderScreen.kt`:
- Call site: `TotalsCard(s.totals, s.dayShare, s.kcalGoal)`.
- Signature: `private fun TotalsCard(t: MealTotals, dayShare: Int, kcalGoal: Int)`.
- The goal text: `stringResource(R.string.goal_note, dayShare, Numbers.grouped(kcalGoal))`, import `dev.viniciuscole.nudge.ui.format.Numbers`.

Strings — replace the existing `goal_note` line in each file:
- `values/strings.xml`: `<string name="goal_note">≈ %1$d%% of a %2$s kcal day</string>`
- `values-pt-rBR/strings.xml`: `<string name="goal_note">≈ %1$d%% de um dia de %2$s kcal</string>`

- [ ] **Step 4: Run tests and build**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: configurable daily calorie goal (default 1800)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Meal builder — fixed meal per reminder and portion entry

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/data/MealSelection.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/builder/IngredientEdits.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/data/MealRepository.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/builder/MealBuilderViewModel.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/builder/MealBuilderScreen.kt`
- Test: `app/src/test/java/dev/viniciuscole/nudge/data/MealSelectionTest.kt` (new), `app/src/test/java/dev/viniciuscole/nudge/ui/builder/IngredientEditsTest.kt` (new)

**Interfaces:**
- Consumes: `Portion`, `PortionMath` (Task 1); `Ingredient.portion`, `withInferredPortion()` (Task 2); `BuilderUiState.kcalGoal` (Task 3).
- Produces: `object MealSelection { fun latest(meals: List<SavedMeal>, reminderId: Long): SavedMeal? }`; `MealRepository.latestFor(reminderId: Long): SavedMeal?` (replaces `forReminderOn`, which is removed); `object IngredientEdits { fun setCount(ing: Ingredient, text: String): Ingredient; fun setGrams(ing: Ingredient, text: String): Ingredient; fun plus(ing: Ingredient): Ingredient; fun minus(ing: Ingredient): Ingredient }`; `MealBuilderViewModel.setCount(id: Long, text: String)`; `BuilderUiState.countText: Map<Long, String>`.

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/dev/viniciuscole/nudge/data/MealSelectionTest.kt`:
```kotlin
package dev.viniciuscole.nudge.data

import dev.viniciuscole.nudge.data.model.SavedMeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MealSelectionTest {

    private val today = LocalDate.of(2026, 9, 23)
    private fun meal(id: Long, reminderId: Long, date: LocalDate) = SavedMeal(id, reminderId, "x", date, emptyList())

    @Test
    fun yesterdaysMealIsFoundToday() {
        val meals = listOf(meal(1, 7, today.minusDays(1)))
        assertEquals(1L, MealSelection.latest(meals, 7)!!.id)
    }

    @Test
    fun latestDateWinsEvenWithLowerId() {
        val meals = listOf(meal(9, 7, today.minusDays(3)), meal(2, 7, today))
        assertEquals(2L, MealSelection.latest(meals, 7)!!.id)
    }

    @Test
    fun sameDateHigherIdWins() {
        val meals = listOf(meal(3, 7, today), meal(5, 7, today))
        assertEquals(5L, MealSelection.latest(meals, 7)!!.id)
    }

    @Test
    fun otherRemindersAreIgnored() {
        val meals = listOf(meal(1, 8, today))
        assertNull(MealSelection.latest(meals, 7))
    }
}
```

`app/src/test/java/dev/viniciuscole/nudge/ui/builder/IngredientEditsTest.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.builder

import dev.viniciuscole.nudge.data.food.Portion
import dev.viniciuscole.nudge.data.model.Ingredient
import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientEditsTest {

    private val bread = Ingredient(1, "Pão francês", "g", 50, 300.0, 8.0, 58.6, 3.1, Portion("pão", "pães", 50.0))
    private val rice = Ingredient(2, "Rice, cooked", "g", 100, 130.0, 2.7, 28.0, 0.3)
    private val milk = Ingredient(3, "Whole milk", "ml", 10, 61.0, 2.9, 4.3, 3.2)

    @Test
    fun countTextSetsQtyInPortions() {
        assertEquals(100, IngredientEdits.setCount(bread, "2").qty)
        assertEquals(25, IngredientEdits.setCount(bread, "0,5").qty)
        assertEquals(75, IngredientEdits.setCount(bread, "1.5").qty)
    }

    @Test
    fun partialDecimalKeepsTheParsedPart() {
        assertEquals(50, IngredientEdits.setCount(bread, "1,").qty)
    }

    @Test
    fun unparseableTextLeavesQtyUnchanged() {
        assertEquals(50, IngredientEdits.setCount(bread, ",").qty)
    }

    @Test
    fun emptyTextMeansZero() {
        assertEquals(0, IngredientEdits.setCount(bread, "").qty)
    }

    @Test
    fun plusAndMinusMoveOnePortion() {
        assertEquals(100, IngredientEdits.plus(bread).qty)
        assertEquals(0, IngredientEdits.minus(bread).qty)
        assertEquals(0, IngredientEdits.minus(IngredientEdits.minus(bread)).qty)
        assertEquals(75, IngredientEdits.plus(IngredientEdits.setCount(bread, "0,5")).qty)
    }

    @Test
    fun gramsIngredientKeepsOldStepping() {
        assertEquals(110, IngredientEdits.plus(rice).qty)
        assertEquals(90, IngredientEdits.minus(rice).qty)
        assertEquals(15, IngredientEdits.plus(milk).qty)
        assertEquals(250, IngredientEdits.setGrams(rice, "250").qty)
        assertEquals(0, IngredientEdits.setGrams(rice, "").qty)
        assertEquals(2000, IngredientEdits.setGrams(rice, "9999").qty)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: compilation FAILS (`MealSelection`, `IngredientEdits` unresolved).

- [ ] **Step 3: Implement the pure objects**

`data/MealSelection.kt`:
```kotlin
package dev.viniciuscole.nudge.data

import dev.viniciuscole.nudge.data.model.SavedMeal

object MealSelection {
    fun latest(meals: List<SavedMeal>, reminderId: Long): SavedMeal? =
        meals.filter { it.reminderId == reminderId }
            .maxWithOrNull(compareBy<SavedMeal>({ it.date }, { it.id }))
}
```

`ui/builder/IngredientEdits.kt`:
```kotlin
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Wire repository and ViewModel**

`data/MealRepository.kt` — delete `forReminderOn(...)` and its now-unused `import java.time.LocalDate`, add:
```kotlin
    fun latestFor(reminderId: Long): SavedMeal? = MealSelection.latest(_meals.value, reminderId)
```

`ui/builder/MealBuilderViewModel.kt`:
- `BuilderUiState` gains `val countText: Map<Long, String> = emptyMap(),`.
- In `init`, replace the two lines loading the meal:
```kotlin
        val existing = app.meals.latestFor(reminderId)
        val ingredients = existing?.ingredients?.map { it.withInferredPortion() } ?: emptyList()
```
- Replace `setQty`, `plus`, `minus` with:
```kotlin
    fun setQty(id: Long, text: String) = updateIngredients { list -> list.map { if (it.id == id) IngredientEdits.setGrams(it, text) else it } }

    fun setCount(id: Long, text: String) {
        val kept = text.filter { it.isDigit() || it == ',' || it == '.' }.take(5)
        _state.update { s ->
            val list = s.ingredients.map { if (it.id == id) IngredientEdits.setCount(it, kept) else it }
            s.copy(ingredients = list, totals = Nutrition.totals(list), countText = s.countText + (id to kept), saved = false)
        }
    }

    fun plus(id: Long) = step(id, IngredientEdits::plus)

    fun minus(id: Long) = step(id, IngredientEdits::minus)

    private fun step(id: Long, f: (Ingredient) -> Ingredient) {
        _state.update { s ->
            val list = s.ingredients.map { if (it.id == id) f(it) else it }
            s.copy(ingredients = list, totals = Nutrition.totals(list), countText = s.countText - id, saved = false)
        }
    }
```
- In `remove(id)`, also drop the text: replace its body with
```kotlin
    fun remove(id: Long) = _state.update { s ->
        val list = s.ingredients.filter { it.id != id }
        s.copy(ingredients = list, totals = Nutrition.totals(list), countText = s.countText - id, saved = false)
    }
```
- In `save()`, replace `val existing = app.meals.forReminderOn(reminderId, LocalDate.now())` with `val existing = app.meals.latestFor(reminderId)`. Keep `date = LocalDate.now()` (the saved meal moves to today; the same `id` is reused, so it is updated in place).

- [ ] **Step 6: Portion UI in the ingredient row**

`ui/builder/MealBuilderScreen.kt`:
- Call site becomes:
```kotlin
IngredientRow(
    ing,
    countText = s.countText[ing.id],
    onQty = { vm.setQty(ing.id, it) },
    onCount = { vm.setCount(ing.id, it) },
    onPlus = { vm.plus(ing.id) },
    onMinus = { vm.minus(ing.id) },
    onRemove = { vm.remove(ing.id) },
)
```
- Replace the whole `IngredientRow` with (add imports `dev.viniciuscole.nudge.data.food.PortionMath`, `androidx.compose.ui.text.style.TextOverflow`):
```kotlin
@Composable
private fun IngredientRow(
    ing: Ingredient,
    countText: String?,
    onQty: (String) -> Unit,
    onCount: (String) -> Unit,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onRemove: () -> Unit,
) {
    val c = NudgeTheme.colors
    val portion = ing.portion
    val count = portion?.let { PortionMath.count(ing.qty, it) }
    Card24(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(ing.name, style = manrope(15.5.sp, FontWeight.Bold), color = c.ink, maxLines = 1)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.weight(1f, fill = false).clip(RoundedCornerShape(10.dp)).background(c.field).padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BasicTextField(
                            value = if (portion != null) countText ?: PortionMath.format(count!!) else ing.qty.toString(),
                            onValueChange = if (portion != null) onCount else onQty,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = if (portion != null) KeyboardType.Decimal else KeyboardType.Number),
                            textStyle = manrope(14.sp, FontWeight.Bold).copy(color = c.ink, textAlign = TextAlign.End),
                            cursorBrush = SolidColor(c.coral),
                            modifier = Modifier.width(if (portion != null) 44.dp else 38.dp),
                        )
                        Text(
                            if (portion != null) PortionMath.noun(count!!, portion) else ing.unit,
                            style = manrope(12.5.sp, FontWeight.SemiBold),
                            color = c.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    CircleTextButton("−", onClick = onMinus)
                    CircleTextButton("+", onClick = onPlus)
                }
                if (portion != null) {
                    Text("${ing.qty} ${ing.unit}", style = manrope(12.sp, FontWeight.Medium), color = c.faint, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(ing.kcal.roundToInt().toString(), style = manrope(16.sp, FontWeight.ExtraBold), color = c.ink)
                Text(stringResource(R.string.kcal_caps), style = manrope(11.sp, FontWeight.SemiBold, letterSpacing = .7.sp), color = c.faint, modifier = Modifier.padding(top = 4.dp))
            }
            CircleIconButton(R.drawable.ic_close, onClick = onRemove, size = 34.dp, tint = c.line, iconSize = 16.dp, contentDescription = stringResource(R.string.remove))
        }
    }
}
```
If `KeyboardType.Decimal` does not resolve in this Compose version, use `KeyboardType.Number` — the text filter already accepts `,` and `.`.

- [ ] **Step 7: Build and run the full suite**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL. `grep -rn "forReminderOn" app/src` returns nothing.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: fixed meal per reminder and portion entry in the builder

The builder now opens the reminder's most recent meal from any day,
so a diet built once repeats every day. Stored meals are untouched.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Edit reminder (+ "Test alarm")

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/add/Draft.kt` (moves `Draft` out of `AddReminderViewModel.kt`)
- Create: `app/src/main/java/dev/viniciuscole/nudge/alarm/ReminderPreview.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/add/AddReminderViewModel.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/add/AddReminderScreen.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/alarm/Notifications.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/alarm/AlarmReceiver.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/NudgeNavHost.kt`
- Modify: both `strings.xml`
- Test: `app/src/test/java/dev/viniciuscole/nudge/ui/add/DraftTest.kt` (new)

**Interfaces:**
- Produces: `Draft.from(r: Reminder): Draft`; `Draft.toReminder(id: Long, enabled: Boolean, defaultMealLabel: String, defaultWaterLabel: String): Reminder`; `Draft.PRESET_INTERVALS`; `AddReminderViewModel(app: NudgeApp, editId: Long? = null)` with `isEdit: Boolean`, `missing: Boolean`, `testAlarm()`; `Notifications.postGentle(context: Context, r: Reminder)`; `ReminderPreview.run(app: NudgeApp, r: Reminder)`; `Routes.EDIT = "edit/{reminderId}"`, `Routes.edit(id: Long)`.

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/dev/viniciuscole/nudge/ui/add/DraftTest.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.add

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftTest {

    private val lunch = Reminder(3, ReminderType.MEAL, "Almoço", enabled = true, insistent = true, hour = 12, minute = 30)
    private val water = Reminder(4, ReminderType.WATER, "Água", enabled = true, insistent = false, startHour = 7, endHour = 21, intervalMin = 45)

    @Test
    fun mealRoundTripsThroughDraft() {
        assertEquals(lunch, Draft.from(lunch).toReminder(lunch.id, lunch.enabled, "Refeição", "Beber água"))
    }

    @Test
    fun waterRoundTripsThroughDraft() {
        assertEquals(water, Draft.from(water).toReminder(water.id, water.enabled, "Refeição", "Beber água"))
    }

    @Test
    fun editingKeepsDisabledState() {
        val off = lunch.copy(enabled = false)
        assertFalse(Draft.from(off).toReminder(off.id, off.enabled, "Refeição", "Beber água").enabled)
    }

    @Test
    fun nonPresetIntervalMarksCustom() {
        assertTrue(Draft.from(water).customInterval)
        assertFalse(Draft.from(water.copy(intervalMin = 90)).customInterval)
    }

    @Test
    fun blankLabelFallsBackByTypeAndIntervalHasAFloor() {
        val draft = Draft(type = ReminderType.WATER, label = "  ", intervalMin = 2)
        val r = draft.toReminder(9, true, "Refeição", "Beber água")
        assertEquals("Beber água", r.label)
        assertEquals(5, r.intervalMin)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: compilation FAILS (`Draft.from` / `toReminder` unresolved).

- [ ] **Step 3: Move `Draft` and add the conversions**

Delete the `data class Draft(...)` block from `AddReminderViewModel.kt` and create `ui/add/Draft.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.add

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType

data class Draft(
    val type: ReminderType = ReminderType.MEAL,
    val label: String = "",
    val hour: Int = 12,
    val minute: Int = 30,
    val startHour: Int = 8,
    val endHour: Int = 22,
    val intervalMin: Int = 90,
    val customInterval: Boolean = false,
    val insistent: Boolean = true,
) {
    fun toReminder(id: Long, enabled: Boolean, defaultMealLabel: String, defaultWaterLabel: String): Reminder = Reminder(
        id = id,
        type = type,
        label = label.trim().ifEmpty { if (type == ReminderType.MEAL) defaultMealLabel else defaultWaterLabel },
        enabled = enabled,
        insistent = insistent,
        hour = hour,
        minute = minute,
        startHour = startHour,
        endHour = endHour,
        intervalMin = intervalMin.coerceAtLeast(5),
    )

    companion object {
        val PRESET_INTERVALS = listOf(30, 60, 90, 120)

        fun from(r: Reminder): Draft = Draft(
            type = r.type,
            label = r.label,
            hour = r.hour,
            minute = r.minute,
            startHour = r.startHour,
            endHour = r.endHour,
            intervalMin = r.intervalMin,
            customInterval = r.intervalMin !in PRESET_INTERVALS,
            insistent = r.insistent,
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Gentle notification helper and preview**

In `alarm/Notifications.kt` add (imports: `android.Manifest`, `android.content.pm.PackageManager`, `android.os.Build`, `androidx.core.app.NotificationManagerCompat`, `androidx.core.content.ContextCompat`):
```kotlin
    fun postGentle(context: Context, r: Reminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(GENTLE_ID, gentle(context, r))
    }
```
In `alarm/AlarmReceiver.kt`: delete `postGentleNotification(...)`, call `Notifications.postGentle(context, reminder)` in its place, and remove the imports that become unused.

Create `alarm/ReminderPreview.kt`:
```kotlin
package dev.viniciuscole.nudge.alarm

import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.data.model.Reminder

object ReminderPreview {
    fun run(app: NudgeApp, r: Reminder) {
        if (r.insistent) {
            AlarmRingingService.start(app, r.id)
            app.startActivity(AlarmActivity.intent(app, r.id))
        } else {
            Notifications.postGentle(app, r)
        }
    }
}
```

- [ ] **Step 6: Edit mode in the ViewModel**

`AddReminderViewModel.kt` — new constructor and changed members (keep all other mutators as they are; import `dev.viniciuscole.nudge.alarm.ReminderPreview`):
```kotlin
class AddReminderViewModel(private val app: NudgeApp, editId: Long? = null) : ViewModel() {

    private val editing: Reminder? = editId?.let { app.reminders.get(it) }
    val isEdit: Boolean = editId != null
    val missing: Boolean = editId != null && editing == null

    private val _draft = MutableStateFlow(editing?.let(Draft::from) ?: Draft())
    val draft: StateFlow<Draft> = _draft.asStateFlow()

    val presetIntervals = Draft.PRESET_INTERVALS

    fun setType(type: ReminderType) {
        if (isEdit) return
        _draft.update { it.copy(type = type) }
    }

    fun save(defaultMealLabel: String, defaultWaterLabel: String): Reminder {
        val reminder = _draft.value.toReminder(
            id = editing?.id ?: app.reminders.nextId(),
            enabled = editing?.enabled ?: true,
            defaultMealLabel = defaultMealLabel,
            defaultWaterLabel = defaultWaterLabel,
        )
        app.reminders.upsert(reminder)
        app.scheduler.schedule(reminder)
        return reminder
    }

    fun testAlarm() {
        val id = editing?.id ?: return
        app.reminders.get(id)?.let { ReminderPreview.run(app, it) }
    }
```
`scheduler.schedule` already cancels first and does nothing more for a disabled reminder, so saving a switched-off reminder never re-arms it.

- [ ] **Step 7: Edit mode in the screen**

`AddReminderScreen.kt` (imports: `androidx.compose.runtime.LaunchedEffect`, `androidx.compose.ui.draw.alpha`):
- At the top of `AddReminderScreen`, after the `val`s:
```kotlin
    LaunchedEffect(vm.missing) { if (vm.missing) onBack() }
```
- Title: `Text(stringResource(if (vm.isEdit) R.string.edit_reminder else R.string.new_reminder), ...)`.
- Wrap the `SegmentedPill(...)` call: `Box(Modifier.alpha(if (vm.isEdit) 0.5f else 1f)) { SegmentedPill(...) }` (the ViewModel already ignores `setType` in edit mode).
- Replace the content of the bottom `Box` (currently one `PillButton`) with:
```kotlin
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    text = stringResource(if (vm.isEdit) R.string.save_changes else R.string.save_reminder),
                    onClick = {
                        vm.save(defaultMeal, defaultWater)
                        onBack()
                    },
                    color = if (isMeal) c.coral else c.teal,
                )
                if (vm.isEdit) {
                    PillButton(
                        text = stringResource(R.string.test_alarm),
                        onClick = vm::testAlarm,
                        color = if (isMeal) c.coral else c.teal,
                        contentColor = if (isMeal) c.coral else c.teal,
                        height = 52.dp,
                        outlined = true,
                    )
                }
            }
```

Strings — add before `</resources>`:
- `values/strings.xml`:
```xml
    <string name="edit_reminder">Edit reminder</string>
    <string name="save_changes">Save changes</string>
    <string name="test_alarm">Test alarm</string>
```
- `values-pt-rBR/strings.xml`:
```xml
    <string name="edit_reminder">Editar lembrete</string>
    <string name="save_changes">Salvar alterações</string>
    <string name="test_alarm">Testar alarme</string>
```

- [ ] **Step 8: Route**

`ui/NudgeNavHost.kt` — in `Routes` add:
```kotlin
    const val EDIT = "edit/{reminderId}"
    fun edit(id: Long) = "edit/$id"
```
and a destination after `ADD`:
```kotlin
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType }),
        ) { entry ->
            val reminderId = entry.arguments?.getLong("reminderId") ?: -1L
            val vm: AddReminderViewModel = viewModel(
                key = "edit-$reminderId",
                factory = viewModelFactory { initializer { AddReminderViewModel(app, reminderId) } },
            )
            AddReminderScreen(vm = vm, onBack = { nav.popBackStack() })
        }
```
(Nothing navigates here yet — Task 7 wires the long press.)

- [ ] **Step 9: Build and run the full suite**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: edit reminders, with a test-alarm button

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Diet screen

**Files:**
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/diet/DietCalc.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/diet/DietViewModel.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/diet/DietScreen.kt`
- Create: `app/src/main/java/dev/viniciuscole/nudge/ui/components/MacroBreakdown.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/builder/MealBuilderScreen.kt` (use `MacroBreakdown`, delete its private `Legend`)
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/NudgeNavHost.kt`
- Modify: both `strings.xml`
- Test: `app/src/test/java/dev/viniciuscole/nudge/ui/diet/DietCalcTest.kt` (new)

**Interfaces:**
- Consumes: `MealSelection.latest` (Task 4); `KcalGoal`, `SettingsRepository`, `Numbers.grouped` (Task 3).
- Produces: `data class DietMealRow(reminderId: Long, label: String, hour: Int, minute: Int, totals: MealTotals, empty: Boolean)`; `data class DietUiState(rows, totals, goal) { val progress: Float }`; `object DietCalc { fun build(reminders: List<Reminder>, meals: List<SavedMeal>, goal: Int): DietUiState }`; `@Composable fun MacroBreakdown(t: MealTotals, modifier: Modifier = Modifier, showPercent: Boolean = false)`; `Routes.DIET = "diet"`.

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/dev/viniciuscole/nudge/ui/diet/DietCalcTest.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.diet

import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import dev.viniciuscole.nudge.data.model.SavedMeal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DietCalcTest {

    private val today = LocalDate.of(2026, 9, 23)
    private fun meal(id: Long, h: Int, enabled: Boolean = true) = Reminder(id, ReminderType.MEAL, "r$id", enabled = enabled, hour = h, minute = 0)
    private fun saved(id: Long, reminderId: Long, kcal100: Double, qty: Int, date: LocalDate = today) =
        SavedMeal(id, reminderId, "m", date, listOf(Ingredient(1, "x", "g", qty, kcal100, 10.0, 10.0, 10.0)))

    @Test
    fun sumsActiveMealReminders() {
        val s = DietCalc.build(listOf(meal(1, 8), meal(2, 12)), listOf(saved(10, 1, 200.0, 100), saved(11, 2, 200.0, 50)), 1800)
        assertEquals(300, s.totals.kcal)
        assertEquals(2, s.rows.size)
    }

    @Test
    fun disabledAndWaterRemindersAreExcluded() {
        val water = Reminder(3, ReminderType.WATER, "água")
        val s = DietCalc.build(listOf(meal(1, 8), meal(2, 12, enabled = false), water), listOf(saved(10, 1, 100.0, 100), saved(11, 2, 500.0, 100)), 1800)
        assertEquals(listOf(1L), s.rows.map { it.reminderId })
        assertEquals(100, s.totals.kcal)
    }

    @Test
    fun emptyMealCountsZero() {
        val s = DietCalc.build(listOf(meal(1, 8), meal(2, 12)), listOf(saved(10, 1, 100.0, 100)), 1800)
        val empty = s.rows.first { it.reminderId == 2L }
        assertTrue(empty.empty)
        assertEquals(0, empty.totals.kcal)
        assertEquals(100, s.totals.kcal)
    }

    @Test
    fun noMealsGivesZeros() {
        val s = DietCalc.build(emptyList(), emptyList(), 1800)
        assertTrue(s.rows.isEmpty())
        assertEquals(0, s.totals.kcal)
        assertEquals(0, s.totals.proteinPct + s.totals.carbsPct + s.totals.fatPct)
        assertEquals(0f, s.progress, 0f)
    }

    @Test
    fun rowsAreSortedByTime() {
        val s = DietCalc.build(listOf(meal(1, 19), meal(2, 8)), emptyList(), 1800)
        assertEquals(listOf(2L, 1L), s.rows.map { it.reminderId })
    }

    @Test
    fun usesTheLatestMealFromAnyDay() {
        val s = DietCalc.build(listOf(meal(1, 8)), listOf(saved(10, 1, 100.0, 100, today.minusDays(2)), saved(11, 1, 300.0, 100, today.minusDays(1))), 1800)
        assertEquals(300, s.totals.kcal)
    }

    @Test
    fun progressIsCappedAtOne() {
        val s = DietCalc.build(listOf(meal(1, 8)), listOf(saved(10, 1, 3000.0, 100)), 1800)
        assertEquals(1f, s.progress, 0f)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: compilation FAILS (`DietCalc` unresolved).

- [ ] **Step 3: Implement `DietCalc`**

`ui/diet/DietCalc.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.diet

import dev.viniciuscole.nudge.data.KcalGoal
import dev.viniciuscole.nudge.data.MealSelection
import dev.viniciuscole.nudge.data.model.MealTotals
import dev.viniciuscole.nudge.data.model.Nutrition
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.SavedMeal

data class DietMealRow(
    val reminderId: Long,
    val label: String,
    val hour: Int,
    val minute: Int,
    val totals: MealTotals,
    val empty: Boolean,
)

data class DietUiState(
    val rows: List<DietMealRow> = emptyList(),
    val totals: MealTotals = Nutrition.totals(emptyList()),
    val goal: Int = KcalGoal.DEFAULT,
) {
    val progress: Float get() = if (goal <= 0) 0f else (totals.kcal.toFloat() / goal).coerceIn(0f, 1f)
}

object DietCalc {
    fun build(reminders: List<Reminder>, meals: List<SavedMeal>, goal: Int): DietUiState {
        val perMeal = reminders
            .filter { it.isMeal && it.enabled }
            .sortedBy { it.hour * 60 + it.minute }
            .map { r -> r to MealSelection.latest(meals, r.id)?.ingredients.orEmpty() }
        val rows = perMeal.map { (r, ingredients) ->
            DietMealRow(r.id, r.label, r.hour, r.minute, Nutrition.totals(ingredients), ingredients.isEmpty())
        }
        return DietUiState(rows, Nutrition.totals(perMeal.flatMap { it.second }), goal)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Shared macro bar**

Create `ui/components/MacroBreakdown.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.MealTotals
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope

@Composable
fun MacroBreakdown(t: MealTotals, modifier: Modifier = Modifier, showPercent: Boolean = false) {
    val c = NudgeTheme.colors
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)).background(c.chip)) {
            if (t.proteinPct > 0) Box(Modifier.weight(t.proteinPct.toFloat()).fillMaxSize().background(c.coral))
            if (t.carbsPct > 0) Box(Modifier.weight(t.carbsPct.toFloat()).fillMaxSize().background(c.carbs))
            if (t.fatPct > 0) Box(Modifier.weight(t.fatPct.toFloat()).fillMaxSize().background(c.teal))
        }
        val protein = stringResource(R.string.protein_g, t.proteinG)
        val carbs = stringResource(R.string.carbs_g, t.carbsG)
        val fat = stringResource(R.string.fat_g, t.fatG)
        if (showPercent) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Legend(c.coral, "$protein · ${t.proteinPct}%")
                Legend(c.carbs, "$carbs · ${t.carbsPct}%")
                Legend(c.teal, "$fat · ${t.fatPct}%")
            }
        } else {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(c.coral, protein)
                Legend(c.carbs, carbs)
                Legend(c.teal, fat)
            }
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    val c = NudgeTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(text, style = manrope(12.5.sp, FontWeight.SemiBold), color = c.body)
    }
}
```

In `MealBuilderScreen.kt` `TotalsCard`: replace the macro bar `Row(...)` and the legend `Row(...)` with `MacroBreakdown(t, Modifier.padding(top = 14.dp))` (import `dev.viniciuscole.nudge.ui.components.MacroBreakdown`), delete the file's private `Legend` composable, and remove imports that become unused.

- [ ] **Step 6: ViewModel and screen**

`ui/diet/DietViewModel.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.data.KcalGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DietViewModel(private val app: NudgeApp) : ViewModel() {

    val state: StateFlow<DietUiState> =
        combine(app.reminders.reminders, app.meals.meals, app.settings.kcalGoal) { r, m, g -> DietCalc.build(r, m, g) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                DietCalc.build(app.reminders.reminders.value, app.meals.meals.value, app.settings.kcalGoal.value),
            )

    fun setGoal(text: String): Boolean {
        val goal = KcalGoal.parse(text) ?: return false
        app.settings.setKcalGoal(goal)
        return true
    }
}
```

`ui/diet/DietScreen.kt`:
```kotlin
package dev.viniciuscole.nudge.ui.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.ui.components.Card24
import dev.viniciuscole.nudge.ui.components.CircleIconButton
import dev.viniciuscole.nudge.ui.components.MacroBreakdown
import dev.viniciuscole.nudge.ui.components.SectionLabel
import dev.viniciuscole.nudge.ui.format.Numbers
import dev.viniciuscole.nudge.ui.format.ReminderFormat
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope

@Composable
fun DietScreen(vm: DietViewModel, onBack: () -> Unit, onOpenBuilder: (Long) -> Unit) {
    val c = NudgeTheme.colors
    val s by vm.state.collectAsStateWithLifecycle()
    var editingGoal by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(R.drawable.ic_back, onClick = onBack, contentDescription = stringResource(R.string.back))
            Text(stringResource(R.string.diet_title), style = manrope(19.sp, FontWeight.Bold), color = c.ink)
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(s, onEditGoal = { editingGoal = true }) }
            if (s.rows.isEmpty()) {
                item { Text(stringResource(R.string.diet_no_meals), style = manrope(14.sp, FontWeight.Medium), color = c.muted, modifier = Modifier.padding(top = 8.dp)) }
            }
            items(s.rows, key = { it.reminderId }) { row -> MealRowCard(row, onClick = { onOpenBuilder(row.reminderId) }) }
        }
    }

    if (editingGoal) {
        GoalDialog(current = s.goal, onDismiss = { editingGoal = false }, onConfirm = { if (vm.setGoal(it)) editingGoal = false })
    }
}

@Composable
private fun SummaryCard(s: DietUiState, onEditGoal: () -> Unit) {
    val c = NudgeTheme.colors
    Card24(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(stringResource(R.string.diet_day_total))
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                        Text(s.totals.kcal.toString(), style = manrope(40.sp, FontWeight.ExtraBold, letterSpacing = (-0.8).sp), color = c.ink)
                        Text(" " + stringResource(R.string.kcal), style = manrope(14.sp, FontWeight.Bold), color = c.muted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                Text(
                    stringResource(R.string.diet_goal, Numbers.grouped(s.goal)),
                    style = manrope(13.sp, FontWeight.Bold),
                    color = c.coral,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onEditGoal).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp).clip(RoundedCornerShape(5.dp)).background(c.chip)) {
                Box(Modifier.fillMaxWidth(s.progress).fillMaxHeight().background(c.coral))
            }
            MacroBreakdown(s.totals, Modifier.padding(top = 18.dp), showPercent = true)
        }
    }
}

@Composable
private fun MealRowCard(row: DietMealRow, onClick: () -> Unit) {
    val c = NudgeTheme.colors
    Card24(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.label, style = manrope(15.5.sp, FontWeight.Bold), color = c.ink, maxLines = 1)
                val detail = if (row.empty) stringResource(R.string.diet_meal_empty)
                else stringResource(R.string.diet_macros_short, row.totals.proteinG, row.totals.carbsG, row.totals.fatG)
                Text(
                    "${ReminderFormat.time(row.hour, row.minute)} · $detail",
                    style = manrope(13.sp, FontWeight.Medium),
                    color = c.muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(row.totals.kcal.toString(), style = manrope(16.sp, FontWeight.ExtraBold), color = c.ink)
                Text(stringResource(R.string.kcal_caps), style = manrope(11.sp, FontWeight.SemiBold, letterSpacing = .7.sp), color = c.faint, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun GoalDialog(current: Int, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diet_goal_edit)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(5) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text(stringResource(R.string.kcal)) },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
```

Strings — add before `</resources>`:
- `values/strings.xml`:
```xml
    <string name="diet_title">Your diet</string>
    <string name="diet_day_total">Daily total</string>
    <string name="diet_goal">Goal %1$s kcal</string>
    <string name="diet_goal_edit">Daily calorie goal</string>
    <string name="diet_meal_empty">Empty — tap to build</string>
    <string name="diet_no_meals">No active meal reminders.</string>
    <string name="diet_macros_short">P %1$d g · C %2$d g · F %3$d g</string>
```
- `values-pt-rBR/strings.xml`:
```xml
    <string name="diet_title">Sua dieta</string>
    <string name="diet_day_total">Total do dia</string>
    <string name="diet_goal">Meta %1$s kcal</string>
    <string name="diet_goal_edit">Meta diária de calorias</string>
    <string name="diet_meal_empty">Vazia — toque para montar</string>
    <string name="diet_no_meals">Nenhum lembrete de refeição ativo.</string>
    <string name="diet_macros_short">P %1$d g · C %2$d g · G %3$d g</string>
```

- [ ] **Step 7: Route**

`ui/NudgeNavHost.kt` — in `Routes` add `const val DIET = "diet"`, and a destination (imports `dev.viniciuscole.nudge.ui.diet.DietScreen`, `dev.viniciuscole.nudge.ui.diet.DietViewModel`):
```kotlin
        composable(Routes.DIET) {
            val vm: DietViewModel = viewModel(factory = viewModelFactory { initializer { DietViewModel(app) } })
            DietScreen(vm = vm, onBack = { nav.popBackStack() }, onOpenBuilder = { id -> nav.navigate(Routes.builder(id)) })
        }
```

- [ ] **Step 8: Build and run the full suite**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: diet screen with daily totals and macro split

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Home screen tap map — water logging, edit on long press, diet link

**Files:**
- Modify: `app/src/main/java/dev/viniciuscole/nudge/data/model/DayStats.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/data/DayStatsRepository.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/home/HomeViewModel.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/dev/viniciuscole/nudge/ui/NudgeNavHost.kt`
- Modify: both `strings.xml`
- Test: `app/src/test/java/dev/viniciuscole/nudge/data/model/DayStatsTest.kt` (new)

**Interfaces:**
- Consumes: `Routes.edit(id)` (Task 5), `Routes.DIET` (Task 6).
- Produces: `fun DayStats.withWater(delta: Int, today: LocalDate): DayStats`; `DayStatsRepository.addWater(delta: Int)`; `HomeViewModel.logWater()`, `undoWater()` (and `preview` is removed); `HomeScreen(vm, onAdd, onOpenBuilder, onEdit: (Long) -> Unit, onOpenDiet: () -> Unit)`.

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/dev/viniciuscole/nudge/data/model/DayStatsTest.kt`:
```kotlin
package dev.viniciuscole.nudge.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayStatsTest {

    private val today = LocalDate.of(2026, 9, 23)

    @Test
    fun addsAGlassOnTheSameDay() {
        assertEquals(DayStats(today, 3, 1), DayStats(today, 2, 1).withWater(1, today))
    }

    @Test
    fun undoAtZeroStaysZero() {
        assertEquals(DayStats(today, 0, 1), DayStats(today, 0, 1).withWater(-1, today))
    }

    @Test
    fun newDayStartsFromZero() {
        assertEquals(DayStats(today, 1, 0), DayStats(today.minusDays(1), 5, 2).withWater(1, today))
    }

    @Test
    fun undoRightAfterMidnightNeverGoesNegative() {
        assertEquals(DayStats(today, 0, 0), DayStats(today.minusDays(1), 5, 2).withWater(-1, today))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: compilation FAILS (`withWater` unresolved).

- [ ] **Step 3: Implement the data side**

Append to `data/model/DayStats.kt`:
```kotlin
fun DayStats.withWater(delta: Int, today: LocalDate): DayStats {
    val base = if (date == today) this else DayStats(today)
    return base.copy(waterDone = (base.waterDone + delta).coerceAtLeast(0))
}
```

In `data/DayStatsRepository.kt` add (import `dev.viniciuscole.nudge.data.model.withWater`):
```kotlin
    fun addWater(delta: Int) = write(_stats.value.withWater(delta, LocalDate.now()))
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: ViewModel**

`ui/home/HomeViewModel.kt` — delete `preview(id)` and the now-unused `AlarmActivity` import (keep `AlarmRingingService`, still used by `stopRinging`), then add:
```kotlin
    fun logWater() = app.stats.addWater(1)

    fun undoWater() = app.stats.addWater(-1)
```

- [ ] **Step 6: Screen**

`ui/home/HomeScreen.kt` (imports: `androidx.compose.material3.SnackbarDuration`, `androidx.compose.material3.SnackbarHost`, `androidx.compose.material3.SnackbarHostState`, `androidx.compose.material3.SnackbarResult`, `androidx.compose.runtime.rememberCoroutineScope`, `kotlinx.coroutines.launch`):

- Signature:
```kotlin
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onAdd: () -> Unit,
    onOpenBuilder: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onOpenDiet: () -> Unit,
) {
```
- After `val ringingId by ...` add:
```kotlin
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val waterLogged = stringResource(R.string.water_logged)
    val undo = stringResource(R.string.undo)
    val logWater = {
        vm.logWater()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(waterLogged, undo, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                vm.undoWater()
            }
        }
    }
```
- `StatsRow(state)` becomes `StatsRow(state, onOpenDiet)`.
- In the reminder `items`, the two callbacks become:
```kotlin
                            onTap = { if (r.isMeal) onOpenBuilder(r.id) else logWater() },
                            onLongPress = { onEdit(r.id) },
```
- Inside the outer `Box`, after the FAB `if` block, add:
```kotlin
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 20.dp, end = 100.dp, bottom = 24.dp),
        )
```
- `StatsRow` gets the link on the meals card:
```kotlin
@Composable
private fun StatsRow(state: HomeUiState, onOpenDiet: () -> Unit) {
```
and its second `StatCard(...)` call gets two extra arguments: `onClick = onOpenDiet, onClickLabel = stringResource(R.string.open_diet)`.
- `StatCard`:
```kotlin
@Composable
private fun StatCard(
    modifier: Modifier, bg: Color, label: String, labelColor: Color,
    value: Int, valueColor: Color, suffix: String, suffixColor: Color,
    onClick: (() -> Unit)? = null, onClickLabel: String? = null,
) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick) else Modifier)
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = manrope(11.5.sp, FontWeight.Medium, letterSpacing = .7.sp), color = labelColor, modifier = Modifier.weight(1f))
            if (onClick != null) Text("›", style = manrope(16.sp, FontWeight.Bold), color = labelColor)
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text("$value", style = manrope(22.sp, FontWeight.ExtraBold), color = valueColor)
            Text(" $suffix", style = manrope(13.sp, FontWeight.SemiBold), color = suffixColor, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}
```

Strings — add before `</resources>`:
- `values/strings.xml`:
```xml
    <string name="water_logged">Glass logged</string>
    <string name="undo">Undo</string>
    <string name="open_diet">Open diet</string>
```
- `values-pt-rBR/strings.xml`:
```xml
    <string name="water_logged">Copo registrado</string>
    <string name="undo">Desfazer</string>
    <string name="open_diet">Abrir dieta</string>
```

- [ ] **Step 7: Navigation**

`ui/NudgeNavHost.kt` — the `HOME` destination's `HomeScreen(...)` call gains:
```kotlin
                onEdit = { id -> nav.navigate(Routes.edit(id)) },
                onOpenDiet = { nav.navigate(Routes.DIET) },
```

- [ ] **Step 8: Build and run the full suite**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL. `grep -rn "preview(" app/src/main/java` returns nothing (the only preview path is now `ReminderPreview.run` from the edit screen).

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: tap water to log a glass, long-press any card to edit

Tapping a water card no longer fires a real alarm by accident; the
alarm is tested from the edit screen instead.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: String parity guard, version bump, final verification

**Files:**
- Create: `app/src/test/java/dev/viniciuscole/nudge/StringsParityTest.kt`
- Modify: `app/build.gradle.kts` (`versionCode`/`versionName` only — do not touch `signingConfigs`)

**Interfaces:**
- Consumes: every string added in Tasks 3, 5, 6, 7.

- [ ] **Step 1: Write the parity test**

`app/src/test/java/dev/viniciuscole/nudge/StringsParityTest.kt`:
```kotlin
package dev.viniciuscole.nudge

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class StringsParityTest {

    private fun file(path: String): File = listOf(File(path), File("app/$path")).first { it.exists() }

    private fun specifiers(s: String): List<String> =
        Regex("""%\d+\$[0-9.+-]*[sdf]""").findAll(s).map { it.value }.distinct().sorted().toList()

    private fun entries(path: String): Map<String, List<String>> {
        val xml = file(path).readText()
        val strings = Regex("""<string name="([a-z_]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).associate { it.groupValues[1] to specifiers(it.groupValues[2]) }
        val plurals = Regex("""<plurals name="([a-z_]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).associate { "plurals:" + it.groupValues[1] to specifiers(it.groupValues[2]) }
        return strings + plurals
    }

    @Test
    fun bothLocalesHaveTheSameKeysAndSpecifiers() {
        val en = entries("src/main/res/values/strings.xml")
        val pt = entries("src/main/res/values-pt-rBR/strings.xml")
        assertEquals(en.keys.sorted(), pt.keys.sorted())
        en.forEach { (key, specs) -> assertEquals("specifiers of $key", specs, pt[key]) }
    }
}
```

- [ ] **Step 2: Run it**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew testDebugUnitTest`
Expected: PASS. If it fails, the message names the key — fix the missing/mismatched string, not the test.

- [ ] **Step 3: Bump the version**

In `app/build.gradle.kts`: `versionCode = 1` → `versionCode = 2`, `versionName = "0.1.0"` → `versionName = "0.2.0"`. Nothing else in that file.

- [ ] **Step 4: Final verification**

Run: `JAVA_HOME=/home/vinia/.jdks/temurin-17 ./gradlew clean assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL, 0 failures. Report the real test count (≥ 89: 38 before this plan + 51 added). Then confirm:
- `git diff de5d224 -- app/src/test/java/dev/viniciuscole/nudge/data/json/ReminderJsonTest.kt` shows NO change to `readsTheFormatAlreadyStoredOnDevices`, and the same for that test in `MealJsonTest.kt` (only new tests appended).
- `git diff de5d224 -- app/build.gradle.kts` touches only the two version lines.
- `git diff de5d224 --stat -- keystore/` is empty.
- Verify the signing certificate is unchanged:
  `JAVA_HOME=/home/vinia/.jdks/temurin-17 PATH=/home/vinia/.jdks/temurin-17/bin:$PATH /home/vinia/Android/Sdk/build-tools/35.0.0/apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk`
  Expected SHA-256 digest: `231ad4cb91e120e963738439e576e22252766510e84875fd696530844b5ab1df`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: string parity guard and version 0.2.0

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Step 6: On-device checklist (owner, after installing over the existing app — never uninstall)**

1. Existing meals: open each meal reminder's builder — yesterday's ingredients are there.
2. Portions: add "Pão francês" → "1 pão · 50 g"; type "2" → "2 pães · 100 g"; type "0,5" → "0,5 pães · 25 g"; +/− move one bread. An old ingredient like "Arroz branco cozido" now shows in "colheres de servir".
3. Edit: long-press a meal and a water card → "Editar lembrete" with the type selector greyed out; change the time, save, card updates; "Testar alarme" rings (insistent) or posts a notification (gentle); a switched-off reminder stays off after saving.
4. Water: tap a water card → counter +1 and "Copo registrado · Desfazer"; Desfazer brings it back.
5. Diet: tap the "Refeições" card → "Sua dieta" with total, progress bar, macros with %, one row per active meal; tap the goal, set 2000, the builder's note follows.
