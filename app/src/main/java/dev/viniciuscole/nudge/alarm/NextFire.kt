package dev.viniciuscole.nudge.alarm

import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.data.model.ReminderType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object NextFire {

    fun compute(r: Reminder, now: LocalDateTime): LocalDateTime = when (r.type) {
        ReminderType.MEAL -> {
            val today = now.toLocalDate().atTime(r.hour, r.minute)
            if (today.isAfter(now)) today else today.plusDays(1)
        }
        ReminderType.WATER -> {
            val today = now.toLocalDate()
            slotsOn(r, today).firstOrNull { it.isAfter(now) } ?: slotsOn(r, today.plusDays(1)).first()
        }
    }

    fun slotsOn(r: Reminder, day: LocalDate): List<LocalDateTime> {
        val start = day.atTime(r.startHour, 0)
        val end = day.atTime(r.endHour, 0)
        if (end.isBefore(start)) return listOf(start)
        val step = r.intervalMin.coerceAtLeast(5).toLong()
        return generateSequence(start) { it.plusMinutes(step) }
            .takeWhile { !it.isAfter(end) }
            .toList()
    }

    fun slotCount(r: Reminder): Int = slotsOn(r, LocalDate.of(2000, 1, 1)).size

    fun toEpochMillis(t: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long =
        t.atZone(zone).toInstant().toEpochMilli()
}
