package com.retrobit.launcher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.delay

@Composable
fun RightEdgeAppDrawerHandle(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val currentOnOpen by rememberUpdatedState(onOpen)
    var expanded by remember { mutableStateOf(true) }
    val width by animateDpAsState(if (expanded) 34.dp else 6.dp, label = "drawer-handle-width")
    val height by animateDpAsState(if (expanded) 88.dp else 42.dp, label = "drawer-handle-height")

    LaunchedEffect(Unit) {
        delay(2_500)
        expanded = false
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(colors.surface, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
            .border(1.dp, colors.border, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
            .pointerInput(Unit) {
                var dragDistance = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onHorizontalDrag = { change, amount ->
                        dragDistance += amount
                        change.consume()
                    },
                    onDragEnd = {
                        if (dragDistance < 0f) currentOnOpen()
                        dragDistance = 0f
                    },
                    onDragCancel = { dragDistance = 0f }
                )
            }
            .clickable { currentOnOpen() }
            .semantics { contentDescription = "Open app drawer by tapping or swiping left from the right edge" }
            .padding(horizontal = if (expanded) 4.dp else 1.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (expanded) {
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("›", color = colors.inkPrimary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("A–Z", color = colors.inkSecondary, fontSize = 6.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Box(Modifier.width(2.dp).fillMaxHeight().background(colors.inkMuted))
        }
    }
}
