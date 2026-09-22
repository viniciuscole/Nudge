package dev.viniciuscole.nudge

import android.app.Application
import android.content.Context

class NudgeApp : Application() {

    companion object {
        fun from(context: Context): NudgeApp = context.applicationContext as NudgeApp
    }
}
