package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun LaunchRestrictionDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(colors.background).border(2.dp, colors.border).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = colors.inkPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = colors.inkSecondary, fontSize = 9.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CLOSE", color = colors.inkSecondary, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
                Text("SETTINGS", color = colors.inkPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOpenSettings).padding(8.dp))
            }
        }
    }
}
