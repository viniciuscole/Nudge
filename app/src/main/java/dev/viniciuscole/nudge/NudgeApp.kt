package dev.viniciuscole.nudge

import android.app.Application
import android.content.Context
import dev.viniciuscole.nudge.alarm.AlarmScheduler
import dev.viniciuscole.nudge.alarm.Notifications
import dev.viniciuscole.nudge.data.DayStatsRepository
import dev.viniciuscole.nudge.data.ReminderRepository
import dev.viniciuscole.nudge.data.food.FoodSearch
import dev.viniciuscole.nudge.data.food.UsdaClient

class NudgeApp : Application() {

    val reminders: ReminderRepository by lazy { ReminderRepository(this) }
    val scheduler: AlarmScheduler by lazy { AlarmScheduler(this) }
    val stats: DayStatsRepository by lazy { DayStatsRepository(this) }
    val foodSearch: FoodSearch by lazy { FoodSearch(UsdaClient(BuildConfig.USDA_API_KEY)) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }

    companion object {
        fun from(context: Context): NudgeApp = context.applicationContext as NudgeApp
    }
}
