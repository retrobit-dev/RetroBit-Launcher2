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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun RetroMediaPlayerWidget(
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    var isPlaying by remember { mutableStateOf(true) }
    var trackIndex by remember { mutableIntStateOf(0) }

    val trackList = remember {
        listOf(
            "Master Boot Record - C:\\> CHKDSK /F",
            "Carpenter Brut - Turbo Killer (8-Bit)",
            "Perturbator - Venger (Lo-Fi)",
            "Keygen Church - Tenebre Rosso Sangue"
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(1.dp, colors.border)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isPlaying) "▰▱ " else "▱▱ ",
                    fontSize = 11.sp,
                    color = colors.inkPrimary
                )
                Text(
                    text = trackList[trackIndex],
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .border(1.dp, colors.border)
                        .background(colors.background)
                        .clickable {
                            trackIndex = (trackIndex - 1 + trackList.size) % trackList.size
                        }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = "|◀", fontSize = 8.5.sp, color = colors.inkPrimary)
                }

                Box(
                    modifier = Modifier
                        .border(1.dp, colors.border)
                        .background(colors.background)
                        .clickable { isPlaying = !isPlaying }
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (isPlaying) "❚❚" else "▶",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .border(1.dp, colors.border)
                        .background(colors.background)
                        .clickable {
                            trackIndex = (trackIndex + 1) % trackList.size
                        }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = "▶|", fontSize = 8.5.sp, color = colors.inkPrimary)
                }
            }
        }

        Spacer(Modifier.height(3.dp))

        // ASCII Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "02:14", fontSize = 8.sp, color = colors.inkSecondary)
            Text(
                text = "[████████░░░░░░░░░░]",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = colors.inkPrimary
            )
            Text(text = "03:45", fontSize = 8.sp, color = colors.inkSecondary)
        }
    }
}
