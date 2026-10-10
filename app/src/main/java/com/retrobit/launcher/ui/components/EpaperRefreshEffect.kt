package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
fun EpaperRefreshEffect(
    isRefreshing: Boolean,
    onRefreshFinished: () -> Unit
) {
    if (!isRefreshing) return

    var currentFlashColor by remember { mutableStateOf(Color.Black) }

    LaunchedEffect(isRefreshing) {
        currentFlashColor = Color.Black
        delay(60)
        currentFlashColor = Color.White
        delay(90)
        currentFlashColor = Color.Black
        delay(70)
        currentFlashColor = Color.White
        delay(60)
        onRefreshFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentFlashColor)
    )
}
