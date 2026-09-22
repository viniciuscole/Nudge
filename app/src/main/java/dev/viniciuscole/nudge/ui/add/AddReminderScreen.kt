package dev.viniciuscole.nudge.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.ReminderType
import dev.viniciuscole.nudge.ui.components.Card24
import dev.viniciuscole.nudge.ui.components.CircleIconButton
import dev.viniciuscole.nudge.ui.components.CircleTextButton
import dev.viniciuscole.nudge.ui.components.NudgeChip
import dev.viniciuscole.nudge.ui.components.NudgeSwitch
import dev.viniciuscole.nudge.ui.components.PillButton
import dev.viniciuscole.nudge.ui.components.SectionLabel
import dev.viniciuscole.nudge.ui.components.SegmentedPill
import dev.viniciuscole.nudge.ui.format.ReminderFormat
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope

private enum class Picker { NONE, MEAL_TIME, WATER_START, WATER_END }

@Composable
fun AddReminderScreen(vm: AddReminderViewModel, onBack: () -> Unit) {
    val c = NudgeTheme.colors
    val d by vm.draft.collectAsStateWithLifecycle()
    val isMeal = d.type == ReminderType.MEAL
    var picker by remember { mutableStateOf(Picker.NONE) }
    val defaultMeal = stringResource(R.string.default_meal_label)
    val defaultWater = stringResource(R.string.default_water_label)

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(R.drawable.ic_back, onClick = onBack, contentDescription = stringResource(R.string.back))
            Text(stringResource(R.string.new_reminder), style = manrope(19.sp, FontWeight.Bold), color = c.ink)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            SegmentedPill(
                options = listOf(stringResource(R.string.type_meal), stringResource(R.string.type_water)),
                selectedIndex = if (isMeal) 0 else 1,
                onSelect = { vm.setType(if (it == 0) ReminderType.MEAL else ReminderType.WATER) },
                colors = listOf(c.coral, c.teal),
            )

            Column {
                SectionLabel(stringResource(R.string.label), Modifier.padding(bottom = 8.dp))
                FieldBox {
                    BasicTextField(
                        value = d.label,
                        onValueChange = vm::setLabel,
                        singleLine = true,
                        textStyle = manrope(16.sp, FontWeight.SemiBold).copy(color = c.ink),
                        cursorBrush = SolidColor(c.coral),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (d.label.isEmpty()) {
                                Text(stringResource(R.string.label_hint), style = manrope(16.sp, FontWeight.SemiBold), color = c.faint)
                            }
                            inner()
                        },
                    )
                }
            }

            if (isMeal) {
                Column {
                    SectionLabel(stringResource(R.string.time), Modifier.padding(bottom = 8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.card)
                            .border(1.5.dp, c.line, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircleTextButton("−", onClick = { vm.nudgeTime(-15) }, size = 40.dp, fontSize = 20)
                        Text(
                            ReminderFormat.time(d.hour, d.minute),
                            style = manrope(44.sp, FontWeight.ExtraBold, letterSpacing = (-0.9).sp),
                            color = c.ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(min = 132.dp).padding(horizontal = 10.dp).clickable { picker = Picker.MEAL_TIME },
                        )
                        CircleTextButton("+", onClick = { vm.nudgeTime(15) }, size = 40.dp, fontSize = 20)
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(8 to 0, 12 to 30, 19 to 0).forEach { (h, m) ->
                            NudgeChip(ReminderFormat.time(h, m), selected = d.hour == h && d.minute == m, onClick = { vm.setTime(h, m) }, selectedColor = c.coral)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HourTile(Modifier.weight(1f), stringResource(R.string.start), ReminderFormat.time(d.startHour, 0)) { picker = Picker.WATER_START }
                        HourTile(Modifier.weight(1f), stringResource(R.string.end), ReminderFormat.time(d.endHour, 0)) { picker = Picker.WATER_END }
                    }
                    Column {
                        SectionLabel(stringResource(R.string.every), Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val res = androidx.compose.ui.platform.LocalContext.current.resources
                            vm.presetIntervals.forEach { min ->
                                NudgeChip(ReminderFormat.interval(res, min), selected = !d.customInterval && d.intervalMin == min, onClick = { vm.pickInterval(min) })
                            }
                            NudgeChip(stringResource(R.string.custom_interval), selected = d.customInterval, onClick = vm::pickCustomInterval)
                        }
                        if (d.customInterval) {
                            FieldBox(Modifier.padding(top = 12.dp)) {
                                BasicTextField(
                                    value = if (d.intervalMin == 0) "" else d.intervalMin.toString(),
                                    onValueChange = vm::setCustomInterval,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = manrope(16.sp, FontWeight.SemiBold).copy(color = c.ink),
                                    cursorBrush = SolidColor(c.teal),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (d.intervalMin == 0) Text(stringResource(R.string.custom_interval_hint), style = manrope(16.sp, FontWeight.SemiBold), color = c.faint)
                                        inner()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Card24(Modifier.fillMaxWidth(), radius = 20.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.insistent_title), style = manrope(15.sp, FontWeight.Bold), color = c.ink)
                        Text(stringResource(R.string.insistent_body), style = manrope(13.sp, FontWeight.Medium), color = c.muted, modifier = Modifier.padding(top = 2.dp))
                    }
                    NudgeSwitch(checked = d.insistent, onCheckedChange = vm::setInsistent)
                }
            }
        }

        Box(Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding().imePadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
            PillButton(
                text = stringResource(R.string.save_reminder),
                onClick = {
                    vm.save(defaultMeal, defaultWater)
                    onBack()
                },
                color = if (isMeal) c.coral else c.teal,
            )
        }
    }

    when (picker) {
        Picker.NONE -> Unit
        Picker.MEAL_TIME -> TimeDialog(d.hour, d.minute, onDismiss = { picker = Picker.NONE }) { h, m -> vm.setTime(h, m); picker = Picker.NONE }
        Picker.WATER_START -> TimeDialog(d.startHour, 0, onDismiss = { picker = Picker.NONE }) { h, _ -> vm.setStartHour(h); picker = Picker.NONE }
        Picker.WATER_END -> TimeDialog(d.endHour, 0, onDismiss = { picker = Picker.NONE }) { h, _ -> vm.setEndHour(h); picker = Picker.NONE }
    }
}

@Composable
private fun FieldBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = NudgeTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.5.dp, c.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) { content() }
}

@Composable
private fun HourTile(modifier: Modifier, label: String, value: String, onClick: () -> Unit) {
    val c = NudgeTheme.colors
    Column(modifier) {
        SectionLabel(label, Modifier.padding(bottom = 8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .border(1.5.dp, c.line, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(value, style = manrope(26.sp, FontWeight.ExtraBold), color = c.ink)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = { TimePicker(state = state) },
    )
}
