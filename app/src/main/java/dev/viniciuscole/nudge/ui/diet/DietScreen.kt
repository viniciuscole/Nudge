package dev.viniciuscole.nudge.ui.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.ui.components.Card24
import dev.viniciuscole.nudge.ui.components.CircleIconButton
import dev.viniciuscole.nudge.ui.components.MacroBreakdown
import dev.viniciuscole.nudge.ui.components.SectionLabel
import dev.viniciuscole.nudge.ui.format.Numbers
import dev.viniciuscole.nudge.ui.format.ReminderFormat
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope

@Composable
fun DietScreen(vm: DietViewModel, onBack: () -> Unit, onOpenBuilder: (Long) -> Unit) {
    val c = NudgeTheme.colors
    val s by vm.state.collectAsStateWithLifecycle()
    var editingGoal by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(R.drawable.ic_back, onClick = onBack, contentDescription = stringResource(R.string.back))
            Text(stringResource(R.string.diet_title), style = manrope(19.sp, FontWeight.Bold), color = c.ink)
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(s, onEditGoal = { editingGoal = true }) }
            if (s.rows.isEmpty()) {
                item { Text(stringResource(R.string.diet_no_meals), style = manrope(14.sp, FontWeight.Medium), color = c.muted, modifier = Modifier.padding(top = 8.dp)) }
            }
            items(s.rows, key = { it.reminderId }) { row -> MealRowCard(row, onClick = { onOpenBuilder(row.reminderId) }) }
        }
    }

    if (editingGoal) {
        GoalDialog(current = s.goal, onDismiss = { editingGoal = false }, onConfirm = { if (vm.setGoal(it)) editingGoal = false })
    }
}

@Composable
private fun SummaryCard(s: DietUiState, onEditGoal: () -> Unit) {
    val c = NudgeTheme.colors
    Card24(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(stringResource(R.string.diet_day_total))
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                        Text(s.totals.kcal.toString(), style = manrope(40.sp, FontWeight.ExtraBold, letterSpacing = (-0.8).sp), color = c.ink)
                        Text(" " + stringResource(R.string.kcal), style = manrope(14.sp, FontWeight.Bold), color = c.muted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                Text(
                    stringResource(R.string.diet_goal, Numbers.grouped(s.goal)),
                    style = manrope(13.sp, FontWeight.Bold),
                    color = c.coral,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onEditGoal).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp).clip(RoundedCornerShape(5.dp)).background(c.chip)) {
                Box(Modifier.fillMaxWidth(s.progress).fillMaxHeight().background(c.coral))
            }
            MacroBreakdown(s.totals, Modifier.padding(top = 18.dp), showPercent = true)
        }
    }
}

@Composable
private fun MealRowCard(row: DietMealRow, onClick: () -> Unit) {
    val c = NudgeTheme.colors
    Card24(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.label, style = manrope(15.5.sp, FontWeight.Bold), color = c.ink, maxLines = 1)
                val detail = if (row.empty) stringResource(R.string.diet_meal_empty)
                else stringResource(R.string.diet_macros_short, row.totals.proteinG, row.totals.carbsG, row.totals.fatG)
                Text(
                    "${ReminderFormat.time(row.hour, row.minute)} · $detail",
                    style = manrope(13.sp, FontWeight.Medium),
                    color = c.muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(row.totals.kcal.toString(), style = manrope(16.sp, FontWeight.ExtraBold), color = c.ink)
                Text(stringResource(R.string.kcal_caps), style = manrope(11.sp, FontWeight.SemiBold, letterSpacing = .7.sp), color = c.faint, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun GoalDialog(current: Int, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diet_goal_edit)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(5) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text(stringResource(R.string.kcal)) },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
