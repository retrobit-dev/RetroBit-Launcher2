package com.retrobit.launcher.data

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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

        private val whatsappPackages = setOf("com.whatsapp", "com.whatsapp.w4b")
        private val whatsappMessageCountPattern = Regex(
            """^\s*(?:whatsapp(?:\s+business)?(?:\s*[:·•–—-]\s*|\s+))?(\d{1,5})\s*(?:new\s+)?(?:messages?|msgs?|pesan)\b""",
            RegexOption.IGNORE_CASE
        )

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
        val badgeCount: Int,
        val isGroupSummary: Boolean
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
        val flags = postedNotification.flags
        val isGroupSummary = (flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isOngoing = (flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0
        val isWhatsApp = this.packageName in whatsappPackages
        val messagingMessageCount = extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.size ?: 0
        val summaryCount = if (isWhatsApp) {
            whatsappSummaryCount(extras, isGroupSummary, allowMessageText = messagingMessageCount == 0)
        } else null
        val appProvidedCount = postedNotification.number.takeIf { it > 0 }
        val whatsappEstimate = summaryCount
            ?: maxOf(appProvidedCount ?: 0, messagingMessageCount).takeIf { it > 0 }
        val badgeCount = when {
            isOngoing -> 0
            isWhatsApp -> whatsappEstimate ?: if (isGroupSummary) 0 else 1
            isGroupSummary -> 0
            else -> appProvidedCount ?: 1
        }

        return ActiveNotificationRecord(
            packageName = this.packageName,
            postTime = this.postTime,
            preview = preview,
            badgeCount = badgeCount,
            isGroupSummary = isGroupSummary
        )
    }

    private fun whatsappSummaryCount(
        extras: Bundle,
        isGroupSummary: Boolean,
        allowMessageText: Boolean
    ): Int? {
        val candidates = mutableListOf<String>()
        listOf(
            Notification.EXTRA_SUMMARY_TEXT,
            Notification.EXTRA_TITLE,
            Notification.EXTRA_SUB_TEXT
        ).forEach { key ->
            extras.getCharSequence(key)?.toString()?.let(candidates::add)
        }
        if (isGroupSummary) {
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { candidates += it.toString() }
        }
        if (isGroupSummary || allowMessageText) {
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let(candidates::add)
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let(candidates::add)
        }
        return candidates.firstNotNullOfOrNull { candidate ->
            whatsappMessageCountPattern.find(candidate.trim())
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
        }
    }

    /** Must be called with [notificationLock] held. */
    private fun publishSummariesLocked() {
        val summaries = notificationsByKey.values
            .groupBy { it.packageName }
            .mapValues { (packageName, notifications) ->
                val preview = notifications
                    .asSequence()
                    .filter { !it.preview.isNullOrBlank() }
                    .maxByOrNull { it.postTime }
                    ?.preview
                val childCount = notifications.filterNot { it.isGroupSummary }.sumOf { it.badgeCount }
                val count = if (packageName in whatsappPackages) {
                    val groupSummaryCount = notifications
                        .asSequence()
                        .filter { it.isGroupSummary }
                        .maxOfOrNull { it.badgeCount } ?: 0
                    maxOf(groupSummaryCount, childCount)
                } else {
                    childCount
                }
                PackageNotificationSummary(count = count, preview = preview)
            }
            .filterValues { it.count > 0 || !it.preview.isNullOrBlank() }
        _packageNotificationSummaries.value = summaries
    }
}
