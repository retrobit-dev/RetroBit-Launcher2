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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CyberLockScreenDialog(store: LauncherFeatureStore, onDismiss: () -> Unit) {
    val colors = LocalRetroBitColors.current
    val pin = store.lockscreenPin
    val isLocked = store.lockscreenEnabled && !pin.isNullOrBlank()
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            kotlinx.coroutines.delay(1_000)
        }
    }
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(now).uppercase()

    Dialog(
        onDismissRequest = { if (!isLocked) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isLocked,
            dismissOnClickOutside = !isLocked
        )
    ) {
        Column(
            Modifier.fillMaxSize().background(colors.background).border(3.dp, colors.border).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("RETROBIT // SECURE DECK", color = colors.inkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Text(time, color = colors.inkPrimary, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
            Text(date, color = colors.inkSecondary, fontSize = 10.sp)
            Spacer(Modifier.height(28.dp))
            if (pin.isNullOrBlank()) {
                Text("NO LOCAL PIN CONFIGURED", color = colors.inkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Set a PIN in Settings → Profile first.", color = colors.inkSecondary, fontSize = 9.sp)
            } else {
                Text("ENTER DECK PIN", color = colors.inkPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = entered,
                    onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) entered = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(color = colors.inkPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(colors.inkPrimary),
                    modifier = Modifier.width(150.dp).background(colors.surface).border(1.dp, colors.border).padding(12.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (entered.isEmpty()) Text("••••", color = colors.inkMuted, fontSize = 18.sp)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                if (error.isNotBlank()) Text(error, color = colors.inkSecondary, fontSize = 9.sp)
                Text(
                    "UNLOCK",
                    color = colors.background,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(colors.inkPrimary).clickable {
                        if (store.verifyLockscreenPin(entered)) onDismiss() else error = "Incorrect PIN."
                    }.padding(horizontal = 22.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("IN-APP OVERLAY ONLY // ANDROID KEYGUARD IS NOT REPLACED", color = colors.inkMuted, fontSize = 7.sp)
            if (!isLocked) {
                Text("CLOSE PREVIEW", color = colors.inkSecondary, fontSize = 8.sp, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
            }
        }
    }
}
