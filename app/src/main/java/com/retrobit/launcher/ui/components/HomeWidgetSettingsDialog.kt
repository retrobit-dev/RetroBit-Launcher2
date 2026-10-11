package com.retrobit.launcher.ui.components

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.data.LauncherFeatureStore.HomeWidgetLayout
import com.retrobit.launcher.data.LauncherFeatureStore.HomeWidgetSize
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlin.math.roundToInt

@Composable
fun HomeWidgetSettingsDialog(
    widgetIds: List<Int>,
    widgetSizes: Map<Int, HomeWidgetSize>,
    widgetLayout: HomeWidgetLayout,
    widgetManager: AppWidgetManager,
    onAddWidget: () -> Unit,
    onLayoutChange: (HomeWidgetLayout) -> Unit,
    onWidgetSizeChange: (Int, HomeWidgetSize) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val layoutChoices = listOf(
        HomeWidgetLayout.STACK to ("STACK + SWIPE" to "One widget at a time. Swipe left or right to switch."),
        HomeWidgetLayout.HORIZONTAL to ("SIDE SCROLL" to "Keep widgets in a row and scroll sideways."),
        HomeWidgetLayout.VERTICAL to ("VERTICAL" to "Place widgets one below another."),
        HomeWidgetLayout.MANUAL to ("MANUAL ORDER" to "Arrange the widget order with the arrows below.")
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f)
                .background(colors.background).border(2.dp, colors.border).padding(11.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("WIDGETS // HOME", color = colors.inkPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${widgetIds.size} ${if (widgetIds.size == 1) "WIDGET" else "WIDGETS"} · ${layoutTitle(widgetLayout)}",
                        color = colors.inkSecondary,
                        fontSize = 8.sp
                    )
                }
                Text(
                    "[DONE]",
                    color = colors.inkPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(6.dp)
                )
            }

            Spacer(Modifier.height(9.dp))
            Text("HOW WIDGETS APPEAR", color = colors.inkMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                layoutChoices.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { (layout, copy) ->
                            WidgetLayoutChoice(
                                title = copy.first,
                                detail = copy.second,
                                selected = widgetLayout == layout,
                                modifier = Modifier.weight(1f),
                                onClick = { onLayoutChange(layout) }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                when (widgetLayout) {
                    HomeWidgetLayout.STACK -> "HOME: swipe ← → to browse the stack. Use arrows below to set its order."
                    HomeWidgetLayout.HORIZONTAL -> "HOME: swipe ← → across the widget row."
                    HomeWidgetLayout.VERTICAL -> "HOME: scroll ↑ ↓ through the widget list."
                    HomeWidgetLayout.MANUAL -> "HOME: use ↑ ↓ below to choose the widget order."
                },
                color = colors.inkSecondary,
                fontSize = 7.sp,
                modifier = Modifier.fillMaxWidth().background(colors.subtle).padding(horizontal = 7.dp, vertical = 6.dp)
            )

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("WIDGET ORDER & SIZE", color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Drag the sliders to resize each widget.", color = colors.inkMuted, fontSize = 7.sp)
                }
                WidgetSettingsAction("+ ADD", onClick = onAddWidget)
            }
            Spacer(Modifier.height(5.dp))

            if (widgetIds.isEmpty()) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().background(colors.surface).border(1.dp, colors.border)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("NO WIDGETS YET", color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Add a widget to build a swipeable stack or scrolling row.", color = colors.inkMuted, fontSize = 8.sp)
                    Spacer(Modifier.height(8.dp))
                    WidgetSettingsAction("+ ADD WIDGET", onClick = onAddWidget)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(widgetIds, key = { _, id -> "widget-settings-$id" }) { index, widgetId ->
                        WidgetSettingsCard(
                            widgetId = widgetId,
                            position = index,
                            total = widgetIds.size,
                            widgetLayout = widgetLayout,
                            savedSize = widgetSizes[widgetId],
                            widgetManager = widgetManager,
                            onMove = { offset -> onMoveWidget(widgetId, offset) },
                            onSizeChange = { onWidgetSizeChange(widgetId, it) },
                            onRemove = { onRemoveWidget(widgetId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetLayoutChoice(
    title: String,
    detail: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    Column(
        modifier.background(if (selected) colors.subtle else colors.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.inkPrimary else colors.border)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (selected) "●" else "○", color = colors.inkPrimary, fontSize = 8.sp)
        }
        Text(detail, color = colors.inkSecondary, fontSize = 7.sp, lineHeight = 9.sp)
    }
}

@Composable
private fun WidgetSettingsCard(
    widgetId: Int,
    position: Int,
    total: Int,
    widgetLayout: HomeWidgetLayout,
    savedSize: HomeWidgetSize?,
    widgetManager: AppWidgetManager,
    onMove: (Int) -> Unit,
    onSizeChange: (HomeWidgetSize) -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val providerInfo = remember(widgetId, widgetManager) { widgetManager.getAppWidgetInfo(widgetId) }
    val providerLabel = remember(providerInfo, context) {
        runCatching { providerInfo?.loadLabel(context.packageManager)?.toString() }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: "UNAVAILABLE WIDGET"
    }
    val minimumHeight = ((providerInfo?.minHeight ?: 0) + 12).coerceIn(120, 480)
    val initialSize = (savedSize ?: HomeWidgetSize(heightDp = minimumHeight)).copy(
        widthPercent = (savedSize?.widthPercent ?: 100).coerceIn(50, 100),
        heightDp = (savedSize?.heightDp ?: minimumHeight).coerceIn(minimumHeight, 600)
    )
    var draftSize by remember(widgetId, initialSize) { mutableStateOf(initialSize) }
    val positionLabel = if (widgetLayout == HomeWidgetLayout.STACK) "STACK ${position + 1}/$total" else "ITEM ${position + 1}/$total"

    Column(
        Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    providerLabel.uppercase(),
                    color = colors.inkPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    providerInfo?.provider?.packageName ?: "Widget provider unavailable",
                    color = colors.inkMuted,
                    fontSize = 7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(5.dp))
            Text(positionLabel, color = colors.inkSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ORDER", color = colors.inkMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(5.dp))
            WidgetSettingsAction("↑", onClick = { onMove(-1) }, enabled = position > 0)
            Spacer(Modifier.width(3.dp))
            WidgetSettingsAction("↓", onClick = { onMove(1) }, enabled = position < total - 1)
            Spacer(Modifier.weight(1f))
            WidgetSettingsAction("REMOVE", onClick = onRemove)
        }

        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("WIDTH", color = colors.inkSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${draftSize.widthPercent}%", color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(5.dp))
            Text(
                "RESET",
                color = colors.inkSecondary,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    val reset = HomeWidgetSize(widthPercent = 100, heightDp = minimumHeight)
                    draftSize = reset
                    onSizeChange(reset)
                }.padding(3.dp)
            )
        }
        Slider(
            value = draftSize.widthPercent.toFloat(),
            onValueChange = { value -> draftSize = draftSize.copy(widthPercent = value.roundToInt().coerceIn(50, 100)) },
            onValueChangeFinished = { onSizeChange(draftSize) },
            valueRange = 50f..100f,
            steps = 1
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HEIGHT", color = colors.inkSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${draftSize.heightDp} dp", color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = draftSize.heightDp.toFloat(),
            onValueChange = { value -> draftSize = draftSize.copy(heightDp = value.roundToInt().coerceIn(minimumHeight, 600)) },
            onValueChangeFinished = { onSizeChange(draftSize) },
            valueRange = minimumHeight.toFloat()..600f,
            steps = 11
        )
    }
}

@Composable
private fun WidgetSettingsAction(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = LocalRetroBitColors.current
    Text(
        label,
        color = if (enabled) colors.inkPrimary else colors.inkMuted,
        fontSize = 7.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.border(1.dp, if (enabled) colors.border else colors.inkMuted)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp)
    )
}

private fun layoutTitle(layout: HomeWidgetLayout): String = when (layout) {
    HomeWidgetLayout.STACK -> "STACK + SWIPE"
    HomeWidgetLayout.HORIZONTAL -> "SIDE SCROLL"
    HomeWidgetLayout.VERTICAL -> "VERTICAL"
    HomeWidgetLayout.MANUAL -> "MANUAL"
}
