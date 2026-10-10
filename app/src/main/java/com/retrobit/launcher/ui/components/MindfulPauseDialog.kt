package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.delay

/**
 * Dialog Jeda Sadar (Mindful Pause / Digital Detox Delay).
 * Mencegah pembukaan aplikasi yang mencandu secara refleks tanpa sadar.
 */
@Composable
fun MindfulPauseDialog(
    app: AppModel,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    var secondsLeft by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .border(2.dp, colors.border)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚡ ICE BREAKER // MINDFUL DELAY",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.inkPrimary,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Tarik napas sejenak.\nApakah Anda benar-benar perlu membuka '${app.displayLabel}' sekarang?",
                fontSize = 10.sp,
                color = colors.inkSecondary,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(14.dp))

            // Countdown box
            Box(
                modifier = Modifier
                    .border(1.dp, colors.border)
                    .background(colors.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (secondsLeft > 0) "STANDBY: ${secondsLeft}s..." else "UNLOCK READY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, colors.border)
                        .background(colors.surface)
                        .clickable { onCancel() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "KEMBALI (BATAL)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, if (secondsLeft == 0) colors.border else colors.inkMuted)
                        .background(if (secondsLeft == 0) colors.inkPrimary else colors.surface)
                        .clickable(enabled = secondsLeft == 0) { onProceed() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BUKA APLIKASI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (secondsLeft == 0) colors.background else colors.inkMuted
                    )
                }
            }
        }
    }
}
