package dev.viniciuscole.nudge.data

import android.content.Context
import dev.viniciuscole.nudge.data.json.ReminderJson
import dev.viniciuscole.nudge.data.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderRepository(context: Context) {

    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    private val _reminders = MutableStateFlow(ReminderJson.decode(prefs.getString(KEY, null)))
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    fun get(id: Long): Reminder? = _reminders.value.firstOrNull { it.id == id }

    fun nextId(): Long = (_reminders.value.maxOfOrNull { it.id } ?: 0L) + 1

    fun upsert(r: Reminder) {
        val cur = _reminders.value
        write(if (cur.any { it.id == r.id }) cur.map { if (it.id == r.id) r else it } else cur + r)
    }

    fun delete(id: Long) = write(_reminders.value.filter { it.id != id })

    fun setEnabled(id: Long, enabled: Boolean) =
        write(_reminders.value.map { if (it.id == id) it.copy(enabled = enabled) else it })

    private fun write(list: List<Reminder>) {
        _reminders.value = list
        // commit(), not apply(): the alarm receiver reads this store and must see a durable write
        prefs.edit().putString(KEY, ReminderJson.encode(list)).commit()
    }

    private companion object {
        const val KEY = "list"
    }
}
