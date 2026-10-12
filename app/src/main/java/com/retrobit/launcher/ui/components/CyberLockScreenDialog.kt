package com.retrobit.launcher.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import java.util.concurrent.atomic.AtomicReference

@Composable
fun CyberLockScreenDialog(store: LauncherFeatureStore, onDismiss: () -> Unit) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val pin = store.lockscreenPin
    val isLocked = store.lockscreenEnabled && !pin.isNullOrBlank()
    val lockscreenWidgets = remember(store) { store.getLockscreenWidgetModules() }
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
    val biometricStatus = remember(context) {
        runCatching { BiometricManager.from(context).canAuthenticate(authenticators) }
            .getOrDefault(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
    }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var biometricError by remember { mutableStateOf("") }
    var showPinFallback by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Date()) }
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    val currentBiometricErrorHandler = rememberUpdatedState<(String) -> Unit> { message -> biometricError = message }
    val currentPinFallbackHandler = rememberUpdatedState<() -> Unit> { showPinFallback = true }
    val activeBiometricPrompt = remember { AtomicReference<BiometricPrompt?>(null) }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock RetroBit")
            .setSubtitle("Use fingerprint or face unlock")
            .setNegativeButtonText("CANCEL")
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
                        currentBiometricErrorHandler.value(
                            "Biometric not recognized. Enter your PIN below, or try again."
                        )
                        currentPinFallbackHandler.value.invoke()
                        activeBiometricPrompt.get()?.cancelAuthentication()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_CANCELED
                        ) {
                            currentBiometricErrorHandler.value(errString.toString())
                            currentPinFallbackHandler.value.invoke()
                        }
                    }
                }
            )
        }
    }
    SideEffect { activeBiometricPrompt.set(biometricPrompt) }
    val biometricAvailable = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS && biometricPrompt != null

    LaunchedEffect(biometricPrompt, biometricStatus, pin) {
        val prompt = biometricPrompt
        if (biometricStatus == BiometricManager.BIOMETRIC_SUCCESS && prompt != null && !pin.isNullOrBlank()) {
            biometricError = ""
            runCatching { prompt.authenticate(promptInfo) }
                .onFailure {
                    biometricError = it.message ?: "Could not start Android biometric authentication."
                    showPinFallback = true
                }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            kotlinx.coroutines.delay(1_000)
        }
    }

    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(now).uppercase()
    val unlockMode = if (biometricAvailable) "FINGERPRINT / FACE UNLOCK" else "PIN UNLOCK"

    Dialog(
        onDismissRequest = { if (!isLocked) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isLocked,
            dismissOnClickOutside = !isLocked
        )
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(colors.background)
                .border(3.dp, colors.border)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text("RETROBIT // LOCKSCREEN", color = colors.inkPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                if (lockscreenWidgets.isEmpty()) unlockMode else "$unlockMode · WIDGETS BELOW",
                color = colors.inkSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 3.dp)
            )

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(time, color = colors.inkPrimary, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
                Text(date, color = colors.inkSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))

                if (pin.isNullOrBlank()) {
                    Text("NO LOCAL PIN CONFIGURED", color = colors.inkPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Set a PIN in Settings → Profile first.", color = colors.inkSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                } else {
                    if (biometricAvailable) {
                        Text(
                            "OPEN / RETRY BIOMETRIC UNLOCK",
                            color = colors.background,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .background(colors.inkPrimary)
                                .clickable {
                                    biometricError = ""
                                    error = ""
                                    showPinFallback = false
                                    val prompt = biometricPrompt
                                    if (prompt != null) {
                                        runCatching { prompt.authenticate(promptInfo) }
                                            .onFailure {
                                                biometricError = it.message ?: "Could not start Android biometric authentication."
                                                showPinFallback = true
                                            }
                                    } else {
                                        biometricError = "Android biometric authentication is unavailable."
                                        showPinFallback = true
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 15.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (showPinFallback) "Biometric did not match. Enter your PIN below or retry."
                            else "The Android biometric prompt opens automatically. PIN is only shown after a failed attempt.",
                            color = colors.inkMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            if (biometricStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                                "No fingerprint or face is enrolled. Enter your PIN or enroll biometrics in Android Settings."
                            } else {
                                "Biometric unlock is unavailable. Enter your PIN."
                            },
                            color = colors.inkSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (biometricError.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(biometricError, color = colors.inkSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }

                    if (!biometricAvailable || showPinFallback) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (biometricAvailable) "PIN FALLBACK" else "ENTER YOUR PIN",
                            color = colors.inkPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = entered,
                            onValueChange = {
                                if (it.length <= 8 && it.all { char -> char.isDigit() }) entered = it
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            textStyle = TextStyle(color = colors.inkPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            cursorBrush = SolidColor(colors.inkPrimary),
                            modifier = Modifier.widthIn(min = 200.dp).background(colors.surface).border(1.dp, colors.border).padding(horizontal = 16.dp, vertical = 14.dp),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (entered.isEmpty()) Text("Enter PIN", color = colors.inkMuted, fontSize = 16.sp)
                                    inner()
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        if (error.isNotBlank()) {
                            Text(error, color = colors.inkSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            "UNLOCK",
                            color = colors.background,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .background(colors.inkPrimary)
                                .clickable {
                                    if (store.verifyLockscreenPin(entered)) onDismiss() else error = "Incorrect PIN. Try again."
                                }
                                .padding(vertical = 14.dp)
                        )
                    }
                }

                if (lockscreenWidgets.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    LockscreenWidgetsSection(store = store, modules = lockscreenWidgets)
                }
            }

            Spacer(Modifier.height(10.dp))
            if (lockscreenWidgets.isNotEmpty()) {
                Text(
                    "WIDGETS ARE READ-ONLY WHILE LOCKED",
                    color = colors.inkMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                "IN-APP OVERLAY ONLY · DOES NOT REPLACE ANDROID KEYGUARD",
                color = colors.inkMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (!isLocked) {
                Text(
                    "CLOSE PREVIEW",
                    color = colors.inkPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(12.dp)
                )
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
