package com.retrobit.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun DeviceAdminPromptDialog(
    onDismiss: () -> Unit,
    onActivate: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(colors.background).border(2.dp, colors.border).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("DOUBLE TAP // LOCK PHONE", color = colors.inkPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Android requires you to activate RetroBit as a Device Admin before an app can lock the phone. The only requested policy is FORCE LOCK; approval happens in Android's system screen.",
                color = colors.inkSecondary,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CANCEL", color = colors.inkSecondary, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
                Text("ACTIVATE", color = colors.inkPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onActivate).padding(8.dp))
            }
        }
    }
}
