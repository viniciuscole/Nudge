package dev.viniciuscole.nudge.ui.ring

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.ui.components.PillButton
import dev.viniciuscole.nudge.ui.theme.RingColors
import dev.viniciuscole.nudge.ui.theme.manrope

@Composable
fun RingingScreen(
    reminder: Reminder,
    time: String,
    subtitle: String,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
) {
    val meal = reminder.isMeal
    val bg = if (meal) RingColors.mealBg else RingColors.waterBg
    val accent = if (meal) RingColors.coral else Color.White
    val ringStroke = if (meal) RingColors.coral.copy(alpha = .55f) else Color.White.copy(alpha = .35f)
    val discFill = if (meal) RingColors.coral.copy(alpha = .22f) else Color.White.copy(alpha = .14f)
    val iconTint = if (meal) RingColors.mealIcon else Color.White
    val barColor = if (meal) RingColors.mealBars.copy(alpha = .7f) else Color.White.copy(alpha = .55f)

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Text(
            stringResource(R.string.reminder).uppercase(),
            style = manrope(13.sp, FontWeight.SemiBold, letterSpacing = 1.8.sp),
            color = Color.White.copy(alpha = .7f),
        )
        Text(
            time,
            style = manrope(64.sp, FontWeight.ExtraBold, letterSpacing = (-1.9).sp),
            color = Color.White,
            modifier = Modifier.padding(top = 14.dp),
        )

        Spacer(Modifier.height(44.dp))
        PulsingDisc(ringStroke, discFill) {
            Icon(
                painterResource(if (meal) R.drawable.ic_fork else R.drawable.ic_drop),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(64.dp),
            )
        }

        Text(
            reminder.label,
            style = manrope(34.sp, FontWeight.ExtraBold, lineHeight = 41.sp),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 40.dp),
        )
        Text(
            subtitle,
            style = manrope(15.sp, FontWeight.Medium),
            color = Color.White.copy(alpha = .75f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(26.dp))
        Equalizer(barColor)

        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().padding(bottom = 34.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                text = stringResource(R.string.action_done),
                onClick = onDone,
                color = accent,
                contentColor = if (meal) Color.White else RingColors.waterBg,
                height = 62.dp,
                elevated = false,
            )
            PillButton(
                text = stringResource(R.string.action_snooze),
                onClick = onSnooze,
                contentColor = Color.White,
                height = 62.dp,
                outlined = true,
            )
        }
    }
}

@Composable
private fun PulsingDisc(ringStroke: Color, discFill: Color, content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "disc")
    val pulse by t.animateFloat(
        1f, 1.07f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val ring1 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing)),
        label = "ring1",
    )
    val ring2 by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing), initialStartOffset = StartOffset(1100)),
        label = "ring2",
    )
    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        listOf(ring1, ring2).forEach { p ->
            Box(
                Modifier
                    .fillMaxSize()
                    .scale(0.9f + 0.65f * p)
                    .alpha(0.55f * (1f - p))
                    .border(2.dp, ringStroke, CircleShape),
            )
        }
        Box(
            Modifier.size(132.dp).scale(pulse).clip(CircleShape).background(discFill),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@Composable
private fun Equalizer(color: Color) {
    val delays = listOf(0, 140, 280, 420, 560, 420, 280, 140)
    val t = rememberInfiniteTransition(label = "eq")
    Row(
        Modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        delays.forEach { delay ->
            val scaleY by t.animateFloat(
                0.25f, 1f,
                infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(delay)),
                label = "bar$delay",
            )
            Box(
                Modifier
                    .width(5.dp)
                    .height(36.dp)
                    .graphicsLayer {
                        this.scaleY = scaleY
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}
