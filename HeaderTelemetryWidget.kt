package com.retrobit.launcher.ui.components

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HeaderTelemetryWidget(
    onTriggerRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current

    var currentTime by remember { mutableStateOf("") }
    var currentSeconds by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var batteryPct by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var ramStats by remember { mutableStateOf("0.0/0.0 GB") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val secFormat = SimpleDateFormat("ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentSeconds = secFormat.format(now)
            currentDate = dateFormat.format(now).uppercase()

            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val used = (memInfo.totalMem - memInfo.availMem) / (1024.0 * 1024.0 * 1024.0)
                val total = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
                ramStats = String.format(Locale.US, "%.1f/%.1fGB", used, total)
            }
            delay(1000)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryPct = (level * 100) / scale
                    }
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    val filledBars = (batteryPct / 10).coerceIn(0, 10)
    val asciiBattery = buildString {
        append("[")
        repeat(filledBars) { append("█") }
        repeat(10 - filledBars) { append("░") }
        append("] ")
        append("$batteryPct%")
        if (isCharging) append("⚡")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTriggerRefresh() }
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        // Status Bar Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[NET: 5G-MESH // SYS: OK]",
                fontSize = 9.sp,
                color = colors.inkSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = asciiBattery,
                fontSize = 9.sp,
                color = colors.inkPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))

        // Large Digital Clock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = currentTime,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.inkPrimary,
                    lineHeight = 40.sp
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = currentSeconds,
                    fontSize = 13.sp,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .border(1.dp, colors.border)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "RETROBIT // 1-BIT",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // Date Display
        Text(
            text = "$currentDate // RAM: $ramStats",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.inkSecondary
        )

        // Digital Wellbeing / Detox Telemetry Row (Top Launcher Feature)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DETOX: 1h 38m TODAY",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.inkPrimary
            )
            Text(
                text = "UNLOCKS: 14",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.inkSecondary
            )
            Text(
                text = "AGENDA: 16:00 SYNC",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.inkPrimary
            )
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .border(0.5.dp, colors.inkMuted)
        )
    }
}
