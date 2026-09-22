package dev.viniciuscole.nudge.ui.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.food.FoodItem
import dev.viniciuscole.nudge.data.model.Ingredient
import dev.viniciuscole.nudge.data.model.MealTotals
import dev.viniciuscole.nudge.ui.components.Card24
import dev.viniciuscole.nudge.ui.components.CircleIconButton
import dev.viniciuscole.nudge.ui.components.CircleTextButton
import dev.viniciuscole.nudge.ui.components.PillButton
import dev.viniciuscole.nudge.ui.components.SectionLabel
import dev.viniciuscole.nudge.ui.format.ReminderFormat
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope
import kotlin.math.roundToInt

@Composable
fun MealBuilderScreen(vm: MealBuilderViewModel, onBack: () -> Unit) {
    val c = NudgeTheme.colors
    val s by vm.state.collectAsStateWithLifecycle()
    val reminder = s.reminder

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(R.drawable.ic_back, onClick = onBack, contentDescription = stringResource(R.string.back))
            Column {
                Text(reminder?.label ?: "", style = manrope(19.sp, FontWeight.Bold), color = c.ink)
                if (reminder != null) {
                    Text(
                        stringResource(R.string.builder_subtitle, ReminderFormat.time(reminder.hour, reminder.minute)),
                        style = manrope(12.5.sp, FontWeight.Medium), color = c.faint, modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().zIndex(4f).padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
            SearchField(s.query, s.hasQuery, vm::setQuery, vm::clearQuery)
            if (s.hasQuery) {
                SuggestionsDropdown(
                    suggestions = s.suggestions,
                    noResults = s.noResults,
                    onAdd = vm::add,
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = 60.dp),
                )
            }
        }

        if (s.ingredients.isEmpty()) {
            EmptyIngredients(Modifier.weight(1f))
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { SectionLabel(pluralStringResource(R.plurals.ingredient_count, s.ingredients.size, s.ingredients.size)) }
                items(s.ingredients, key = { it.id }) { ing ->
                    IngredientRow(ing, onQty = { vm.setQty(ing.id, it) }, onPlus = { vm.plus(ing.id) }, onMinus = { vm.minus(ing.id) }, onRemove = { vm.remove(ing.id) })
                }
            }
        }

        Column(Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding().imePadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            TotalsCard(s.totals, s.dayShare)
            PillButton(
                text = stringResource(if (s.saved) R.string.meal_saved else R.string.save_meal),
                onClick = { vm.save() },
                modifier = Modifier.padding(top = 12.dp),
                color = if (s.saved) c.teal else c.coral,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, hasQuery: Boolean, onQuery: (String) -> Unit, onClear: () -> Unit) {
    val c = NudgeTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = c.ink.copy(alpha = .04f), spotColor = c.ink.copy(alpha = .06f))
            .clip(shape)
            .background(c.card)
            .border(1.5.dp, if (hasQuery) c.coral else c.line, shape)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_search), null, tint = if (hasQuery) c.coral else c.faint, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = manrope(15.5.sp, FontWeight.SemiBold).copy(color = c.ink),
            cursorBrush = SolidColor(c.coral),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) Text(stringResource(R.string.search_hint), style = manrope(15.5.sp, FontWeight.SemiBold), color = c.faint)
                inner()
            },
        )
        if (hasQuery) {
            CircleIconButton(R.drawable.ic_close, onClick = onClear, size = 24.dp, bg = c.chip, tint = c.muted, iconSize = 12.dp, contentDescription = stringResource(R.string.clear))
        }
    }
}

@Composable
private fun SuggestionsDropdown(suggestions: List<FoodItem>, noResults: Boolean, onAdd: (FoodItem) -> Unit, modifier: Modifier) {
    val c = NudgeTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, shape, ambientColor = c.ink.copy(alpha = .16f), spotColor = c.ink.copy(alpha = .16f))
            .clip(shape)
            .background(c.card)
            .padding(6.dp),
    ) {
        suggestions.forEach { f ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onAdd(f) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(c.coralContainer), contentAlignment = Alignment.Center) {
                    Text(f.initials, style = manrope(12.5.sp, FontWeight.Bold), color = c.coralDeep)
                }
                Column(Modifier.weight(1f)) {
                    Text(f.name, style = manrope(14.5.sp, FontWeight.Bold), color = c.ink, maxLines = 1)
                    Text(stringResource(R.string.per_100, f.kcal100.roundToInt(), f.unit), style = manrope(12.5.sp, FontWeight.Medium), color = c.faint, modifier = Modifier.padding(top = 1.dp))
                }
                Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.add), tint = c.coral, modifier = Modifier.size(18.dp))
            }
        }
        if (noResults) {
            Text(stringResource(R.string.no_match), style = manrope(13.5.sp, FontWeight.Medium), color = c.faint, modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp))
        }
    }
}

@Composable
private fun IngredientRow(ing: Ingredient, onQty: (String) -> Unit, onPlus: () -> Unit, onMinus: () -> Unit, onRemove: () -> Unit) {
    val c = NudgeTheme.colors
    Card24(Modifier.fillMaxWidth(), radius = 20.dp) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(ing.name, style = manrope(15.5.sp, FontWeight.Bold), color = c.ink, maxLines = 1)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(c.field).padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BasicTextField(
                            value = ing.qty.toString(),
                            onValueChange = onQty,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = manrope(14.sp, FontWeight.Bold).copy(color = c.ink, textAlign = TextAlign.End),
                            cursorBrush = SolidColor(c.coral),
                            modifier = Modifier.width(38.dp),
                        )
                        Text(ing.unit, style = manrope(12.5.sp, FontWeight.SemiBold), color = c.muted)
                    }
                    CircleTextButton("−", onClick = onMinus)
                    CircleTextButton("+", onClick = onPlus)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(ing.kcal.roundToInt().toString(), style = manrope(16.sp, FontWeight.ExtraBold), color = c.ink)
                Text(stringResource(R.string.kcal_caps), style = manrope(11.sp, FontWeight.SemiBold, letterSpacing = .7.sp), color = c.faint, modifier = Modifier.padding(top = 4.dp))
            }
            CircleIconButton(R.drawable.ic_close, onClick = onRemove, size = 34.dp, tint = c.line, iconSize = 16.dp, contentDescription = stringResource(R.string.remove))
        }
    }
}

@Composable
private fun EmptyIngredients(modifier: Modifier) {
    val c = NudgeTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 46.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(32.dp)).background(c.chip), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_fork), null, tint = c.faint, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.empty_ingredients_title), style = manrope(20.sp, FontWeight.ExtraBold), color = c.ink, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.empty_ingredients_body),
            style = manrope(14.5.sp, FontWeight.Medium, lineHeight = 21.sp),
            color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun TotalsCard(t: MealTotals, dayShare: Int) {
    val c = NudgeTheme.colors
    Card24(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    SectionLabel(stringResource(R.string.meal_total))
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                        Text(t.kcal.toString(), style = manrope(40.sp, FontWeight.ExtraBold, letterSpacing = (-0.8).sp), color = c.ink)
                        Text(" " + stringResource(R.string.kcal), style = manrope(14.sp, FontWeight.Bold), color = c.muted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                Text(stringResource(R.string.goal_note, dayShare), style = manrope(12.5.sp, FontWeight.SemiBold), color = c.faint, textAlign = TextAlign.End, modifier = Modifier.padding(bottom = 4.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp).clip(RoundedCornerShape(5.dp)).background(c.chip)) {
                if (t.proteinPct > 0) Box(Modifier.weight(t.proteinPct.toFloat()).fillMaxSize().background(c.coral))
                if (t.carbsPct > 0) Box(Modifier.weight(t.carbsPct.toFloat()).fillMaxSize().background(c.carbs))
                if (t.fatPct > 0) Box(Modifier.weight(t.fatPct.toFloat()).fillMaxSize().background(c.teal))
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(c.coral, stringResource(R.string.protein_g, t.proteinG))
                Legend(c.carbs, stringResource(R.string.carbs_g, t.carbsG))
                Legend(c.teal, stringResource(R.string.fat_g, t.fatG))
            }
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    val c = NudgeTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(text, style = manrope(12.5.sp, FontWeight.SemiBold), color = c.body)
    }
}
