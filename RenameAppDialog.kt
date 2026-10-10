package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun RenameAppDialog(
    app: AppModel,
    onDismiss: () -> Unit,
    onSaveRename: (String) -> Unit
) {
    val colors = LocalRetroBitColors.current
    var inputName by remember { mutableStateOf(app.displayLabel) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    text = "✎ RENAME APPLICATION",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary
                )
                Text(
                    text = "[ESC]",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Enter custom display alias for '${app.originalLabel}':",
                fontSize = 8.5.sp,
                color = colors.inkSecondary
            )

            Spacer(Modifier.height(6.dp))

            BasicTextField(
                value = inputName,
                onValueChange = { inputName = it },
                textStyle = TextStyle(
                    color = colors.inkPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(colors.inkPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .border(1.dp, colors.border)
                    .padding(8.dp)
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.inkPrimary)
                    .clickable { onSaveRename(inputName) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SAVE ALIAS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.background
                )
            }
        }
    }
}
