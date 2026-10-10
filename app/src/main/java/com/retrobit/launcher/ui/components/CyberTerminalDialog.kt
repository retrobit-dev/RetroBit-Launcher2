package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import com.retrobit.launcher.ui.theme.RetroBitThemeMode

@Composable
fun CyberTerminalDialog(
    onDismiss: () -> Unit,
    onTriggerRefresh: () -> Unit,
    onChangeTheme: (RetroBitThemeMode) -> Unit
) {
    val colors = LocalRetroBitColors.current
    var inputCmd by remember { mutableStateOf("") }
    val history = remember {
        mutableStateListOf(
            "> RETROBIT_OS [v2.5.0-pro]",
            "> Type 'help' for available commands."
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .background(colors.background)
                .border(2.dp, colors.border)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ">_ KERNEL CONSOLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
                Text(
                    text = "[EXIT]",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(colors.surface)
                    .border(1.dp, colors.border)
                    .padding(8.dp)
            ) {
                items(history) { line ->
                    Text(
                        text = line,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.inkPrimary,
                        lineHeight = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "> ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.inkPrimary)
                BasicTextField(
                    value = inputCmd,
                    onValueChange = { inputCmd = it },
                    textStyle = TextStyle(
                        color = colors.inkPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(colors.inkPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        val trimmed = inputCmd.trim()
                        if (trimmed.isNotEmpty()) {
                            history.add("> $trimmed")
                            val parts = trimmed.lowercase().split(" ")
                            when (parts[0]) {
                                "help" -> history.add("< CMDS: help, refresh, calc [expr], screentime, theme [mode], clear")
                                "clear" -> {
                                    history.clear()
                                    history.add("> BUFFER CLEARED.")
                                }
                                "refresh" -> {
                                    onTriggerRefresh()
                                    history.add("< E-Paper hardware refresh cycle triggered.")
                                }
                                "screentime" -> history.add("< TOTAL USAGE TODAY: 1h 38m across 14 unlocks.")
                                "calc" -> {
                                    history.add("< Inline calc active in search bar. e.g. '> 45*12'")
                                }
                                "theme" -> {
                                    if (parts.size > 1) {
                                        when (parts[1]) {
                                            "light" -> { onChangeTheme(RetroBitThemeMode.LIGHT_EINK); history.add("< Theme: Paper White") }
                                            "dark" -> { onChangeTheme(RetroBitThemeMode.DARK_EINK); history.add("< Theme: Slate Onyx") }
                                            "pocket" -> { onChangeTheme(RetroBitThemeMode.POCKET_90S); history.add("< Theme: GameBoy Pocket") }
                                            "stark" -> { onChangeTheme(RetroBitThemeMode.STARK_1BIT); history.add("< Theme: Stark 1-Bit") }
                                            else -> history.add("< Unknown theme. Available: light, dark, pocket, stark")
                                        }
                                    } else history.add("< Usage: theme [light | dark | pocket | stark]")
                                }
                                else -> history.add("< COMMAND NOT FOUND: '$trimmed'. Type 'help'.")
                            }
                            inputCmd = ""
                        }
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.surface)
                        .border(1.dp, colors.border)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
        }
    }
}
