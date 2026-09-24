package dev.viniciuscole.nudge.ui.add

import androidx.lifecycle.ViewModel
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.alarm.ReminderPreview
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
}
