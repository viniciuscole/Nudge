package dev.viniciuscole.nudge.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.NudgeApp
import dev.viniciuscole.nudge.ui.ring.RingingScreen
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val reminder = NudgeApp.from(this).reminders.get(id)
        if (reminder == null) {
            finish()
            return
        }
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val subtitle = Notifications.subtitle(this, reminder)

        setContent {
            NudgeTheme(dark = true) {
                val ringing by RingingState.current.collectAsStateWithLifecycle()
                LaunchedEffect(ringing) { if (ringing == null) finish() }
                RingingScreen(
                    reminder = reminder,
                    time = time,
                    subtitle = subtitle,
                    onDone = { startService(AlarmRingingService.intent(this, id, AlarmRingingService.ACTION_DONE)) },
                    onSnooze = { startService(AlarmRingingService.intent(this, id, AlarmRingingService.ACTION_SNOOZE)) },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_ID = "reminder_id"

        fun intent(context: Context, id: Long): Intent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
