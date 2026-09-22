package dev.viniciuscole.nudge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.viniciuscole.nudge.ui.NudgeNavHost
import dev.viniciuscole.nudge.ui.theme.NudgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NudgeTheme { NudgeNavHost() }
        }
    }
}
