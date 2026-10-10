package com.retrobit.launcher.ui.components

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.data.NotificationMonitorService
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun RetroMediaPlayerWidget(modifier: Modifier = Modifier) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var title by remember { mutableStateOf("NO ACTIVE MEDIA SESSION") }
    var artist by remember { mutableStateOf("Enable notification access to connect") }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var hasNotificationAccess by remember(context) {
        mutableStateOf(NotificationMonitorService.isPermissionGranted(context))
    }

    LaunchedEffect(context) {
        while (true) {
            hasNotificationAccess = NotificationMonitorService.isPermissionGranted(context)
            delay(1_000)
        }
    }

    LaunchedEffect(context, hasNotificationAccess) {
        if (!hasNotificationAccess) {
            controller = null
            title = "MEDIA SESSION LOCKED"
            artist = "Tap to enable notification access"
            isPlaying = false
            return@LaunchedEffect
        }

        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val listener = ComponentName(context, NotificationMonitorService::class.java)
        while (true) {
            val activeController = try {
                sessionManager.getActiveSessions(listener)
                    .firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                    ?: sessionManager.getActiveSessions(listener).firstOrNull()
            } catch (_: SecurityException) {
                null
            } catch (_: Exception) {
                null
            }
            controller = activeController
            val metadata = activeController?.metadata
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: "NOTHING PLAYING"
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: ""
            val playback = activeController?.playbackState
            isPlaying = playback?.state == PlaybackState.STATE_PLAYING
            position = playback?.position?.coerceAtLeast(0L) ?: 0L
            duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
            delay(1_000)
        }
    }

    val progress = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val barLength = 18
    val filled = (progress * barLength).roundToInt().coerceIn(0, barLength)
    val progressBar = "[${"█".repeat(filled)}${"░".repeat(barLength - filled)}]"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(1.dp, colors.border)
            .clickable(enabled = !hasNotificationAccess) {
                NotificationMonitorService.openPermissionSettings(context)
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    fontSize = 8.sp,
                    color = colors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MediaControl("|◀", colors.inkPrimary, enabled = controller != null) {
                    controller?.transportControls?.skipToPrevious()
                }
                MediaControl(if (isPlaying) "❚❚" else "▶", colors.inkPrimary, enabled = controller != null) {
                    controller?.transportControls?.let { controls ->
                        if (isPlaying) controls.pause() else controls.play()
                    }
                }
                MediaControl("▶|", colors.inkPrimary, enabled = controller != null) {
                    controller?.transportControls?.skipToNext()
                }
            }
        }

        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatMediaTime(position), fontSize = 8.sp, color = colors.inkSecondary)
            Text(text = progressBar, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = colors.inkPrimary)
            Text(text = formatMediaTime(duration), fontSize = 8.sp, color = colors.inkSecondary)
        }
    }
}

@Composable
private fun MediaControl(label: String, color: androidx.compose.ui.graphics.Color, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalRetroBitColors.current
    Box(
        modifier = Modifier
            .border(1.dp, colors.border)
            .background(colors.background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) color else colors.inkMuted
        )
    }
}

private fun formatMediaTime(millis: Long): String {
    val seconds = (millis.coerceAtLeast(0L) / 1_000L).toInt()
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
