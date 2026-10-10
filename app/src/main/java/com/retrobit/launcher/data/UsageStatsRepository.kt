package com.retrobit.launcher.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import java.util.Calendar

/** Reads genuine foreground-time totals. Access is user-granted in Android Settings. */
class UsageStatsRepository(private val context: Context) {
    private val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun hasUsageAccess(): Boolean = try {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun todayUsageByPackage(): Map<String, Long> {
        if (!hasUsageAccess()) return emptyMap()

        val now = Calendar.getInstance()
        val end = now.timeInMillis
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        val start = now.timeInMillis

        return try {
            usageStats.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
                .orEmpty()
                .groupBy { it.packageName }
                .mapValues { (_, entries) -> entries.sumOf { it.totalTimeInForeground.coerceAtLeast(0L) } }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun totalUsageTodayMillis(): Long = todayUsageByPackage().values.sum()

    fun formatDuration(millis: Long): String {
        val totalMinutes = (millis.coerceAtLeast(0L) / 60_000L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
