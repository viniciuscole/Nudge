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
