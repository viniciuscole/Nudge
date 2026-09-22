package dev.viniciuscole.nudge.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.data.food.FoodItem
import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.MealTotals
import dev.viniciuscole.nudge.data.model.Nutrition
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.SavedMeal
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val noResults: Boolean get() = hasQuery && !searching && suggestions.isEmpty()
    val dayShare: Int get() = Nutrition.dayShare(totals.kcal)
}

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MealBuilderViewModel(private val app: NudgeApp, private val reminderId: Long) : ViewModel() {

    private val _state = MutableStateFlow(BuilderUiState())
    val state: StateFlow<BuilderUiState> = _state
    private val query = MutableStateFlow("")
    private var nextIngredientId = 1L

    init {
        val reminder = app.reminders.get(reminderId)
        val existing = app.meals.forReminderOn(reminderId, LocalDate.now())
        val ingredients = existing?.ingredients ?: emptyList()
        nextIngredientId = (ingredients.maxOfOrNull { it.id } ?: 0L) + 1
        _state.value = BuilderUiState(reminder = reminder, ingredients = ingredients, totals = Nutrition.totals(ingredients))

        query
            .debounce(350)
            .distinctUntilChanged()
            .onEach { q -> _state.update { it.copy(searching = q.isNotBlank(), suggestions = if (q.isBlank()) emptyList() else it.suggestions) } }
            .mapLatest { q -> if (q.isBlank()) emptyList() else app.foodSearch.search(q) }
            .onEach { results -> _state.update { it.copy(suggestions = results, searching = false) } }
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

    fun setQty(id: Long, text: String) {
        val v = text.filter(Char::isDigit).take(4).toIntOrNull() ?: 0
        updateIngredients { list -> list.map { if (it.id == id) it.copy(qty = v.coerceIn(0, 2000)) else it } }
    }

    fun plus(id: Long) = updateIngredients { list -> list.map { if (it.id == id) it.copy(qty = (it.qty + it.step).coerceAtMost(2000)) else it } }

    fun minus(id: Long) = updateIngredients { list -> list.map { if (it.id == id) it.copy(qty = (it.qty - it.step).coerceAtLeast(0)) else it } }

    fun remove(id: Long) = updateIngredients { list -> list.filter { it.id != id } }

    fun save() {
        val s = _state.value
        val existing = app.meals.forReminderOn(reminderId, LocalDate.now())
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
