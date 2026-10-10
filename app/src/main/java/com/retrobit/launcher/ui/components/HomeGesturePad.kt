package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun HomeGesturePad(
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(colors.surface, RoundedCornerShape(2.dp))
            .border(1.dp, colors.border, RoundedCornerShape(2.dp))
            .pointerInput(onDoubleTap, onLongPress) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .pointerInput(onSwipeUp, onSwipeDown) {
                var dragDistance = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onVerticalDrag = { change, amount ->
                        dragDistance += amount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            dragDistance < -48f -> onSwipeUp()
                            dragDistance > 48f -> onSwipeDown()
                        }
                        dragDistance = 0f
                    },
                    onDragCancel = { dragDistance = 0f }
                )
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "GESTURE PAD // DOUBLE TAP: LOCK  •  SWIPE ↑: APPS  •  SWIPE ↓: TILES  •  HOLD: EDIT",
            color = colors.inkSecondary,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
