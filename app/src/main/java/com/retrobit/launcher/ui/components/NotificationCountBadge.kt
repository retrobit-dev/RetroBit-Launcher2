package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun NotificationCountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return

    val colors = LocalRetroBitColors.current
    val label = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = if (label.length > 2) 21.dp else 15.dp, minHeight = 15.dp)
            .background(colors.inkPrimary, CircleShape)
            .border(1.dp, colors.background, CircleShape)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = colors.background,
            fontSize = 7.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}
