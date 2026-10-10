package com.retrobit.launcher.data

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Persistent settings for launcher features; all data stays on-device. */
class LauncherFeatureStore(context: Context) {
    private val prefs = context.getSharedPreferences("retrobit_launcher_prefs", Context.MODE_PRIVATE)

    data class TodoItem(val id: Long, val title: String, val done: Boolean)

    var weatherCity: String
        get() = prefs.getString(KEY_WEATHER_CITY, "Semarang") ?: "Semarang"
        set(value) { prefs.edit().putString(KEY_WEATHER_CITY, value.trim()).apply() }

    var themeModeName: String
        get() = prefs.getString(KEY_THEME_MODE, "LIGHT_EINK") ?: "LIGHT_EINK"
        set(value) { prefs.edit().putString(KEY_THEME_MODE, value).apply() }

    var voiceLanguage: String
        get() = prefs.getString(KEY_VOICE_LANGUAGE, "id-ID") ?: "id-ID"
        set(value) { prefs.edit().putString(KEY_VOICE_LANGUAGE, value).apply() }

    var focusModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_FOCUS_MODE, false)
        set(value) { prefs.edit().putBoolean(KEY_FOCUS_MODE, value).apply() }

    var iconSizeDp: Float
        get() = prefs.getFloat(KEY_ICON_SIZE, 42f).coerceIn(28f, 72f)
        set(value) { prefs.edit().putFloat(KEY_ICON_SIZE, value.coerceIn(28f, 72f)).apply() }

    var selectedIconPack: String?
        get() = prefs.getString(KEY_ICON_PACK, null)
        set(value) { prefs.edit().putString(KEY_ICON_PACK, value).apply() }

    var lockscreenEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCKSCREEN, false)
        set(value) { prefs.edit().putBoolean(KEY_LOCKSCREEN, value).apply() }

    var lockscreenPin: String?
        get() = if (prefs.contains(KEY_LOCKSCREEN_PIN_HASH)) "configured" else null
        set(value) {
            if (value.isNullOrBlank()) {
                prefs.edit().remove(KEY_LOCKSCREEN_PIN_HASH).remove(KEY_LOCKSCREEN_PIN_SALT).apply()
            } else {
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val hash = hashPin(value, salt)
                prefs.edit()
                    .putString(KEY_LOCKSCREEN_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                    .putString(KEY_LOCKSCREEN_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .apply()
            }
        }

    fun verifyLockscreenPin(value: String): Boolean = try {
        val salt = Base64.decode(prefs.getString(KEY_LOCKSCREEN_PIN_SALT, ""), Base64.NO_WRAP)
        val expected = Base64.decode(prefs.getString(KEY_LOCKSCREEN_PIN_HASH, ""), Base64.NO_WRAP)
        MessageDigest.isEqual(expected, hashPin(value, salt))
    } catch (_: Exception) {
        false
    }

    private fun hashPin(value: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(value.toCharArray(), salt, 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    var customPaletteJson: String?
        get() = prefs.getString(KEY_CUSTOM_PALETTE, null)
        set(value) { prefs.edit().putString(KEY_CUSTOM_PALETTE, value).apply() }

    fun getTodoItems(): List<TodoItem> = try {
        val array = JSONArray(prefs.getString(KEY_TODOS, "[]") ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title").trim()
            if (title.isEmpty()) null else TodoItem(
                id = item.optLong("id", index.toLong()),
                title = title,
                done = item.optBoolean("done", false)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun addTodo(title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        saveTodos(getTodoItems() + TodoItem(System.currentTimeMillis(), cleanTitle, false))
    }

    fun toggleTodo(id: Long) {
        saveTodos(getTodoItems().map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun removeTodo(id: Long) {
        saveTodos(getTodoItems().filterNot { it.id == id })
    }

    private fun saveTodos(items: List<TodoItem>) {
        val json = JSONArray()
        items.forEach { item ->
            json.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("done", item.done)
            })
        }
        prefs.edit().putString(KEY_TODOS, json.toString()).apply()
    }

    fun getAppTimers(): Map<String, Int> = try {
        val json = JSONObject(prefs.getString(KEY_APP_TIMERS, "{}") ?: "{}")
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.optInt(key, 0)
                if (value > 0) put(key, value)
            }
        }
    } catch (_: Exception) {
        emptyMap()
    }

    fun setAppTimer(packageName: String, minutes: Int?) {
        val timers = getAppTimers().toMutableMap()
        if (minutes == null || minutes <= 0) timers.remove(packageName)
        else timers[packageName] = minutes
        prefs.edit().putString(KEY_APP_TIMERS, JSONObject(timers as Map<*, *>).toString()).apply()
    }

    fun getAppTimerMinutes(packageName: String): Int? = getAppTimers()[packageName]

    fun getProfileNames(): List<String> {
        val profiles = readProfiles()
        val names = mutableListOf<String>()
        val keys = profiles.keys()
        while (keys.hasNext()) names += keys.next()
        return names.sorted()
    }

    fun activeProfileName(): String = prefs.getString(KEY_ACTIVE_PROFILE, "Personal") ?: "Personal"

    fun saveCurrentAsProfile(name: String, repository: AppRepository) {
        val cleanName = name.trim().take(30)
        if (cleanName.isEmpty()) return
        val profiles = readProfiles()
        profiles.put(cleanName, repository.exportBackupJson())
        prefs.edit()
            .putString(KEY_PROFILES, profiles.toString())
            .putString(KEY_ACTIVE_PROFILE, cleanName)
            .apply()
    }

    fun switchProfile(name: String, repository: AppRepository): Boolean {
        val profiles = readProfiles()
        val active = activeProfileName()
        if (profiles.has(active)) profiles.put(active, repository.exportBackupJson())
        val snapshot = profiles.optString(name, "")
        if (snapshot.isBlank()) return false
        return try {
            repository.importBackupJson(snapshot)
            prefs.edit()
                .putString(KEY_PROFILES, profiles.toString())
                .putString(KEY_ACTIVE_PROFILE, name)
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun ensureDefaultProfiles(repository: AppRepository) {
        val profiles = readProfiles()
        if (profiles.length() > 0) return
        val base = repository.exportBackupJson()
        profiles.put("Personal", base)
        profiles.put("Work", base)
        profiles.put("Gaming", base)
        prefs.edit()
            .putString(KEY_PROFILES, profiles.toString())
            .putString(KEY_ACTIVE_PROFILE, "Personal")
            .apply()
    }

    private fun readProfiles(): JSONObject = try {
        JSONObject(prefs.getString(KEY_PROFILES, "{}") ?: "{}")
    } catch (_: Exception) {
        JSONObject()
    }

    companion object {
        private const val KEY_WEATHER_CITY = "feature_weather_city"
        private const val KEY_THEME_MODE = "feature_theme_mode"
        private const val KEY_VOICE_LANGUAGE = "voice_language"
        private const val KEY_FOCUS_MODE = "feature_focus_mode"
        private const val KEY_ICON_SIZE = "feature_icon_size"
        private const val KEY_ICON_PACK = "feature_icon_pack"
        private const val KEY_LOCKSCREEN = "feature_lockscreen"
        private const val KEY_LOCKSCREEN_PIN_HASH = "feature_lockscreen_pin_hash"
        private const val KEY_LOCKSCREEN_PIN_SALT = "feature_lockscreen_pin_salt"
        private const val KEY_CUSTOM_PALETTE = "feature_custom_palette"
        private const val KEY_TODOS = "feature_todos"
        private const val KEY_APP_TIMERS = "feature_app_timers"
        private const val KEY_PROFILES = "feature_profiles"
        private const val KEY_ACTIVE_PROFILE = "feature_active_profile"
    }
}
