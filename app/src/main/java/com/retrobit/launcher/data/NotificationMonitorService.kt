package com.retrobit.launcher.data

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service untuk memantau notifikasi masuk dan menampilkan ringkasan preview
 * tepat di bawah nama aplikasi pada daftar favorit (seperti pada Niagara Launcher).
 */
class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private val _activeNotifications = MutableStateFlow<Map<String, String>>(emptyMap())
        val activeNotifications: StateFlow<Map<String, String>> = _activeNotifications.asStateFlow()

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            return enabledListeners.contains(context.packageName)
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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val pkg = it.packageName
            val extras = it.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            if (text.isNotBlank()) {
                val preview = if (title.isNotBlank()) "$title: \"$text\"" else "\"$text\""
                val current = _activeNotifications.value.toMutableMap()
                current[pkg] = preview
                _activeNotifications.value = current
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            val pkg = it.packageName
            val current = _activeNotifications.value.toMutableMap()
            current.remove(pkg)
            _activeNotifications.value = current
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val notifications = activeNotifications ?: return
            val map = mutableMapOf<String, String>()
            for (sbn in activeNotifications) {
                val pkg = sbn.packageName
                val extras = sbn.notification.extras
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                if (!text.isNullOrBlank()) {
                    val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
                    map[pkg] = if (title.isNotBlank()) "$title: \"$text\"" else "\"$text\""
                }
            }
            _activeNotifications.value = map
        } catch (e: Exception) {}
    }
}
