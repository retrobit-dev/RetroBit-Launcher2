package com.retrobit.launcher.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.delay

@Composable
fun MatrixRainDialog(onDismiss: () -> Unit) {
    val colors = LocalRetroBitColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            MatrixRainCanvas(Modifier.fillMaxSize())
            Text(
                text = "[ EXIT MATRIX ]",
                color = Color(0xFF72FF8A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Text(
                text = "RETROBIT // EASTER EGG",
                color = colors.inkMuted,
                fontSize = 8.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun MatrixRainCanvas(modifier: Modifier = Modifier) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(55)
            frame++
        }
    }

    Canvas(modifier.background(Color.Black)) {
        val currentFrame = frame
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val cellWidth = 16f
        val cellHeight = 18f
        val columns = (size.width / cellWidth).toInt().coerceAtLeast(1)
        val rows = (size.height / cellHeight).toInt().coerceAtLeast(1)
        val chars = "アカサタナハマヤラワ012345789ZXCVBNM#$%"
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textSize = 14f
            color = android.graphics.Color.rgb(50, 255, 92)
        }
        nativeCanvas.drawColor(android.graphics.Color.BLACK)
        repeat(columns) { column ->
            val speed = 1 + column % 4
            val head = (currentFrame * speed + column * 31) % (rows + 24)
            repeat(22) { tail ->
                val row = head - tail
                if (row in 0 until rows) {
                    paint.alpha = (255 * (1f - tail / 23f)).toInt().coerceIn(0, 255)
                    val charIndex = (currentFrame * (column % 5 + 1) + column * 13 + tail * 7) % chars.length
                    nativeCanvas.drawText(
                        chars[charIndex].toString(),
                        column * cellWidth,
                        row * cellHeight,
                        paint
                    )
                }
            }
        }
    }
}
