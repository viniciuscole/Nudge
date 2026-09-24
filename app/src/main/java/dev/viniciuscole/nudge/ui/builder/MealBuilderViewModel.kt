package dev.viniciuscole.nudge.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.data.KcalGoal
import dev.viniciuscole.nudge.data.food.FoodItem
import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.MealTotals
import dev.viniciuscole.nudge.data.model.Nutrition
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.SavedMeal
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class BuilderUiState(
    val reminder: Reminder? = null,
    val query: String = "",
    val suggestions: List<FoodItem> = emptyList(),
    val searching: Boolean = false,
    val ingredients: List<Ingredient> = emptyList(),
    val totals: MealTotals = Nutrition.totals(emptyList()),
    val saved: Boolean = false,
    val kcalGoal: Int = KcalGoal.DEFAULT,
    val countText: Map<Long, String> = emptyMap(),
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val noResults: Boolean get() = hasQuery && !searching && suggestions.isEmpty()
    val dayShare: Int get() = Nutrition.dayShare(totals.kcal, kcalGoal)
}

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MealBuilderViewModel(private val app: NudgeApp, private val reminderId: Long) : ViewModel() {

    private val _state = MutableStateFlow(BuilderUiState())
    val state: StateFlow<BuilderUiState> = _state
    private val query = MutableStateFlow("")
    private var nextIngredientId = 1L

    init {
        val reminder = app.reminders.get(reminderId)
        val existing = app.meals.latestFor(reminderId)
        val ingredients = existing?.ingredients?.map { it.withInferredPortion() } ?: emptyList()
        nextIngredientId = (ingredients.maxOfOrNull { it.id } ?: 0L) + 1
        _state.value = BuilderUiState(reminder = reminder, ingredients = ingredients, totals = Nutrition.totals(ingredients))

        query
            .debounce(350)
            .distinctUntilChanged()
            .onEach { q -> _state.update { it.copy(searching = q.isNotBlank(), suggestions = if (q.isBlank()) emptyList() else it.suggestions) } }
            .mapLatest { q -> if (q.isBlank()) emptyList() else app.foodSearch.search(q) }
            .onEach { results -> _state.update { it.copy(suggestions = results, searching = false) } }
            .catch { _state.update { s -> s.copy(searching = false, suggestions = emptyList()) } }
            .launchIn(viewModelScope)

        app.settings.kcalGoal
            .onEach { goal -> _state.update { it.copy(kcalGoal = goal) } }
            .launchIn(viewModelScope)
    }

    fun setQuery(q: String) {
        query.value = q
        _state.update { it.copy(query = q, saved = false) }
    }

    fun clearQuery() = setQuery("")

    fun add(food: FoodItem) {
        val ingredient = Ingredient.from(food, nextIngredientId++)
        updateIngredients { it + ingredient }
        clearQuery()
    }

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

    fun remove(id: Long) = _state.update { s ->
        val list = s.ingredients.filter { it.id != id }
        s.copy(ingredients = list, totals = Nutrition.totals(list), countText = s.countText - id, saved = false)
    }

    fun save() {
        val s = _state.value
        val existing = app.meals.latestFor(reminderId)
        app.meals.save(
            SavedMeal(
                id = existing?.id ?: app.meals.nextId(),
                reminderId = reminderId,
                label = s.reminder?.label ?: "",
                date = LocalDate.now(),
                ingredients = s.ingredients,
            ),
        )
        _state.update { it.copy(saved = true) }
    }

    private fun updateIngredients(f: (List<Ingredient>) -> List<Ingredient>) {
        _state.update { val list = f(it.ingredients); it.copy(ingredients = list, totals = Nutrition.totals(list), saved = false) }
    }
}
