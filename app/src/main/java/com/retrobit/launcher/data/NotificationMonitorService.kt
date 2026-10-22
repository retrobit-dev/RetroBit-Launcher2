package com.retrobit.launcher.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PackageNotificationSummary(
    val count: Int,
    val preview: String?
)

/** Tracks active notifications for app-icon badges and short drawer previews. */
class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private val _packageNotificationSummaries = MutableStateFlow<Map<String, PackageNotificationSummary>>(emptyMap())
        val packageNotificationSummaries: StateFlow<Map<String, PackageNotificationSummary>> =
            _packageNotificationSummaries.asStateFlow()

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            return enabledListeners.split(':').any { flattenedComponent ->
                ComponentName.unflattenFromString(flattenedComponent)?.packageName == context.packageName
            }
        }

        fun openPermissionSettings(context: Context) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private data class ActiveNotificationRecord(
        val packageName: String,
        val postTime: Long,
        val preview: String?,
        val badgeCount: Int
    )

    private val notificationLock = Any()
    private val notificationsByKey = mutableMapOf<String, ActiveNotificationRecord>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return
        synchronized(notificationLock) {
            notificationsByKey[sbn.key] = sbn.toRecord()
            publishSummariesLocked()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn ?: return
        synchronized(notificationLock) {
            notificationsByKey.remove(sbn.key)
            publishSummariesLocked()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val snapshot = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        synchronized(notificationLock) {
            notificationsByKey.clear()
            snapshot.forEach { notificationsByKey[it.key] = it.toRecord() }
            publishSummariesLocked()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        synchronized(notificationLock) {
            notificationsByKey.clear()
            publishSummariesLocked()
        }
    }

    private fun StatusBarNotification.toRecord(): ActiveNotificationRecord {
        val postedNotification = this.notification
        val extras = postedNotification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val preview = text.takeIf { it.isNotBlank() }?.let {
            if (title.isNotBlank()) "$title: \"$it\"" else "\"$it\""
        }
        val badgeSuppressionFlags =
            Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE or Notification.FLAG_GROUP_SUMMARY
        val countsTowardBadge = (postedNotification.flags and badgeSuppressionFlags) == 0
        val badgeCount = if (countsTowardBadge) postedNotification.number.takeIf { it > 0 } ?: 1 else 0

        return ActiveNotificationRecord(
            packageName = this.packageName,
            postTime = this.postTime,
            preview = preview,
            badgeCount = badgeCount
        )
    }

    /** Must be called with [notificationLock] held. */
    private fun publishSummariesLocked() {
        val summaries = notificationsByKey.values
            .groupBy { it.packageName }
            .mapValues { (_, notifications) ->
                val preview = notifications
                    .asSequence()
                    .filter { !it.preview.isNullOrBlank() }
                    .maxByOrNull { it.postTime }
                    ?.preview
                PackageNotificationSummary(
                    count = notifications.sumOf { it.badgeCount },
                    preview = preview
                )
            }
            .filterValues { it.count > 0 || !it.preview.isNullOrBlank() }
        _packageNotificationSummaries.value = summaries
    }
}
