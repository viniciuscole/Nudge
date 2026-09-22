package dev.viniciuscole.nudge.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RingingState {
    private val _current = MutableStateFlow<Long?>(null)
    val current: StateFlow<Long?> = _current.asStateFlow()

    fun set(id: Long?) {
        _current.value = id
    }
}
