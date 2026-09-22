package dev.viniciuscole.nudge.ui.add

import androidx.lifecycle.ViewModel
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
)

class AddReminderViewModel(private val app: NudgeApp) : ViewModel() {

    private val _draft = MutableStateFlow(Draft())
    val draft: StateFlow<Draft> = _draft.asStateFlow()

    val presetIntervals = listOf(30, 60, 90, 120)

    fun setType(type: ReminderType) = _draft.update { it.copy(type = type) }
    fun setLabel(label: String) = _draft.update { it.copy(label = label) }
    fun setTime(hour: Int, minute: Int) = _draft.update { it.copy(hour = hour, minute = minute) }
    fun setInsistent(v: Boolean) = _draft.update { it.copy(insistent = v) }

    fun nudgeTime(deltaMinutes: Int) = _draft.update {
        val total = ((it.hour * 60 + it.minute + deltaMinutes) % 1440 + 1440) % 1440
        it.copy(hour = total / 60, minute = total % 60)
    }

    fun setStartHour(h: Int) = _draft.update { it.copy(startHour = h.coerceIn(0, 23)) }
    fun setEndHour(h: Int) = _draft.update { it.copy(endHour = h.coerceIn(0, 23)) }

    fun pickInterval(min: Int) = _draft.update { it.copy(intervalMin = min, customInterval = false) }
    fun pickCustomInterval() = _draft.update { it.copy(customInterval = true) }
    fun setCustomInterval(text: String) {
        val v = text.filter(Char::isDigit).take(3).toIntOrNull() ?: return
        _draft.update { it.copy(intervalMin = v) }
    }

    fun save(defaultMealLabel: String, defaultWaterLabel: String): Reminder {
        val d = _draft.value
        val label = d.label.trim().ifEmpty { if (d.type == ReminderType.MEAL) defaultMealLabel else defaultWaterLabel }
        val reminder = Reminder(
            id = app.reminders.nextId(),
            type = d.type,
            label = label,
            enabled = true,
            insistent = d.insistent,
            hour = d.hour,
            minute = d.minute,
            startHour = d.startHour,
            endHour = d.endHour,
            intervalMin = d.intervalMin.coerceAtLeast(5),
        )
        app.reminders.upsert(reminder)
        app.scheduler.schedule(reminder)
        return reminder
    }
}
