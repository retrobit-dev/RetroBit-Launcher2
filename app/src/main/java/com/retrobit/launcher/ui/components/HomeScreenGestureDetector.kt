package com.retrobit.launcher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Observes launcher gestures across the Home and Widget pages without consuming pointer changes.
 * Child controls and horizontal widget carousels keep their own scroll/tap gestures.
 */
@Composable
fun Modifier.homeScreenGestureDetector(
    enabled: Boolean,
    doubleTapEnabled: Boolean,
    swipeThresholdDp: Float,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
): Modifier {
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)

    return pointerInput(enabled, doubleTapEnabled, swipeThresholdDp) {
        if (!enabled) return@pointerInput

        var lastTapTime = 0L
        var lastTapX = 0f
        var lastTapY = 0f
        val swipeDistance = swipeThresholdDp.dp.toPx()
        val doubleTapDistance = viewConfiguration.touchSlop * 2f

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var childConsumedTap = awaitPointerEvent(PointerEventPass.Final)
                .changes.any { it.id == down.id && it.isConsumed }
            var childConsumedHorizontalDrag = false
            var current = down
            var moved = false
            var multiTouch = false
            var longPressTriggered = false
            var maxDistance = 0f

            while (current.pressed) {
                val remainingLongPress = viewConfiguration.longPressTimeoutMillis -
                    (current.uptimeMillis - down.uptimeMillis)
                val event = if (!moved && !longPressTriggered && remainingLongPress > 0L) {
                    withTimeoutOrNull(remainingLongPress) { awaitPointerEvent(PointerEventPass.Initial) }
                } else {
                    awaitPointerEvent(PointerEventPass.Initial)
                }
                if (event == null) {
                    if (!moved && !multiTouch && !childConsumedTap) {
                        lastTapTime = 0L
                        currentOnLongPress()
                        longPressTriggered = true
                    }
                    continue
                }
                if (event.changes.count { it.pressed } > 1) multiTouch = true
                if (event.changes.firstOrNull { it.id == down.id } == null) break
                val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                val change = finalEvent.changes.firstOrNull { it.id == down.id } ?: break
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                maxDistance = maxOf(maxDistance, abs(dx), abs(dy))
                if (maxDistance > viewConfiguration.touchSlop) moved = true
                if (change.isConsumed) {
                    if (maxDistance > viewConfiguration.touchSlop && abs(dx) > abs(dy) * 1.2f) {
                        childConsumedHorizontalDrag = true
                    } else if (maxDistance <= viewConfiguration.touchSlop) {
                        childConsumedTap = true
                    }
                }
                current = change
            }

            val dx = current.position.x - down.position.x
            val dy = current.position.y - down.position.y
            val duration = (current.uptimeMillis - down.uptimeMillis).coerceAtLeast(0L)

            if (multiTouch) {
                lastTapTime = 0L
            } else if (!moved && childConsumedTap) {
                lastTapTime = 0L
            } else if (!moved) {
                when {
                    duration >= viewConfiguration.longPressTimeoutMillis -> {
                        lastTapTime = 0L
                        if (!longPressTriggered) currentOnLongPress()
                    }
                    doubleTapEnabled &&
                        lastTapTime > 0L &&
                        current.uptimeMillis - lastTapTime <= viewConfiguration.doubleTapTimeoutMillis &&
                        abs(current.position.x - lastTapX) <= doubleTapDistance &&
                        abs(current.position.y - lastTapY) <= doubleTapDistance -> {
                        lastTapTime = 0L
                        currentOnDoubleTap()
                    }
                    else -> {
                        lastTapTime = current.uptimeMillis
                        lastTapX = current.position.x
                        lastTapY = current.position.y
                    }
                }
            } else {
                lastTapTime = 0L
                val isQuickHorizontalSwipe =
                    maxDistance >= swipeDistance &&
                        abs(dx) >= swipeDistance &&
                        abs(dx) > abs(dy) * 1.2f &&
                        duration in 1L..700L
                val isQuickVerticalSwipe =
                    maxDistance >= swipeDistance &&
                        abs(dy) >= swipeDistance &&
                        abs(dy) > abs(dx) * 1.2f &&
                        duration in 1L..700L
                if (!longPressTriggered) {
                    when {
                        isQuickHorizontalSwipe && !childConsumedHorizontalDrag -> {
                            if (dx > 0f) currentOnSwipeRight() else currentOnSwipeLeft()
                        }
                        isQuickVerticalSwipe -> {
                            if (dy < 0f) currentOnSwipeUp() else currentOnSwipeDown()
                        }
                    }
                }
            }
        }
    }
}
