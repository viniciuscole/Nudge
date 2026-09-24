package dev.viniciuscole.nudge.data

import android.content.Context
import dev.viniciuscole.nudge.data.model.DayStats
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.withWater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class DayStatsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE)
    private val _stats = MutableStateFlow(load())
    val stats: StateFlow<DayStats> = _stats.asStateFlow()

    fun today(): DayStats {
        val s = _stats.value
        return if (s.date == LocalDate.now()) s else DayStats(LocalDate.now())
    }

    fun recordDone(r: Reminder?) {
        r ?: return
        val cur = today()
        write(if (r.isWater) cur.copy(waterDone = cur.waterDone + 1) else cur.copy(mealsDone = cur.mealsDone + 1))
    }

    fun addWater(delta: Int) = write(_stats.value.withWater(delta, LocalDate.now()))

    private fun load(): DayStats = try {
        val date = prefs.getString("date", null)?.let(LocalDate::parse) ?: LocalDate.now()
        DayStats(date, prefs.getInt("water", 0), prefs.getInt("meals", 0))
    } catch (e: Exception) {
        // corrupt store: clear it so the next process start does not hit the same throw
        prefs.edit().remove("date").remove("water").remove("meals").commit()
        DayStats(LocalDate.now())
    }

    private fun write(s: DayStats) {
        _stats.value = s
        prefs.edit()
            .putString("date", s.date.toString())
            .putInt("water", s.waterDone)
            .putInt("meals", s.mealsDone)
            .apply()
    }
}
