package com.retrobit.launcher.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun AlphabetScrubber(
    availableLetters: Set<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val view = LocalView.current
    val alphabet = remember { listOf("★") + ('A'..'Z').map { it.toString() } }
    
    var activeLetter by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(24.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val index = ((offset.y / size.height) * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                            val letter = alphabet[index]
                            activeLetter = letter
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onLetterSelected(letter)
                        },
                        onVerticalDrag = { change, _ ->
                            val index = ((change.position.y / size.height) * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                            val letter = alphabet[index]
                            if (letter != activeLetter) {
                                activeLetter = letter
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onLetterSelected(letter)
                            }
                        },
                        onDragEnd = { activeLetter = null },
                        onDragCancel = { activeLetter = null }
                    )
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { char ->
                val isAvailable = availableLetters.contains(char) || char == "★"
                val isActive = char == activeLetter

                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .then(
                            if (isActive) Modifier.background(colors.inkPrimary, RoundedCornerShape(2.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        fontSize = 8.sp,
                        fontWeight = if (isAvailable || isActive) FontWeight.ExtraBold else FontWeight.Normal,
                        color = when {
                            isActive -> colors.background
                            isAvailable -> colors.inkPrimary
                            else -> colors.inkMuted
                        }
                    )
                }
            }
        }

        activeLetter?.let { letter ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-32).dp)
                    .shadow(4.dp)
                    .background(colors.background)
                    .border(2.dp, colors.border)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "[ $letter ]",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.inkPrimary
                    )
                    Text(
                        text = if (letter == "★") "FAV" else "SECTION",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkSecondary
                    )
                }
            }
        }
    }
}
