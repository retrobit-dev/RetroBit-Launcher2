package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private data class PeriodProgress(
    val fraction: Float,
    val detail: String
)

@Composable
fun TimeProgressWidget(modifier: Modifier = Modifier) {
    val colors = LocalRetroBitColors.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val millisToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(millisToNextMinute)
            nowMillis = System.currentTimeMillis()
        }
    }

    val now = remember(nowMillis) { Calendar.getInstance().apply { timeInMillis = nowMillis } }
    val dayStart = remember(nowMillis) {
        (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val dayEnd = remember(nowMillis) { (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) } }
    val monthStart = remember(nowMillis) {
        (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val nextMonth = remember(nowMillis) { (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }
    val yearStart = remember(nowMillis) {
        (now.clone() as Calendar).apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val nextYear = remember(nowMillis) { (yearStart.clone() as Calendar).apply { add(Calendar.YEAR, 1) } }

    val daily = PeriodProgress(
        fraction = intervalFraction(dayStart.timeInMillis, dayEnd.timeInMillis, nowMillis),
        detail = String.format(Locale.getDefault(), "%02d:%02d LOCAL", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
    )
    val monthly = PeriodProgress(
        fraction = intervalFraction(monthStart.timeInMillis, nextMonth.timeInMillis, nowMillis),
        detail = "DAY ${now.get(Calendar.DAY_OF_MONTH)} / ${now.getActualMaximum(Calendar.DAY_OF_MONTH)}"
    )
    val yearly = PeriodProgress(
        fraction = intervalFraction(yearStart.timeInMillis, nextYear.timeInMillis, nowMillis),
        detail = "DAY ${now.get(Calendar.DAY_OF_YEAR)} / ${now.getActualMaximum(Calendar.DAY_OF_YEAR)}"
    )

    Column(
        modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("TIME PASSED // LOCAL", color = colors.inkSecondary, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold)
        TimeProgressRow("DAY", daily, colors.inkPrimary, colors.inkMuted, colors.subtle, colors.border)
        TimeProgressRow("MONTH", monthly, colors.inkPrimary, colors.inkMuted, colors.subtle, colors.border)
        TimeProgressRow("YEAR", yearly, colors.inkPrimary, colors.inkMuted, colors.subtle, colors.border)
    }
}

@Composable
private fun TimeProgressRow(
    title: String,
    progress: PeriodProgress,
    ink: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color,
    track: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = ink, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(42.dp))
            Text(progress.detail, color = muted, fontSize = 7.sp, modifier = Modifier.weight(1f))
            Text("${(progress.fraction * 100f).roundToInt()}%", color = ink, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(track).border(0.5.dp, border)) {
            Box(
                Modifier.fillMaxWidth(progress.fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(ink)
            )
        }
    }
}

private fun intervalFraction(startMillis: Long, endMillis: Long, nowMillis: Long): Float =
    ((nowMillis - startMillis).toDouble() / (endMillis - startMillis).toDouble())
        .toFloat()
        .coerceIn(0f, 1f)
