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
