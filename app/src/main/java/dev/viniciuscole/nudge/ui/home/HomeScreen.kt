package dev.viniciuscole.nudge.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.Reminder
import dev.viniciuscole.nudge.permissions.Permissions
import dev.viniciuscole.nudge.ui.components.Card24
import dev.viniciuscole.nudge.ui.components.IconTile
import dev.viniciuscole.nudge.ui.components.NudgeSwitch
import dev.viniciuscole.nudge.ui.components.PillButton
import dev.viniciuscole.nudge.ui.components.SectionLabel
import dev.viniciuscole.nudge.ui.format.ReminderFormat
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onAdd: () -> Unit,
    onOpenBuilder: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onOpenDiet: () -> Unit,
) {
    val c = NudgeTheme.colors
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Permissions.canPostNotifications(ctx)) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var permissionTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        permissionTick++
        onPauseOrDispose { }
    }

    val ringingId by vm.ringing.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val waterLogged = stringResource(R.string.water_logged)
    val undo = stringResource(R.string.undo)
    val logWater = {
        vm.logWater()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(waterLogged, undo, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                vm.undoWater()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            val ringingReminder = ringingId?.let { id -> state.reminders.firstOrNull { it.id == id } }
            if (ringingReminder != null) {
                RingingBanner(ringingReminder, onStop = vm::stopRinging)
            }
            Header(onSettings = { ctx.startActivity(Permissions.notificationSettingsIntent(ctx)) })

            if (state.reminders.isEmpty()) {
                EmptyState(onAdd)
            } else {
                StatsRow(state, onOpenDiet)
                SectionLabel(
                    stringResource(R.string.reminders),
                    Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    item(key = "permissions-$permissionTick") { PermissionBanners() }
                    items(state.reminders, key = { it.id }) { r ->
                        DismissibleReminderCard(
                            r = r,
                            onToggle = { vm.toggle(r.id, it) },
                            onDelete = { vm.delete(r.id) },
                            onTap = { if (r.isMeal) onOpenBuilder(r.id) else logWater() },
                            onLongPress = { onEdit(r.id) },
                        )
                    }
                }
            }
        }

        if (state.reminders.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 28.dp)
                    .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = c.coral.copy(alpha = .5f))
                    .size(64.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.coral)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.add_reminder), tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 20.dp, end = 100.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    val c = NudgeTheme.colors
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(date.uppercase(Locale.getDefault()), style = manrope(13.sp, FontWeight.Medium, letterSpacing = 1.2.sp), color = c.faint)
            Text(stringResource(R.string.home_title), style = manrope(30.sp, FontWeight.ExtraBold), color = c.ink, modifier = Modifier.padding(top = 4.dp))
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(c.chip).clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(16.dp).clip(CircleShape).border(2.dp, c.faint, CircleShape))
        }
    }
}

@Composable
private fun RingingBanner(r: Reminder, onStop: () -> Unit) {
    val c = NudgeTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp)).background(c.coralContainer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.alarm_ringing_now), style = manrope(15.sp, FontWeight.Bold), color = c.coralOnContainer)
            Text(r.label, style = manrope(13.sp, FontWeight.Medium), color = c.coralMuted, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            stringResource(R.string.stop),
            style = manrope(14.sp, FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.coral).clickable(onClick = onStop).padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun StatsRow(state: HomeUiState, onOpenDiet: () -> Unit) {
    val c = NudgeTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            Modifier.weight(1f),
            bg = c.tealContainer, label = stringResource(R.string.water), labelColor = c.tealDeep,
            value = state.waterDone, valueColor = c.tealOnContainer,
            suffix = stringResource(R.string.stat_glasses, state.waterGoal), suffixColor = c.tealMuted,
        )
        StatCard(
            Modifier.weight(1f),
            bg = c.coralContainer, label = stringResource(R.string.meals), labelColor = c.coralDeep,
            value = state.mealsDone, valueColor = c.coralOnContainer,
            suffix = stringResource(R.string.stat_on_time, state.mealsTotal), suffixColor = c.coralMuted,
            onClick = onOpenDiet, onClickLabel = stringResource(R.string.open_diet),
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier, bg: Color, label: String, labelColor: Color,
    value: Int, valueColor: Color, suffix: String, suffixColor: Color,
    onClick: (() -> Unit)? = null, onClickLabel: String? = null,
) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick) else Modifier)
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = manrope(11.5.sp, FontWeight.Medium, letterSpacing = .7.sp), color = labelColor, modifier = Modifier.weight(1f))
            if (onClick != null) Text("›", style = manrope(16.sp, FontWeight.Bold), color = labelColor)
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text("$value", style = manrope(22.sp, FontWeight.ExtraBold), color = valueColor)
            Text(" $suffix", style = manrope(13.sp, FontWeight.SemiBold), color = suffixColor, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleReminderCard(
    r: Reminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val c = NudgeTheme.colors
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(c.coralContainer).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.delete), tint = c.coralDeep)
            }
        },
    ) {
        ReminderCard(r, onToggle, onTap, onLongPress)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderCard(r: Reminder, onToggle: (Boolean) -> Unit, onTap: () -> Unit, onLongPress: () -> Unit) {
    val c = NudgeTheme.colors
    val res = LocalContext.current.resources
    Card24(Modifier.fillMaxWidth().alpha(if (r.enabled) 1f else .7f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.weight(1f).combinedClickable(onClick = onTap, onLongClick = onLongPress),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (r.isMeal) {
                    IconTile(R.drawable.ic_fork, bg = if (r.enabled) c.coralContainer else c.chip, tint = if (r.enabled) c.coralDeep else c.faint)
                } else {
                    IconTile(R.drawable.ic_drop, bg = if (r.enabled) c.tealContainer else c.chip, tint = if (r.enabled) c.tealDeep else c.faint)
                }
                Column(Modifier.weight(1f)) {
                    Text(r.label, style = manrope(16.5.sp, FontWeight.Bold), color = if (r.enabled) c.ink else c.body)
                    Text(ReminderFormat.schedule(res, r), style = manrope(13.5.sp, FontWeight.Medium), color = c.muted, modifier = Modifier.padding(top = 3.dp))
                }
            }
            NudgeSwitch(checked = r.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun PermissionBanners() {
    val ctx = LocalContext.current
    val c = NudgeTheme.colors
    val banners = buildList {
        if (!Permissions.canPostNotifications(ctx)) add(Triple(R.string.perm_notifications_title, R.string.perm_notifications_body, Permissions.notificationSettingsIntent(ctx)))
        if (!Permissions.canFullScreen(ctx)) add(Triple(R.string.perm_fullscreen_title, R.string.perm_fullscreen_body, Permissions.fullScreenSettingsIntent(ctx)))
        if (!Permissions.canExactAlarms(ctx)) add(Triple(R.string.perm_exact_title, R.string.perm_exact_body, Permissions.exactAlarmSettingsIntent(ctx)))
    }
    if (banners.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
        banners.forEach { (title, body, intent) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.coralContainer).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(title), style = manrope(15.sp, FontWeight.Bold), color = c.coralOnContainer)
                    Text(stringResource(body), style = manrope(13.sp, FontWeight.Medium), color = c.coralMuted, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    stringResource(R.string.perm_fix),
                    style = manrope(14.sp, FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.coral).clickable { ctx.startActivity(intent) }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val c = NudgeTheme.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(200.dp).clip(CircleShape).background(c.chip))
            Box(
                Modifier.align(Alignment.TopStart).offset(x = 18.dp, y = 34.dp).size(78.dp).rotate(-8f)
                    .clip(RoundedCornerShape(26.dp)).background(c.tealContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_drop), null, tint = c.tealDeep, modifier = Modifier.size(36.dp)) }
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = (-16).dp, y = (-30).dp).size(88.dp).rotate(7f)
                    .clip(RoundedCornerShape(28.dp)).background(c.coralContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_fork), null, tint = c.coralDeep, modifier = Modifier.size(40.dp)) }
        }
        Spacer(Modifier.height(26.dp))
        Text(stringResource(R.string.empty_title), style = manrope(23.sp, FontWeight.ExtraBold), color = c.ink, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.empty_body),
            style = manrope(15.sp, FontWeight.Medium, lineHeight = 22.sp),
            color = c.muted, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(26.dp))
        PillButton(
            text = stringResource(R.string.add_first_reminder),
            onClick = onAdd,
            height = 54.dp,
            modifier = Modifier.width(280.dp),
        )
    }
}
