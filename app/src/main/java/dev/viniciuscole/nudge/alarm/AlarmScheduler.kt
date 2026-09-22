package dev.viniciuscole.nudge.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.viniciuscole.nudge.MainActivity
import dev.viniciuscole.nudge.data.model.Reminder
import java.time.LocalDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(r: Reminder) {
        cancel(r.id)
        if (!r.enabled) return
        val at = NextFire.toEpochMillis(NextFire.compute(r, LocalDateTime.now()))
        set(at, firePendingIntent(r.id, snooze = false))
    }

    fun snooze(id: Long, minutes: Int = 5) {
        set(System.currentTimeMillis() + minutes * 60_000L, firePendingIntent(id, snooze = true))
    }

    fun cancel(id: Long) {
        alarmManager.cancel(firePendingIntent(id, snooze = false))
        alarmManager.cancel(firePendingIntent(id, snooze = true))
    }

    fun rescheduleAll(list: List<Reminder>) = list.forEach(::schedule)

    private fun set(at: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            return
        }
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, openAppPendingIntent()), pi)
    }

    private fun firePendingIntent(id: Long, snooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_ID, id)
        val requestCode = (id * 2 + if (snooze) 1 else 0).toInt()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppPendingIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
