@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.retrobit.launcher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun HomeDashboard(
    apps: List<AppModel>,
    homeShortcutPackages: List<String>,
    widgetIds: List<Int>,
    widgetHost: AppWidgetHost,
    iconSizeDp: Float,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onLaunchApp: (AppModel) -> Unit,
    onAddApp: () -> Unit,
    onAddWidget: () -> Unit,
    onRemoveShortcut: (String) -> Unit,
    onMoveShortcut: (String, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val appByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val shortcuts = remember(apps, homeShortcutPackages) {
        homeShortcutPackages.mapNotNull { appByPackage[it] }
    }
    val shortcutRows = remember(shortcuts) { shortcuts.chunked(4) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        item(key = "home-title") {
            Column(Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("HOME // PERSONAL DECK", color = colors.inkPrimary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Apps, widgets, and quick gestures in one scrollable workspace.", color = colors.inkSecondary, fontSize = 8.sp)
                    }
                    Text(
                        if (isEditing) "[DONE]" else "[EDIT]",
                        color = colors.inkPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.combinedClickable(onClick = onToggleEdit, onLongClick = onToggleEdit).padding(6.dp)
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HomeAction("+ APP", Modifier.weight(1f), onAddApp)
                    HomeAction("+ WIDGET", Modifier.weight(1f), onAddWidget)
                }
                if (isEditing) {
                    Text("EDIT MODE // use ↑ ↓ to reorder; × removes a shortcut or widget.", color = colors.inkMuted, fontSize = 7.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }

        item(key = "home-shortcuts-title") {
            HomeSectionLabel("PINNED APPS // HOME SHORTCUTS")
        }

        if (shortcutRows.isEmpty()) {
            item(key = "home-shortcuts-empty") {
                Text("No home shortcuts yet. Tap + APP or pin an app from its app-drawer menu.", color = colors.inkMuted, fontSize = 9.sp)
            }
        } else {
            itemsIndexed(shortcutRows, key = { _, row -> "shortcut-row-${row.first().packageName}" }) { _, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEachIndexed { index, app ->
                        HomeShortcutTile(
                            app = app,
                            iconSizeDp = iconSizeDp,
                            isEditing = isEditing,
                            modifier = Modifier.weight(1f),
                            onLaunch = { onLaunchApp(app) },
                            onRemove = { onRemoveShortcut(app.packageName) },
                            onMove = { offset -> onMoveShortcut(app.packageName, offset) }
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        item(key = "home-widgets-title") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                HomeSectionLabel("WIDGET STACK // SCROLLABLE")
                if (widgetIds.isNotEmpty()) Text("${widgetIds.size} ITEMS", color = colors.inkMuted, fontSize = 7.sp)
            }
        }

        if (widgetIds.isEmpty()) {
            item(key = "home-widgets-empty") {
                Text("No widgets added. Use + WIDGET to choose Android widgets; they will stack here and scroll with the home page.", color = colors.inkMuted, fontSize = 9.sp)
            }
        } else {
            itemsIndexed(widgetIds, key = { _, id -> "widget-$id" }) { index, widgetId ->
                HomeWidgetCard(
                    widgetId = widgetId,
                    widgetHost = widgetHost,
                    manager = manager,
                    isEditing = isEditing,
                    onRemove = { onRemoveWidget(widgetId) },
                    onMove = { offset -> onMoveWidget(widgetId, offset) }
                )
            }
        }

        item(key = "home-scroll-hint") {
            Text(
                "SCROLL THE DECK ↑↓ // long-press the gesture pad below to edit",
                color = colors.inkMuted,
                fontSize = 7.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun HomeShortcutTile(
    app: AppModel,
    iconSizeDp: Float,
    isEditing: Boolean,
    modifier: Modifier,
    onLaunch: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit
) {
    val colors = LocalRetroBitColors.current
    val bitmap = remember(app.icon) { app.icon?.let { drawableToGrayscaleBitmap(it, 72, 72) } }
    Column(modifier = modifier.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(iconSizeDp.coerceIn(28f, 52f).dp)
                .background(colors.surface).border(1.dp, colors.border)
                .combinedClickable(onClick = onLaunch, onLongClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), contentDescription = app.displayLabel, modifier = Modifier.size((iconSizeDp.coerceIn(28f, 52f) * 0.72f).dp))
            } else {
                Text(app.displayLabel.take(2).uppercase(), color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            app.displayLabel,
            color = colors.inkPrimary,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
        if (isEditing) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                MiniHomeControl("↑") { onMove(-1) }
                MiniHomeControl("↓") { onMove(1) }
                MiniHomeControl("×") { onRemove() }
            }
        }
    }
}

@Composable
private fun HomeWidgetCard(
    widgetId: Int,
    widgetHost: AppWidgetHost,
    manager: AppWidgetManager,
    isEditing: Boolean,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val providerInfo = remember(widgetId, manager) { manager.getAppWidgetInfo(widgetId) }
    val label = remember(providerInfo) {
        providerInfo?.loadLabel(context.packageManager)?.toString() ?: "UNAVAILABLE WIDGET"
    }
    Column(Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), color = colors.inkSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    MiniHomeControl("↑") { onMove(-1) }
                    MiniHomeControl("↓") { onMove(1) }
                    MiniHomeControl("×") { onRemove() }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (providerInfo == null) {
            Text("Widget provider is no longer available. Remove this item in edit mode.", color = colors.inkMuted, fontSize = 8.sp)
        } else {
            val minHeight = (providerInfo.minHeight + 12).coerceIn(120, 480).dp
            AndroidView(
                factory = { viewContext -> widgetHost.createView(viewContext, widgetId, providerInfo) },
                modifier = Modifier.fillMaxWidth().heightIn(min = minHeight, max = 520.dp)
            )
        }
    }
}

@Composable
private fun HomeAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalRetroBitColors.current
    Box(
        modifier.background(colors.background).border(1.dp, colors.border).combinedClickable(onClick = onClick, onLongClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeSectionLabel(text: String) {
    val colors = LocalRetroBitColors.current
    Text(text, color = colors.inkPrimary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
}

@Composable
private fun MiniHomeControl(label: String, onClick: () -> Unit) {
    val colors = LocalRetroBitColors.current
    Text(
        label,
        color = colors.inkPrimary,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.border(1.dp, colors.border).combinedClickable(onClick = onClick, onLongClick = onClick).padding(horizontal = 4.dp, vertical = 2.dp)
    )
}
