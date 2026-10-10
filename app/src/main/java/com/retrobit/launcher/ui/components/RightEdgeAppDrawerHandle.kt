package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun RightEdgeAppDrawerHandle(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val currentOnOpen by rememberUpdatedState(onOpen)
    Column(
        modifier = modifier
            .width(34.dp)
            .height(88.dp)
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
            .semantics { contentDescription = "Open app drawer by tapping or swiping left" }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("›", color = colors.inkPrimary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("A–Z", color = colors.inkSecondary, fontSize = 6.sp, fontWeight = FontWeight.Bold)
    }
}
