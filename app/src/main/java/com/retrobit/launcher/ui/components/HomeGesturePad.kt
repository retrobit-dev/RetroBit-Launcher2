@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlin.math.abs

@Composable
fun HomeGesturePad(
    doubleTapEnabled: Boolean,
    swipeUpLabel: String,
    swipeDownLabel: String,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentLongPress by rememberUpdatedState(onLongPress)
    val currentSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentSwipeDown by rememberUpdatedState(onSwipeDown)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(colors.surface, RoundedCornerShape(2.dp))
            .border(1.dp, colors.border, RoundedCornerShape(2.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var current = down
                    var swipeHandled = false
                    while (current.pressed) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = change.position - down.position
                        if (!swipeHandled && abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) > abs(delta.x)) {
                            swipeHandled = true
                            if (delta.y < 0f) currentSwipeUp() else currentSwipeDown()
                        }
                        current = change
                    }
                }
            }
            .combinedClickable(
                onClick = {},
                onDoubleClick = { if (doubleTapEnabled) currentDoubleTap() },
                onLongClick = { currentLongPress() }
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "GESTURES // 2×: ${if (doubleTapEnabled) "LOCK" else "OFF"}  •  ↑ $swipeUpLabel  •  ↓ $swipeDownLabel  •  HOLD: EDIT",
            color = colors.inkSecondary,
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
