package dev.viniciuscole.nudge.data

import android.content.Context
import dev.viniciuscole.nudge.data.json.ReminderJson
import dev.viniciuscole.nudge.data.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderRepository(context: Context) {

    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    private val _reminders = MutableStateFlow(loadOrReset())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private fun loadOrReset(): List<Reminder> = try {
        ReminderJson.decode(prefs.getString(KEY, null))
    } catch (e: Exception) {
        // corrupt store: clear it so the next process start does not hit the same throw
        prefs.edit().remove(KEY).commit()
        emptyList()
    }

    fun get(id: Long): Reminder? = _reminders.value.firstOrNull { it.id == id }

    fun nextId(): Long {
        val seed = maxOf(prefs.getLong(KEY_SEQ, 0L), _reminders.value.maxOfOrNull { it.id } ?: 0L)
        val next = seed + 1
        prefs.edit().putLong(KEY_SEQ, next).commit()
        return next
    }

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
        const val KEY_SEQ = "seq"
    }
}
