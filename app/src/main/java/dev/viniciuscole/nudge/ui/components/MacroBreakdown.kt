package dev.viniciuscole.nudge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.viniciuscole.nudge.R
import dev.viniciuscole.nudge.data.model.MealTotals
import dev.viniciuscole.nudge.ui.theme.NudgeTheme
import dev.viniciuscole.nudge.ui.theme.manrope

@Composable
fun MacroBreakdown(t: MealTotals, modifier: Modifier = Modifier, showPercent: Boolean = false) {
    val c = NudgeTheme.colors
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)).background(c.chip)) {
            if (t.proteinPct > 0) Box(Modifier.weight(t.proteinPct.toFloat()).fillMaxSize().background(c.coral))
            if (t.carbsPct > 0) Box(Modifier.weight(t.carbsPct.toFloat()).fillMaxSize().background(c.carbs))
            if (t.fatPct > 0) Box(Modifier.weight(t.fatPct.toFloat()).fillMaxSize().background(c.teal))
        }
        val protein = stringResource(R.string.protein_g, t.proteinG)
        val carbs = stringResource(R.string.carbs_g, t.carbsG)
        val fat = stringResource(R.string.fat_g, t.fatG)
        if (showPercent) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Legend(c.coral, "$protein · ${t.proteinPct}%")
                Legend(c.carbs, "$carbs · ${t.carbsPct}%")
                Legend(c.teal, "$fat · ${t.fatPct}%")
            }
        } else {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(c.coral, protein)
                Legend(c.carbs, carbs)
                Legend(c.teal, fat)
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
