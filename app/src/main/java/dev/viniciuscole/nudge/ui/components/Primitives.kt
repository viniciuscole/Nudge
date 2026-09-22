package dev.viniciuscole.nudge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.viniciuscole.nudge.ui.theme.CapsLabel
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope

@Composable
fun NudgeSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = NudgeTheme.colors
    val track by animateColorAsState(if (checked) c.toggleOn else c.toggleOff, label = "track")
    val knobSize by animateDpAsState(if (checked) 24.dp else 16.dp, label = "knob")
    val knobOffset by animateDpAsState(if (checked) 24.dp else 8.dp, label = "offset")
    Box(
        modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(track)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .clip(CircleShape)
                .background(if (checked) c.toggleKnobOn else c.toggleKnobOff),
        )
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NudgeTheme.colors.coral,
    contentColor: Color = Color.White,
    height: Dp = 58.dp,
    outlined: Boolean = false,
    elevated: Boolean = true,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .then(if (elevated && !outlined) Modifier.shadow(8.dp, shape, spotColor = color.copy(alpha = .45f), ambientColor = color.copy(alpha = .3f)) else Modifier)
            .clip(shape)
            .then(if (outlined) Modifier.border(2.dp, contentColor.copy(alpha = .6f), shape) else Modifier.background(color))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = manrope(17.sp, FontWeight.Bold), color = contentColor)
    }
}

@Composable
fun IconTile(iconRes: Int, bg: Color, tint: Color, size: Dp = 48.dp, radius: Dp = 16.dp, iconSize: Dp = 24.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = CapsLabel, color = NudgeTheme.colors.faint, modifier = modifier)
}

@Composable
fun NudgeChip(text: String, selected: Boolean, onClick: () -> Unit, selectedColor: Color = NudgeTheme.colors.teal) {
    val c = NudgeTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .clip(shape)
            .then(if (selected) Modifier.background(selectedColor) else Modifier.border(1.5.dp, c.line, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Text(
            text,
            style = manrope(13.5.sp, if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) Color.White else c.body,
        )
    }
}

@Composable
fun CircleIconButton(
    iconRes: Int,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    bg: Color = Color.Transparent,
    tint: Color = NudgeTheme.colors.ink,
    iconSize: Dp = 22.dp,
    contentDescription: String? = null,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun CircleTextButton(text: String, onClick: () -> Unit, size: Dp = 28.dp, fontSize: Int = 15) {
    val c = NudgeTheme.colors
    Box(
        Modifier.size(size).clip(CircleShape).background(c.field).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = manrope(fontSize.sp, FontWeight.Bold), color = c.muted)
    }
}

@Composable
fun SegmentedPill(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    colors: List<Color>,
) {
    val c = NudgeTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(c.chip).padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) colors[i] else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = manrope(14.5.sp, if (selected) FontWeight.Bold else FontWeight.SemiBold),
                    color = if (selected) Color.White else c.muted,
                )
            }
        }
    }
}

@Composable
fun Card24(modifier: Modifier = Modifier, radius: Dp = 24.dp, content: @Composable BoxScope.() -> Unit) {
    val c = NudgeTheme.colors
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .shadow(if (c.isDark) 0.dp else 6.dp, shape, ambientColor = c.ink.copy(alpha = .06f), spotColor = c.ink.copy(alpha = .08f))
            .clip(shape)
            .background(c.card),
        content = content,
    )
}
