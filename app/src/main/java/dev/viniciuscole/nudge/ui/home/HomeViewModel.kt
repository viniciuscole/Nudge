package dev.viniciuscole.nudge.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.alarm.AlarmRingingService
import dev.viniciuscole.nudge.alarm.NextFire
import dev.viniciuscole.nudge.alarm.RingingState
import dev.viniciuscole.nudge.data.model.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val reminders: List<Reminder> = emptyList(),
    val waterDone: Int = 0,
    val waterGoal: Int = 0,
    val mealsDone: Int = 0,
    val mealsTotal: Int = 0,
)

class HomeViewModel(private val app: NudgeApp) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(app.reminders.reminders, app.stats.stats) { list, stats ->
        val today = if (stats.date == LocalDate.now()) stats else stats.copy(date = LocalDate.now(), waterDone = 0, mealsDone = 0)
        val enabled = list.filter { it.enabled }
        HomeUiState(
            reminders = list,
            waterDone = today.waterDone,
            waterGoal = enabled.filter { it.isWater }.sumOf { NextFire.slotCount(it) },
            mealsDone = today.mealsDone,
            mealsTotal = enabled.count { it.isMeal },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(app.reminders.reminders.value))

    val ringing: StateFlow<Long?> = RingingState.current

    fun stopRinging() {
        val id = RingingState.current.value ?: return
        app.startService(AlarmRingingService.intent(app, id, AlarmRingingService.ACTION_DONE))
    }

    fun toggle(id: Long, enabled: Boolean) {
        app.reminders.setEnabled(id, enabled)
        app.reminders.get(id)?.let { if (enabled) app.scheduler.schedule(it) else app.scheduler.cancel(id) }
    }

    fun delete(id: Long) {
        app.scheduler.cancel(id)
        app.reminders.delete(id)
    }

    fun logWater() = app.stats.addWater(1)

    fun undoWater() = app.stats.addWater(-1)
}
