package com.retrobit.launcher.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.delay

@Composable
fun QuickControlsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalRetroBitColors.current
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val camera = remember(context) { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    var volume by remember(context) { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    val maxVolume = remember(context) { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1).toFloat() }
    var brightness by remember(context) {
        mutableFloatStateOf(runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        }.getOrDefault(0.5f).coerceIn(0f, 1f))
    }
    var canWriteSettings by remember(context) { mutableStateOf(Settings.System.canWrite(context)) }
    var torchEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        while (true) {
            canWriteSettings = Settings.System.canWrite(context)
            if (canWriteSettings) {
                brightness = runCatching {
                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                }.getOrDefault(brightness).coerceIn(0.05f, 1f)
            }
            delay(750)
        }
    }

    fun toggleTorch() {
        try {
            val cameraId = camera.cameraIdList.firstOrNull { id ->
                camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchEnabled = !torchEnabled
            camera.setTorchMode(cameraId, torchEnabled)
        } catch (_: Exception) {
            torchEnabled = false
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) toggleTorch()
    }

    fun openSettings(action: String) {
        try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            // OEM settings may not expose every panel action.
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .verticalScroll(rememberScrollState())
                .background(colors.background)
                .border(2.dp, colors.border)
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("QUICK TILES // SYSTEM CONTROLS", color = colors.inkPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("[X]", color = colors.inkPrimary, modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SystemTile("WI-FI", colors.inkPrimary, Modifier.weight(1f)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) openSettings(Settings.Panel.ACTION_WIFI)
                    else openSettings(Settings.ACTION_WIFI_SETTINGS)
                }
                SystemTile("BLUETOOTH", colors.inkPrimary, Modifier.weight(1f)) { openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SystemTile("AIRPLANE", colors.inkPrimary, Modifier.weight(1f)) { openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS) }
                SystemTile(if (torchEnabled) "FLASH: ON" else "FLASHLIGHT", colors.inkPrimary, Modifier.weight(1f)) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        toggleTorch()
                    } else {
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("MEDIA VOLUME  ${volume.toInt()}/${maxVolume.toInt()}", color = colors.inkSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = volume.coerceIn(0f, maxVolume),
                onValueChange = {
                    volume = it
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
                },
                valueRange = 0f..maxVolume
            )
            Spacer(Modifier.height(2.dp))
            Text("SCREEN BRIGHTNESS", color = colors.inkSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = brightness,
                onValueChange = { value ->
                    brightness = value
                    if (canWriteSettings) {
                        runCatching {
                            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (value * 255).toInt())
                        }
                    }
                },
                enabled = canWriteSettings,
                valueRange = 0.05f..1f
            )
            if (!canWriteSettings) {
                Text(
                    "Grant special access to change system brightness.",
                    color = colors.inkMuted,
                    fontSize = 9.sp
                )
                Spacer(Modifier.height(4.dp))
                SystemTile("GRANT BRIGHTNESS ACCESS", colors.inkPrimary) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        canWriteSettings = Settings.System.canWrite(context)
                    } catch (_: Exception) { }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Android requires Wi-Fi, Bluetooth, and airplane mode changes to be confirmed in system settings.",
                color = colors.inkMuted,
                fontSize = 8.sp
            )
        }
    }
}

@Composable
private fun SystemTile(
    label: String,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    Box(
        modifier
            .background(colors.surface)
            .border(1.dp, colors.border)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
    }
}
