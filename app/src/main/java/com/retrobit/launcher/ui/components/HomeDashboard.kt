@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.retrobit.launcher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.data.LauncherFeatureStore.HomeContentLayout
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun HomeDashboard(
    apps: List<AppModel>,
    homeShortcutPackages: List<String>,
    widgetIds: List<Int>,
    widgetHost: AppWidgetHost,
    iconSizeDp: Float,
    shortcutLayout: HomeContentLayout,
    widgetLayout: HomeContentLayout,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onLaunchApp: (AppModel) -> Unit,
    onChangeIcon: (AppModel) -> Unit,
    onAddApp: () -> Unit,
    onAddWidget: () -> Unit,
    onRemoveShortcut: (String) -> Unit,
    onMoveShortcut: (String, Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onShortcutLayoutChange: (HomeContentLayout) -> Unit,
    onWidgetLayoutChange: (HomeContentLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val appByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val shortcuts = remember(apps, homeShortcutPackages) {
        homeShortcutPackages.mapNotNull { appByPackage[it] }
    }
    val manualShortcutRows = remember(shortcuts) { shortcuts.chunked(4) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        // Match the 14dp horizontal chassis inset used by the telemetry and weather cards.
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
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
                    Spacer(Modifier.height(6.dp))
                    Text("LAYOUT // choose each section's direction", color = colors.inkMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    HomeLayoutSelector("APP ICONS", shortcutLayout, onShortcutLayoutChange)
                    HomeLayoutSelector("WIDGETS", widgetLayout, onWidgetLayoutChange)
                    Text("MANUAL: ↑ ↓ arrange; × removes. Hold an app icon to customize it.", color = colors.inkMuted, fontSize = 7.sp)
                }
            }
        }

        item(key = "home-shortcuts-title") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                HomeSectionLabel("PINNED APPS // ${shortcutLayout.label}")
                if (shortcuts.isNotEmpty()) Text("${shortcuts.size} APPS", color = colors.inkMuted, fontSize = 7.sp)
            }
        }

        if (shortcuts.isEmpty()) {
            item(key = "home-shortcuts-empty") {
                HomeEmptyState("No home shortcuts yet. Tap + APP or pin an app from its app-drawer menu.")
            }
        } else when (shortcutLayout) {
            HomeContentLayout.HORIZONTAL -> {
                item(key = "home-shortcuts-horizontal") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(shortcuts, key = { _, app -> "shortcut-${app.packageName}" }) { _, app ->
                            HomeShortcutTile(
                                app = app,
                                iconSizeDp = iconSizeDp,
                                layout = HomeContentLayout.HORIZONTAL,
                                isEditing = isEditing,
                                modifier = Modifier.width(76.dp),
                                onLaunch = { onLaunchApp(app) },
                                onChangeIcon = { onChangeIcon(app) },
                                onRemove = { onRemoveShortcut(app.packageName) },
                                onMove = { offset -> onMoveShortcut(app.packageName, offset) }
                            )
                        }
                    }
                }
            }
            HomeContentLayout.VERTICAL -> {
                itemsIndexed(shortcuts, key = { _, app -> "shortcut-${app.packageName}" }) { _, app ->
                    HomeShortcutTile(
                        app = app,
                        iconSizeDp = iconSizeDp,
                        layout = HomeContentLayout.VERTICAL,
                        isEditing = isEditing,
                        modifier = Modifier.fillMaxWidth(),
                        onLaunch = { onLaunchApp(app) },
                        onChangeIcon = { onChangeIcon(app) },
                        onRemove = { onRemoveShortcut(app.packageName) },
                        onMove = { offset -> onMoveShortcut(app.packageName, offset) }
                    )
                }
            }
            HomeContentLayout.MANUAL -> {
                itemsIndexed(manualShortcutRows, key = { _, row -> "shortcut-row-${row.first().packageName}" }) { _, row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        row.forEach { app ->
                            HomeShortcutTile(
                                app = app,
                                iconSizeDp = iconSizeDp,
                                layout = HomeContentLayout.MANUAL,
                                isEditing = isEditing,
                                modifier = Modifier.weight(1f),
                                onLaunch = { onLaunchApp(app) },
                                onChangeIcon = { onChangeIcon(app) },
                                onRemove = { onRemoveShortcut(app.packageName) },
                                onMove = { offset -> onMoveShortcut(app.packageName, offset) }
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        item(key = "home-widgets-title") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                HomeSectionLabel("WIDGET STACK // ${widgetLayout.label}")
                if (widgetIds.isNotEmpty()) Text("${widgetIds.size} ITEMS", color = colors.inkMuted, fontSize = 7.sp)
            }
        }

        if (widgetIds.isEmpty()) {
            item(key = "home-widgets-empty") {
                HomeEmptyState("No widgets added. Use + WIDGET to choose Android widgets.")
            }
        } else if (widgetLayout == HomeContentLayout.HORIZONTAL) {
            item(key = "home-widgets-horizontal") {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cardWidth = (maxWidth * 0.88f).coerceAtMost(360.dp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(widgetIds, key = { _, id -> "widget-$id" }) { _, widgetId ->
                            HomeWidgetCard(
                                widgetId = widgetId,
                                widgetHost = widgetHost,
                                manager = manager,
                                isEditing = isEditing,
                                showMoveControls = false,
                                modifier = Modifier.width(cardWidth),
                                onRemove = { onRemoveWidget(widgetId) },
                                onMove = { offset -> onMoveWidget(widgetId, offset) }
                            )
                        }
                    }
                }
            }
        } else {
            itemsIndexed(widgetIds, key = { _, id -> "widget-$id" }) { _, widgetId ->
                HomeWidgetCard(
                    widgetId = widgetId,
                    widgetHost = widgetHost,
                    manager = manager,
                    isEditing = isEditing,
                    showMoveControls = widgetLayout == HomeContentLayout.MANUAL,
                    modifier = Modifier.fillMaxWidth(),
                    onRemove = { onRemoveWidget(widgetId) },
                    onMove = { offset -> onMoveWidget(widgetId, offset) }
                )
            }
        }

        item(key = "home-scroll-hint") {
            Text(
                "SCROLL THE DECK ↑↓ // long press anywhere for edit; fast swipes run your gesture actions",
                color = colors.inkMuted,
                fontSize = 7.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun HomeLayoutSelector(
    title: String,
    selected: HomeContentLayout,
    onSelect: (HomeContentLayout) -> Unit
) {
    val colors = LocalRetroBitColors.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = colors.inkSecondary, fontSize = 6.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(62.dp))
        HomeContentLayout.entries.forEach { layout ->
            val isSelected = selected == layout
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) colors.inkPrimary else colors.background)
                    .border(1.dp, colors.border)
                    .clickable { onSelect(layout) }
                    .padding(horizontal = 2.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    layout.label,
                    color = if (isSelected) colors.background else colors.inkSecondary,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HomeShortcutTile(
    app: AppModel,
    iconSizeDp: Float,
    layout: HomeContentLayout,
    isEditing: Boolean,
    modifier: Modifier,
    onLaunch: () -> Unit,
    onChangeIcon: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit
) {
    val colors = LocalRetroBitColors.current
    val bitmap = remember(app.icon) { app.icon?.let { drawableToGrayscaleBitmap(it, 72, 72) } }
    val iconSize = iconSizeDp.coerceIn(28f, 52f).dp
    val icon = @Composable {
        Box(
            Modifier.size(iconSize)
                .background(colors.surface)
                .border(1.dp, colors.border)
                .combinedClickable(onClick = onLaunch, onLongClick = onChangeIcon),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), contentDescription = app.displayLabel, modifier = Modifier.size(iconSizeDp.coerceIn(28f, 52f).times(0.72f).dp))
            } else {
                Text(app.displayLabel.take(2).uppercase(), color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    val controls = @Composable {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (isEditing) MiniHomeControl("◈") { onChangeIcon() }
            if (isEditing && layout == HomeContentLayout.MANUAL) {
                MiniHomeControl("↑") { onMove(-1) }
                MiniHomeControl("↓") { onMove(1) }
            }
            if (isEditing) MiniHomeControl("×") { onRemove() }
        }
    }

    if (layout == HomeContentLayout.VERTICAL) {
        Row(modifier = modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Text(
                app.displayLabel,
                color = colors.inkPrimary,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            controls()
        }
    } else {
        Column(modifier = modifier.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Text(
                app.displayLabel,
                color = colors.inkPrimary,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                textAlign = TextAlign.Center
            )
            if (isEditing) controls()
        }
    }
}

@Composable
private fun HomeWidgetCard(
    widgetId: Int,
    widgetHost: AppWidgetHost,
    manager: AppWidgetManager,
    isEditing: Boolean,
    showMoveControls: Boolean,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val providerInfo = remember(widgetId, manager) { manager.getAppWidgetInfo(widgetId) }
    val label = remember(providerInfo) {
        providerInfo?.loadLabel(context.packageManager)?.toString() ?: "UNAVAILABLE WIDGET"
    }
    Column(modifier.background(colors.surface).border(1.dp, colors.border).padding(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), color = colors.inkSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (showMoveControls) {
                        MiniHomeControl("↑") { onMove(-1) }
                        MiniHomeControl("↓") { onMove(1) }
                    }
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
private fun HomeEmptyState(text: String) {
    val colors = LocalRetroBitColors.current
    Text(
        text,
        color = colors.inkMuted,
        fontSize = 9.sp,
        modifier = Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(10.dp)
    )
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
