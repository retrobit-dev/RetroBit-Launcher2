package com.retrobit.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val prefs: SharedPreferences = context.getSharedPreferences("retrobit_launcher_prefs", Context.MODE_PRIVATE)
    private val iconPackManager = IconPackManager(context)

    companion object {
        private const val PREF_FAVORITES = "pinned_favorites"
        private const val PREF_HIDDEN = "hidden_apps"
        private const val PREF_LABELS = "custom_labels"
        private const val PREF_DETOX_RESTRICTED = "detox_restricted_apps"
    }

    // 1. Favorites
    fun getFavoritePackageNames(): Set<String> {
        return prefs.getStringSet(PREF_FAVORITES, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(packageName: String): Boolean {
        val current = getFavoritePackageNames().toMutableSet()
        val isFav = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        prefs.edit().putStringSet(PREF_FAVORITES, current).apply()
        return isFav
    }

    // 1b. Detox Restricted (Mindful Delay Apps)
    fun getDetoxRestrictedPackageNames(): Set<String> {
        return prefs.getStringSet(PREF_DETOX_RESTRICTED, emptySet()) ?: emptySet()
    }

    fun toggleDetoxRestricted(packageName: String): Boolean {
        val current = getDetoxRestrictedPackageNames().toMutableSet()
        val isRestricted = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        prefs.edit().putStringSet(PREF_DETOX_RESTRICTED, current).apply()
        return isRestricted
    }

    // 2. Hidden Apps
    fun getHiddenPackageNames(): Set<String> {
        return prefs.getStringSet(PREF_HIDDEN, emptySet()) ?: emptySet()
    }

    fun toggleHideApp(packageName: String): Boolean {
        val current = getHiddenPackageNames().toMutableSet()
        val isHidden = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        prefs.edit().putStringSet(PREF_HIDDEN, current).apply()
        return isHidden
    }

    // 3. Custom Labels / Rename App
    fun getCustomLabels(): Map<String, String> {
        val jsonStr = prefs.getString(PREF_LABELS, "{}") ?: "{}"
        val map = mutableMapOf<String, String>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = json.getString(k)
            }
        } catch (e: Exception) {}
        return map
    }

    fun renameApp(packageName: String, newLabel: String) {
        val map = getCustomLabels().toMutableMap()
        if (newLabel.isBlank()) {
            map.remove(packageName)
        } else {
            map[packageName] = newLabel.trim()
        }
        val json = JSONObject(map as Map<*, *>).toString()
        prefs.edit().putString(PREF_LABELS, json).apply()
    }

    // 4. Query Installed Apps
    suspend fun loadInstalledApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val userHandle: UserHandle = Process.myUserHandle()
        val activities = launcherApps.getActivityList(null, userHandle)
        val usageStatsRepository = UsageStatsRepository(context)
        val usageByPackage = usageStatsRepository.todayUsageByPackage()
        val favorites = getFavoritePackageNames()
        val hidden = getHiddenPackageNames()
        val customNames = getCustomLabels()
        val detoxApps = getDetoxRestrictedPackageNames()
        val selectedIconPack = prefs.getString("feature_icon_pack", null)

        activities.map { info ->
            val pkg = info.applicationInfo.packageName
            val compName = info.name
            val original = info.label.toString()
            val custom = customNames[pkg]

            val catTag = when {
                pkg.contains("message") || pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") -> "COMM"
                pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox") -> "NET"
                pkg.contains("camera") || pkg.contains("photo") || pkg.contains("gallery") -> "OPTIC"
                pkg.contains("music") || pkg.contains("spotify") || pkg.contains("audio") -> "MEDIA"
                pkg.contains("terminal") || pkg.contains("termux") -> "SYS"
                pkg.contains("bank") || pkg.contains("wallet") || pkg.contains("crypto") -> "DATA"
                pkg.contains("tool") || pkg.contains("calc") || pkg.contains("map") -> "TOOL"
                pkg.contains("settings") -> "SYS"
                else -> "NODE"
            }

            AppModel(
                packageName = pkg,
                componentName = compName,
                originalLabel = original,
                customLabel = custom,
                icon = iconPackManager.iconFor(pkg, compName, selectedIconPack) ?: info.getBadgedIcon(0),
                isFavorite = favorites.contains(pkg),
                isHidden = hidden.contains(pkg),
                isDetoxRestricted = detoxApps.contains(pkg),
                categoryTag = catTag,
                screentimeText = usageByPackage[pkg]?.let(usageStatsRepository::formatDuration) ?: "—",
                subtext = "pkg://$pkg"
            )
        }.sortedBy { it.displayLabel.lowercase() }
    }

    // 5. App Launching & System Actions
    fun launchApp(app: AppModel) {
        try {
            val component = ComponentName(app.packageName, app.componentName)
            launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
        } catch (e: Exception) {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) context.startActivity(intent)
        }
    }

    fun openAppDetails(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // 6. Backup & Restore
    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("app", "RetroBit Launcher")
        root.put("version", "2.5.0")
        root.put("favorites", JSONArray(getFavoritePackageNames()))
        root.put("hidden", JSONArray(getHiddenPackageNames()))
        root.put("detoxRestricted", JSONArray(getDetoxRestrictedPackageNames()))
        root.put("labels", JSONObject(getCustomLabels()))
        return root.toString(2)
    }

    fun importBackupJson(content: String) {
        val root = JSONObject(content)
        fun readSet(key: String): Set<String> {
            val array = root.optJSONArray(key) ?: JSONArray()
            return buildSet {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim()
                    if (value.isNotEmpty()) add(value)
                }
            }
        }
        val labelsObject = root.optJSONObject("labels") ?: JSONObject()
        val labels = mutableMapOf<String, String>()
        val keys = labelsObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = labelsObject.optString(key).trim()
            if (value.isNotEmpty()) labels[key] = value
        }
        prefs.edit()
            .putStringSet(PREF_FAVORITES, readSet("favorites"))
            .putStringSet(PREF_HIDDEN, readSet("hidden"))
            .putStringSet(PREF_DETOX_RESTRICTED, readSet("detoxRestricted"))
            .putString(PREF_LABELS, JSONObject(labels as Map<*, *>).toString())
            .apply()
    }
}
