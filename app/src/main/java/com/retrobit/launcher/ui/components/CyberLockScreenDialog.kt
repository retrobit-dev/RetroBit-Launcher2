package com.retrobit.launcher.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CyberLockScreenDialog(store: LauncherFeatureStore, onDismiss: () -> Unit) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val pin = store.lockscreenPin
    val isLocked = store.lockscreenEnabled && !pin.isNullOrBlank()
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
    val biometricStatus = remember(context) {
        runCatching { BiometricManager.from(context).canAuthenticate(authenticators) }
            .getOrDefault(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
    }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var biometricError by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(Date()) }
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    val currentBiometricErrorHandler = rememberUpdatedState<(String) -> Unit> { message -> biometricError = message }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock RetroBit")
            .setSubtitle("Use fingerprint or face unlock")
            .setNegativeButtonText("USE PIN")
            .setAllowedAuthenticators(authenticators)
            .build()
    }
    val biometricPrompt = remember(activity) {
        activity?.let { host ->
            BiometricPrompt(
                host,
                ContextCompat.getMainExecutor(host),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        currentBiometricErrorHandler.value("")
                        currentOnDismiss.value.invoke()
                    }

                    override fun onAuthenticationFailed() {
                        currentBiometricErrorHandler.value("Biometric not recognized. Try again or enter your PIN.")
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_CANCELED
                        ) {
                            currentBiometricErrorHandler.value(errString.toString())
                        }
                    }
                }
            )
        }
    }
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
                if (biometricStatus == BiometricManager.BIOMETRIC_SUCCESS && biometricPrompt != null) {
                    Text(
                        "UNLOCK WITH FINGERPRINT / FACE",
                        color = colors.background,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(colors.inkPrimary).clickable {
                            biometricError = ""
                            runCatching { biometricPrompt.authenticate(promptInfo) }
                                .onFailure { biometricError = it.message ?: "Could not start Android biometric authentication." }
                        }.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                } else if (biometricStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                    Text(
                        "ENROLL FINGERPRINT OR FACE IN ANDROID SETTINGS TO ENABLE BIOMETRICS.",
                        color = colors.inkMuted,
                        fontSize = 7.sp
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    Text("ANDROID BIOMETRICS UNAVAILABLE // USE PIN", color = colors.inkMuted, fontSize = 7.sp)
                    Spacer(Modifier.height(8.dp))
                }
                if (biometricError.isNotBlank()) {
                    Text(biometricError, color = colors.inkSecondary, fontSize = 8.sp)
                    Spacer(Modifier.height(6.dp))
                }
                Text("OR ENTER DECK PIN", color = colors.inkPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current != null) {
        if (current is FragmentActivity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
