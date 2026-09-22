package dev.viniciuscole.nudge.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import dev.viniciuscole.nudge.NudgeApp

class AlarmRingingService : Service() {

    private lateinit var ringer: Ringer
    private val handler = Handler(Looper.getMainLooper())
    private val autoSnooze = Runnable { finish(snooze = true) }
    private var reminderId = -1L

    override fun onCreate() {
        super.onCreate()
        ringer = Ringer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = NudgeApp.from(this)
        reminderId = intent?.getLongExtra(EXTRA_ID, reminderId) ?: reminderId
        when (intent?.action) {
            ACTION_RING -> {
                val reminder = app.reminders.get(reminderId)
                if (reminder == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val notification = Notifications.ringing(this, reminder)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(Notifications.RINGING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(Notifications.RINGING_ID, notification)
                }
                RingingState.set(reminderId)
                if (reminder.insistent) ringer.start()
                handler.removeCallbacks(autoSnooze)
                handler.postDelayed(autoSnooze, AUTO_SNOOZE_MS)
            }
            ACTION_DONE -> {
                app.stats.recordDone(app.reminders.get(reminderId))
                finish(snooze = false)
            }
            ACTION_SNOOZE -> finish(snooze = true)
        }
        return START_NOT_STICKY
    }

    private fun finish(snooze: Boolean) {
        handler.removeCallbacks(autoSnooze)
        ringer.stop()
        if (snooze && reminderId >= 0) NudgeApp.from(this).scheduler.snooze(reminderId)
        RingingState.set(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoSnooze)
        ringer.stop()
        RingingState.set(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_RING = "dev.viniciuscole.nudge.action.RING"
        const val ACTION_DONE = "dev.viniciuscole.nudge.action.DONE"
        const val ACTION_SNOOZE = "dev.viniciuscole.nudge.action.SNOOZE"
        const val EXTRA_ID = "reminder_id"
        private const val AUTO_SNOOZE_MS = 10 * 60_000L

        fun intent(context: Context, id: Long, action: String): Intent =
            Intent(context, AlarmRingingService::class.java).setAction(action).putExtra(EXTRA_ID, id)

        fun start(context: Context, id: Long) {
            ContextCompat.startForegroundService(context, intent(context, id, ACTION_RING))
        }
    }
}
