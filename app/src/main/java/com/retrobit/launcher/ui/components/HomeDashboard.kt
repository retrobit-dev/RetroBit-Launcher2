@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.retrobit.launcher.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.data.LauncherFeatureStore.HomeContentLayout
import com.retrobit.launcher.data.LauncherFeatureStore.HomeWidgetLayout
import com.retrobit.launcher.data.LauncherFeatureStore.HomeWidgetSize
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

@Composable
fun HomeDashboard(
    apps: List<AppModel>,
    homeShortcutPackages: List<String>,
    widgetIds: List<Int>,
    widgetSizes: Map<Int, HomeWidgetSize>,
    widgetHost: AppWidgetHost,
    iconSizeDp: Float,
    shortcutLayout: HomeContentLayout,
    widgetLayout: HomeWidgetLayout,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onLaunchApp: (AppModel) -> Unit,
    onChangeIcon: (AppModel) -> Unit,
    onAddApp: () -> Unit,
    onAddWidget: () -> Unit,
    onRemoveShortcut: (String) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveShortcut: (String, Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onOpenWidgetSettings: () -> Unit,
    onShortcutLayoutChange: (HomeContentLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val stackScope = rememberCoroutineScope()
    val appByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val shortcuts = remember(apps, homeShortcutPackages) {
        homeShortcutPackages.mapNotNull { appByPackage[it] }
    }
    val manualShortcutRows = remember(shortcuts) { shortcuts.chunked(4) }
    val dragController = remember { HomeCardDragController() }
    val handleCardDrop: (HomeCardDrop) -> Unit = { drop ->
        when (drop) {
            is HomeCardDrop.RemoveShortcut -> onRemoveShortcut(drop.packageName)
            is HomeCardDrop.RemoveWidget -> onRemoveWidget(drop.widgetId)
            is HomeCardDrop.MoveShortcut -> {
                val offset = homeMoveOffsetForDrop(
                    order = homeShortcutPackages,
                    source = drop.packageName,
                    target = drop.targetPackageName,
                    after = drop.after
                )
                if (offset != null) onMoveShortcut(drop.packageName, offset)
            }
            is HomeCardDrop.MoveWidget -> {
                val offset = homeMoveOffsetForDrop(
                    order = widgetIds,
                    source = drop.widgetId,
                    target = drop.targetWidgetId,
                    after = drop.after
                )
                if (offset != null) onMoveWidget(drop.widgetId, offset)
            }
        }
    }
    val currentOnCardDrop by rememberUpdatedState(handleCardDrop)

    Box(
        modifier = modifier.fillMaxSize().onGloballyPositioned(dragController::updateHomeBounds)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
        item(key = "home-title") {
            Column(Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("HOME // PERSONAL DECK", color = colors.inkPrimary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (isEditing) "Edit mode // hold blank space to close; drag cards to sort or remove." else "Swipe right for widgets · hold blank space to edit · hold-drag cards.",
                            color = colors.inkSecondary,
                            fontSize = 8.sp
                        )
                    }
                    if (isEditing) {
                        Text(
                            "[DONE]",
                            color = colors.inkPrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = onToggleEdit).padding(6.dp)
                        )
                    }
                }
                if (isEditing) {
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HomeAction("+ APP", Modifier.weight(1f), onAddApp)
                        HomeAction("+ WIDGET", Modifier.weight(1f), onAddWidget)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("HOME LAYOUT", color = colors.inkMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    HomeLayoutSelector(
                        title = "APP ICONS",
                        selected = shortcutLayout.label,
                        options = HomeContentLayout.entries.map { it.label },
                        onSelect = { label -> HomeContentLayout.entries.firstOrNull { it.label == label }?.let(onShortcutLayoutChange) }
                    )
                    HomeAction("WIDGET SETTINGS  //  STACK · SWIPE · SIZE", Modifier.fillMaxWidth(), onOpenWidgetSettings)
                    Text("Manage widget layout, swipe behavior, order and size in one place.", color = colors.inkMuted, fontSize = 7.sp)
                    Text("Hold and drag cards to reorder or remove; use ◈ to customize icons. Manual arrows remain available.", color = colors.inkMuted, fontSize = 7.sp)
                }
            }
        }

        item(key = "home-time-progress") {
            TimeProgressWidget()
        }

        item(key = "home-widgets-title") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                HomeSectionLabel("WIDGETS // ${widgetLayout.label}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (widgetIds.isNotEmpty()) Text("${widgetIds.size} ITEMS", color = colors.inkMuted, fontSize = 7.sp)
                    if (widgetIds.size > 1 && widgetLayout in setOf(HomeWidgetLayout.STACK, HomeWidgetLayout.HORIZONTAL)) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (widgetLayout == HomeWidgetLayout.STACK) "SWIPE ↔" else "SCROLL ↔",
                            color = colors.inkPrimary,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (widgetIds.isEmpty()) {
            item(key = "home-widgets-empty") {
                HomeEmptyState("No widgets yet. Hold a blank area, then choose + WIDGET to add one.")
            }
        } else when (widgetLayout) {
            HomeWidgetLayout.HORIZONTAL -> {
                item(key = "home-widgets-horizontal") {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val availableWidth = maxWidth
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(widgetIds, key = { _, id -> "widget-$id" }) { _, widgetId ->
                                HomeWidgetFrame(
                                    widgetId = widgetId,
                                    savedSize = widgetSizes[widgetId],
                                    widgetHost = widgetHost,
                                    manager = manager,
                                    availableWidth = availableWidth,
                                    modifier = Modifier.homeCardDragTarget(
                                        controller = dragController,
                                        cardKey = widgetCardKey(widgetId),
                                        cardKind = HomeCardKind.WIDGET,
                                        shortcutLayout = shortcutLayout,
                                        widgetLayout = widgetLayout,
                                        onDrop = currentOnCardDrop
                                    ),
                                    fillAvailableWidth = false,
                                    contentAlignment = Alignment.CenterStart
                                )
                            }
                        }
                    }
                }
            }
            HomeWidgetLayout.VERTICAL, HomeWidgetLayout.MANUAL -> {
                itemsIndexed(widgetIds, key = { _, id -> "widget-$id" }) { _, widgetId ->
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        HomeWidgetFrame(
                            widgetId = widgetId,
                            savedSize = widgetSizes[widgetId],
                            widgetHost = widgetHost,
                            manager = manager,
                            availableWidth = maxWidth,
                            modifier = Modifier.fillMaxWidth().homeCardDragTarget(
                                controller = dragController,
                                cardKey = widgetCardKey(widgetId),
                                cardKind = HomeCardKind.WIDGET,
                                shortcutLayout = shortcutLayout,
                                widgetLayout = widgetLayout,
                                onDrop = currentOnCardDrop
                            ),
                            fillAvailableWidth = true,
                            contentAlignment = Alignment.CenterStart
                        )
                    }
                }
            }
            HomeWidgetLayout.STACK -> {
                item(key = "home-widgets-stack") {
                    val pagerState = rememberPagerState(pageCount = { widgetIds.size })
                    val activeIndex = pagerState.currentPage.coerceIn(0, widgetIds.lastIndex)
                    val activeWidgetId = widgetIds[activeIndex]
                    val activeProvider = remember(activeWidgetId, manager) { manager.getAppWidgetInfo(activeWidgetId) }
                    val activeSize = widgetSizes[activeWidgetId] ?: defaultHomeWidgetSize(activeProvider)
                    val activeHeight = activeSize.heightDp.coerceAtLeast(minimumWidgetBodyHeight(activeProvider)).coerceIn(120, 600)
                    val stackHeight = activeHeight + 38

                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().height(stackHeight.dp),
                            key = { page -> widgetIds[page] }
                        ) { page ->
                            val widgetId = widgetIds[page]
                            BoxWithConstraints(
                                Modifier.fillMaxSize().homeCardDragTarget(
                                    controller = dragController,
                                    cardKey = widgetCardKey(widgetId),
                                    cardKind = HomeCardKind.WIDGET,
                                    shortcutLayout = shortcutLayout,
                                    widgetLayout = widgetLayout,
                                    onDrop = currentOnCardDrop
                                )
                            ) {
                                HomeWidgetFrame(
                                    widgetId = widgetId,
                                    savedSize = widgetSizes[widgetId],
                                    widgetHost = widgetHost,
                                    manager = manager,
                                    availableWidth = maxWidth,
                                    modifier = Modifier.fillMaxSize(),
                                    fillAvailableWidth = true,
                                    contentAlignment = Alignment.Center
                                )
                            }
                        }
                        if (widgetIds.size > 1) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("SWIPE ← → TO SWITCH WIDGETS", color = colors.inkMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "◀",
                                        color = colors.inkPrimary,
                                        modifier = Modifier.clickable {
                                            stackScope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                                        }.padding(4.dp)
                                    )
                                    Text("${activeIndex + 1}/${widgetIds.size}", color = colors.inkSecondary, fontSize = 8.sp)
                                    Text(
                                        "▶",
                                        color = colors.inkPrimary,
                                        modifier = Modifier.clickable {
                                            stackScope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(widgetIds.lastIndex)) }
                                        }.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
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
                HomeEmptyState("No pinned apps. Hold a blank area to add or arrange apps.")
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
                                modifier = Modifier.width(76.dp).homeCardDragTarget(
                                    controller = dragController,
                                    cardKey = shortcutCardKey(app.packageName),
                                    cardKind = HomeCardKind.SHORTCUT,
                                    shortcutLayout = shortcutLayout,
                                    widgetLayout = widgetLayout,
                                    onDrop = currentOnCardDrop
                                ),
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
                        modifier = Modifier.fillMaxWidth().homeCardDragTarget(
                            controller = dragController,
                            cardKey = shortcutCardKey(app.packageName),
                            cardKind = HomeCardKind.SHORTCUT,
                            shortcutLayout = shortcutLayout,
                            widgetLayout = widgetLayout,
                            onDrop = currentOnCardDrop
                        ),
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
                                modifier = Modifier.weight(1f).homeCardDragTarget(
                                    controller = dragController,
                                    cardKey = shortcutCardKey(app.packageName),
                                    cardKind = HomeCardKind.SHORTCUT,
                                    shortcutLayout = shortcutLayout,
                                    widgetLayout = widgetLayout,
                                    onDrop = currentOnCardDrop
                                ),
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

        item(key = "home-scroll-hint") {
            Text(
                "SCROLL THE DECK ↑↓ // swipe right for WIDGET PAGE · hold-drag cards to sort/remove",
                color = colors.inkMuted,
                fontSize = 7.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
        }

        if (dragController.activeCard != null) {
            val hoveringRemovalArea = dragController.isPointerOverRemovalArea
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(if (hoveringRemovalArea) colors.inkPrimary else colors.surface)
                    .border(2.dp, colors.inkPrimary)
                    .onGloballyPositioned(dragController::updateRemovalBounds),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⌫  DROP HERE TO REMOVE",
                    color = if (hoveringRemovalArea) colors.background else colors.inkPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

private enum class HomeCardKind { SHORTCUT, WIDGET }

private data class ActiveHomeCardDrag(
    val cardKey: String,
    val kind: HomeCardKind,
    val pointerInRoot: Offset
)

private sealed class HomeCardDrop {
    data class RemoveShortcut(val packageName: String) : HomeCardDrop()
    data class RemoveWidget(val widgetId: Int) : HomeCardDrop()
    data class MoveShortcut(val packageName: String, val targetPackageName: String?, val after: Boolean) : HomeCardDrop()
    data class MoveWidget(val widgetId: Int, val targetWidgetId: Int?, val after: Boolean) : HomeCardDrop()
}

private class HomeCardDragController {
    private val cardBounds = mutableStateMapOf<String, Rect>()

    var activeCard by mutableStateOf<ActiveHomeCardDrag?>(null)
        private set
    var activeDropTargetKey by mutableStateOf<String?>(null)
        private set
    var pointerInRoot by mutableStateOf<Offset?>(null)
        private set
    private var homeBounds by mutableStateOf<Rect?>(null)
    private var removalBounds by mutableStateOf<Rect?>(null)
    private var hoveredCardKey by mutableStateOf<String?>(null)

    val isPointerOverRemovalArea: Boolean
        get() = pointerInRoot?.let { removalBounds?.contains(it) } == true

    fun updateHomeBounds(coordinates: LayoutCoordinates) {
        homeBounds = coordinates.toHomeRect()
    }

    fun updateRemovalBounds(coordinates: LayoutCoordinates) {
        removalBounds = coordinates.toHomeRect()
    }

    fun updateCardBounds(cardKey: String, coordinates: LayoutCoordinates) {
        val bounds = coordinates.toHomeRect()
        if (cardBounds[cardKey] != bounds) {
            cardBounds[cardKey] = bounds
            refreshHoveredCard()
        }
    }

    fun removeCardBounds(cardKey: String) {
        cardBounds.remove(cardKey)
        refreshHoveredCard()
    }

    fun start(cardKey: String, kind: HomeCardKind, pointer: Offset) {
        activeCard = ActiveHomeCardDrag(cardKey, kind, pointer)
        updatePointer(pointer)
    }

    fun updatePointer(pointer: Offset) {
        pointerInRoot = pointer
        activeCard = activeCard?.copy(pointerInRoot = pointer)
        refreshHoveredCard()
    }

    fun cancel() {
        activeCard = null
        activeDropTargetKey = null
        pointerInRoot = null
        hoveredCardKey = null
    }

    fun finish(shortcutLayout: HomeContentLayout, widgetLayout: HomeWidgetLayout): HomeCardDrop? {
        val drag = activeCard ?: return null
        val pointer = pointerInRoot ?: drag.pointerInRoot
        val sourceBounds = cardBounds[drag.cardKey]
        val targetKey = hoveredCardKey
        val targetBounds = targetKey?.let(cardBounds::get)
        val drop = when {
            removalBounds?.contains(pointer) == true -> when (drag.kind) {
                HomeCardKind.SHORTCUT -> HomeCardDrop.RemoveShortcut(drag.cardKey.substringAfter(':'))
                HomeCardKind.WIDGET -> drag.cardKey.substringAfter(':').toIntOrNull()?.let { HomeCardDrop.RemoveWidget(it) }
            }
            homeBounds?.contains(pointer) != true -> null
            sourceBounds?.contains(pointer) == true -> null
            targetKey != null && targetBounds != null -> {
                if (kindOfCardKey(targetKey) != drag.kind) null
                else {
                    val after = isAfterTarget(pointer, targetBounds, drag.kind, shortcutLayout, widgetLayout)
                    when (drag.kind) {
                        HomeCardKind.SHORTCUT -> HomeCardDrop.MoveShortcut(
                            packageName = drag.cardKey.substringAfter(':'),
                            targetPackageName = targetKey.substringAfter(':'),
                            after = after
                        )
                        HomeCardKind.WIDGET -> {
                            val sourceId = drag.cardKey.substringAfter(':').toIntOrNull()
                            val targetId = targetKey.substringAfter(':').toIntOrNull()
                            if (sourceId == null || targetId == null) null
                            else HomeCardDrop.MoveWidget(sourceId, targetId, after)
                        }
                    }
                }
            }
            else -> when (drag.kind) {
                HomeCardKind.SHORTCUT -> HomeCardDrop.MoveShortcut(
                    packageName = drag.cardKey.substringAfter(':'),
                    targetPackageName = null,
                    after = true
                )
                HomeCardKind.WIDGET -> drag.cardKey.substringAfter(':').toIntOrNull()?.let {
                    HomeCardDrop.MoveWidget(it, targetWidgetId = null, after = true)
                }
            }
        }
        cancel()
        return drop
    }

    private fun refreshHoveredCard() {
        val active = activeCard
        val pointer = pointerInRoot
        if (active == null || pointer == null) {
            hoveredCardKey = null
            activeDropTargetKey = null
            return
        }
        val hovered = cardBounds.entries.firstOrNull { (key, bounds) ->
            key != active.cardKey && bounds.contains(pointer)
        }?.key
        hoveredCardKey = hovered
        activeDropTargetKey = hovered?.takeIf { kindOfCardKey(it) == active.kind }
    }
}

@Composable
private fun Modifier.homeCardDragTarget(
    controller: HomeCardDragController,
    cardKey: String,
    cardKind: HomeCardKind,
    shortcutLayout: HomeContentLayout,
    widgetLayout: HomeWidgetLayout,
    onDrop: (HomeCardDrop) -> Unit
): Modifier {
    val colors = LocalRetroBitColors.current
    val coordinates = remember(cardKey) { mutableStateOf<LayoutCoordinates?>(null) }
    val latestOnDrop by rememberUpdatedState(onDrop)
    val isDragging = controller.activeCard?.cardKey == cardKey
    val isDropTarget = controller.activeDropTargetKey == cardKey

    DisposableEffect(controller, cardKey) {
        onDispose {
            controller.removeCardBounds(cardKey)
            if (controller.activeCard?.cardKey == cardKey) controller.cancel()
        }
    }

    return this
        .onGloballyPositioned { layoutCoordinates ->
            coordinates.value = layoutCoordinates
            controller.updateCardBounds(cardKey, layoutCoordinates)
        }
        .pointerInput(controller, cardKey, cardKind, shortcutLayout, widgetLayout) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var current = down
                var longPressReached = false
                var movedBeforeLongPress = false
                var dragging = false
                var cancelled = false

                while (current.pressed) {
                    val elapsed = current.uptimeMillis - down.uptimeMillis
                    val remainingLongPress = viewConfiguration.longPressTimeoutMillis - elapsed
                    val event = if (!longPressReached && !movedBeforeLongPress && remainingLongPress > 0L) {
                        withTimeoutOrNull(remainingLongPress) {
                            awaitPointerEvent(PointerEventPass.Initial)
                        }
                    } else {
                        awaitPointerEvent(PointerEventPass.Initial)
                    }

                    if (event == null) {
                        longPressReached = true
                        continue
                    }
                    if (event.changes.count { it.pressed } > 1) {
                        cancelled = true
                        controller.cancel()
                        break
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val distanceFromDown = (change.position - down.position).getDistance()
                    if (!longPressReached && distanceFromDown > viewConfiguration.touchSlop) {
                        movedBeforeLongPress = true
                    }
                    if (
                        longPressReached && !movedBeforeLongPress && !dragging &&
                        distanceFromDown > viewConfiguration.touchSlop
                    ) {
                        val rootPosition = coordinates.value?.localToRoot(change.position)
                        if (rootPosition != null) {
                            controller.start(cardKey, cardKind, rootPosition)
                            dragging = true
                        }
                    }
                    if (dragging) {
                        coordinates.value?.localToRoot(change.position)?.let(controller::updatePointer)
                        change.consume()
                    }
                    current = change
                }

                if (dragging && !cancelled) {
                    controller.finish(shortcutLayout, widgetLayout)?.let(latestOnDrop)
                } else if (cancelled) {
                    controller.cancel()
                }
            }
        }
        .then(if (isDragging) Modifier.graphicsLayer { alpha = 0.55f } else Modifier)
        .then(if (isDropTarget) Modifier.border(2.dp, colors.inkPrimary) else Modifier)
}

private fun LayoutCoordinates.toHomeRect(): Rect {
    val origin = positionInRoot()
    return Rect(
        left = origin.x,
        top = origin.y,
        right = origin.x + size.width.toFloat(),
        bottom = origin.y + size.height.toFloat()
    )
}

private fun shortcutCardKey(packageName: String): String = "shortcut:$packageName"
private fun widgetCardKey(widgetId: Int): String = "widget:$widgetId"

private fun kindOfCardKey(cardKey: String): HomeCardKind? = when {
    cardKey.startsWith("shortcut:") -> HomeCardKind.SHORTCUT
    cardKey.startsWith("widget:") -> HomeCardKind.WIDGET
    else -> null
}

private fun isAfterTarget(
    pointer: Offset,
    bounds: Rect,
    cardKind: HomeCardKind,
    shortcutLayout: HomeContentLayout,
    widgetLayout: HomeWidgetLayout
): Boolean {
    val center = bounds.center
    return when (cardKind) {
        HomeCardKind.SHORTCUT -> when (shortcutLayout) {
            HomeContentLayout.HORIZONTAL -> pointer.x >= center.x
            HomeContentLayout.VERTICAL -> pointer.y >= center.y
            HomeContentLayout.MANUAL -> {
                val verticalOffset = pointer.y - center.y
                if (abs(verticalOffset) > bounds.height * 0.2f) verticalOffset > 0f
                else pointer.x >= center.x
            }
        }
        HomeCardKind.WIDGET -> when (widgetLayout) {
            HomeWidgetLayout.HORIZONTAL, HomeWidgetLayout.STACK -> pointer.x >= center.x
            HomeWidgetLayout.VERTICAL, HomeWidgetLayout.MANUAL -> pointer.y >= center.y
        }
    }
}

private fun <T> homeMoveOffsetForDrop(
    order: List<T>,
    source: T,
    target: T?,
    after: Boolean
): Int? {
    val sourceIndex = order.indexOf(source)
    if (sourceIndex < 0 || order.size < 2) return null
    val targetIndex = if (target == null) order.size else order.indexOf(target)
    if (targetIndex < 0 || target == source) return null
    val insertionIndex = if (target == null) order.size else targetIndex + if (after) 1 else 0
    val destinationIndex = (if (sourceIndex < insertionIndex) insertionIndex - 1 else insertionIndex)
        .coerceIn(0, order.lastIndex)
    return (destinationIndex - sourceIndex).takeIf { it != 0 }
}

@Composable
private fun HomeLayoutSelector(
    title: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    val colors = LocalRetroBitColors.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = colors.inkSecondary, fontSize = 6.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(62.dp))
        options.forEach { label ->
            val isSelected = selected == label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) colors.inkPrimary else colors.background)
                    .border(1.dp, colors.border)
                    .clickable { onSelect(label) }
                    .padding(horizontal = 2.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
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
private fun HomeWidgetFrame(
    widgetId: Int,
    savedSize: HomeWidgetSize?,
    widgetHost: AppWidgetHost,
    manager: AppWidgetManager,
    availableWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    fillAvailableWidth: Boolean,
    contentAlignment: Alignment
) {
    val providerInfo = remember(widgetId, manager) { manager.getAppWidgetInfo(widgetId) }
    val minimumHeight = minimumWidgetBodyHeight(providerInfo)
    val defaultSize = remember(providerInfo) { defaultHomeWidgetSize(providerInfo) }
    val rawSize = savedSize ?: defaultSize
    val widgetSize = rawSize.copy(
        widthPercent = rawSize.widthPercent.coerceIn(50, 100),
        heightDp = rawSize.heightDp.coerceAtLeast(minimumHeight).coerceIn(120, 600)
    )
    val minimumWidth = ((providerInfo?.minWidth ?: 0) + 16).dp
    val widgetWidth = (availableWidth * (widgetSize.widthPercent / 100f))
        .coerceAtLeast(minimumWidth)
        .coerceAtMost(availableWidth)
    val widgetContentWidthDp = (widgetWidth.value - 16f).toInt().coerceAtLeast(1)
    val frameModifier = modifier.then(if (fillAvailableWidth) Modifier.fillMaxWidth() else Modifier.width(widgetWidth))

    Box(modifier = frameModifier, contentAlignment = contentAlignment) {
        HomeWidgetCard(
            widgetId = widgetId,
            widgetHost = widgetHost,
            manager = manager,
            providerInfo = providerInfo,
            widgetSize = widgetSize,
            widgetContentWidthDp = widgetContentWidthDp,
            modifier = Modifier.width(widgetWidth)
        )
    }
}

private fun minimumWidgetBodyHeight(providerInfo: AppWidgetProviderInfo?): Int =
    ((providerInfo?.minHeight ?: 0) + 12).coerceIn(120, 480)

private fun defaultHomeWidgetSize(providerInfo: AppWidgetProviderInfo?): HomeWidgetSize =
    HomeWidgetSize(widthPercent = 100, heightDp = minimumWidgetBodyHeight(providerInfo))

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
                .clickable(onClick = onLaunch),
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
    providerInfo: AppWidgetProviderInfo?,
    widgetSize: HomeWidgetSize,
    widgetContentWidthDp: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val label = remember(providerInfo) {
        providerInfo?.loadLabel(context.packageManager)?.toString() ?: "UNAVAILABLE WIDGET"
    }
    val minimumHeight = minimumWidgetBodyHeight(providerInfo)
    val bodyHeightDp = widgetSize.heightDp.coerceAtLeast(minimumHeight).coerceIn(120, 600)
    val contentWidthDp = widgetContentWidthDp.coerceAtLeast(1)

    LaunchedEffect(widgetId, contentWidthDp, bodyHeightDp, providerInfo) {
        if (providerInfo != null) {
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, contentWidthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, contentWidthDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, bodyHeightDp)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, bodyHeightDp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    putParcelableArrayList(
                        AppWidgetManager.OPTION_APPWIDGET_SIZES,
                        arrayListOf(SizeF(contentWidthDp.toFloat(), bodyHeightDp.toFloat()))
                    )
                }
            }
            runCatching { manager.updateAppWidgetOptions(widgetId, options) }
        }
    }

    Column(modifier.background(colors.surface).border(1.dp, colors.border).padding(7.dp)) {
        Text(
            label.uppercase(),
            color = colors.inkSecondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        if (providerInfo == null) {
            Text("Widget provider is unavailable. Remove it in Widget Settings.", color = colors.inkMuted, fontSize = 8.sp)
        } else {
            AndroidView(
                factory = { viewContext -> widgetHost.createView(viewContext, widgetId, providerInfo) },
                modifier = Modifier.fillMaxWidth().height(bodyHeightDp.dp)
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
