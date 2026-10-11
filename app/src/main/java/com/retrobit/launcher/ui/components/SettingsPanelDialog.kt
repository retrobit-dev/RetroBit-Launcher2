package com.retrobit.launcher.ui.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.data.AppRepository
import com.retrobit.launcher.data.DeviceLockController
import com.retrobit.launcher.data.IconPackManager
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.data.NotificationMonitorService
import com.retrobit.launcher.data.UsageStatsRepository
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import com.retrobit.launcher.ui.theme.PaletteCodec
import com.retrobit.launcher.ui.theme.RetroBitColors
import com.retrobit.launcher.ui.theme.RetroBitThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsPanelDialog(
    repository: AppRepository,
    store: LauncherFeatureStore,
    apps: List<AppModel>,
    currentTheme: RetroBitThemeMode,
    focusModeEnabled: Boolean,
    iconSizeDp: Float,
    launcherTextScale: Float,
    appDrawerAutoHideEnabled: Boolean,
    appDrawerAutoHideDelaySeconds: Int,
    homeSwipeThresholdDp: Float,
    onFocusModeChanged: (Boolean) -> Unit,
    onIconSizeChanged: (Float) -> Unit,
    onLauncherTextScaleChanged: (Float) -> Unit,
    onAppDrawerAutoHideEnabledChanged: (Boolean) -> Unit,
    onAppDrawerAutoHideDelayChanged: (Int) -> Unit,
    onHomeSwipeThresholdChanged: (Float) -> Unit,
    onThemeChange: (RetroBitThemeMode) -> Unit,
    onCustomPaletteChange: (RetroBitColors?) -> Unit,
    onRefreshApps: () -> Unit,
    onOpenQuickControls: () -> Unit,
    onOpenTodo: () -> Unit,
    onOpenCalendar: () -> Unit,
    onShowMatrix: () -> Unit,
    onConfigureWeather: () -> Unit,
    onPreviewLockscreen: () -> Unit,
    onActivateDeviceAdmin: () -> Unit,
    onRemoveDeviceAdmin: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalRetroBitColors.current
    val tabs = listOf("PROFILE", "WELLBEING", "APPEARANCE", "SERVICES", "TOOLS")
    var selectedTab by remember { mutableIntStateOf(0) }
    var profileDraft by remember { mutableStateOf("") }
    var activeProfile by remember { mutableStateOf(store.activeProfileName()) }
    var profileNames by remember { mutableStateOf(store.getProfileNames()) }
    var timerRevision by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("") }
    val usageRepository = remember(context) { UsageStatsRepository(context) }
    var hasUsageAccess by remember { mutableStateOf(usageRepository.hasUsageAccess()) }
    var usageSummary by remember { mutableStateOf("—") }
    var lockscreenEnabled by remember { mutableStateOf(store.lockscreenEnabled) }
    var pinDraft by remember { mutableStateOf("") }
    var doubleTapLockEnabled by remember { mutableStateOf(store.doubleTapToLockEnabled) }
    var swipeUpAction by remember { mutableStateOf(store.homeSwipeUpAction) }
    var swipeDownAction by remember { mutableStateOf(store.homeSwipeDownAction) }
    var deviceAdminActive by remember(context) { mutableStateOf(DeviceLockController.isAdminActive(context)) }
    val iconPacks = remember(context) { IconPackManager(context).installedPacks() }

    LaunchedEffect(Unit) {
        store.ensureDefaultProfiles(repository)
        profileNames = store.getProfileNames()
        activeProfile = store.activeProfileName()
        while (true) {
            val usage = withContext(Dispatchers.IO) {
                val allowed = usageRepository.hasUsageAccess()
                allowed to if (allowed) usageRepository.formatDuration(usageRepository.totalUsageTodayMillis()) else "Permission required"
            }
            hasUsageAccess = usage.first
            usageSummary = usage.second
            deviceAdminActive = DeviceLockController.isAdminActive(context)
            kotlinx.coroutines.delay(5_000)
        }
    }

    val palettePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val json = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            val parsed = json?.let { PaletteCodec.parse(it) }
            if (parsed != null && json != null) {
                store.customPaletteJson = json
                onCustomPaletteChange(parsed)
                statusMessage = "Custom palette imported."
            } else {
                statusMessage = "Invalid palette JSON. See the sample schema below."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.88f)
                .background(colors.background).border(2.dp, colors.border).padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("RETROBIT // SETTINGS", color = colors.inkPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                Text("[X]", color = colors.inkPrimary, modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                tabs.forEachIndexed { index, title ->
                    Text(
                        title,
                        color = if (selectedTab == index) colors.background else colors.inkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .background(if (selectedTab == index) colors.inkPrimary else colors.surface)
                            .border(1.dp, colors.border)
                            .clickable { selectedTab = index }
                            .padding(horizontal = 7.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                when (selectedTab) {
                    0 -> {
                        SectionLabel("PROFILES")
                        Text("Active: $activeProfile", color = colors.inkSecondary, fontSize = 9.sp)
                        Spacer(Modifier.height(5.dp))
                        profileNames.forEach { name ->
                            SettingAction(
                                title = if (name == activeProfile) "● $name (ACTIVE)" else name,
                                detail = "Switch favorites, hidden apps, and aliases to this profile.",
                                onClick = {
                                    if (store.switchProfile(name, repository)) {
                                        activeProfile = name
                                        onRefreshApps()
                                        statusMessage = "Switched to $name profile."
                                    } else statusMessage = "Could not load profile."
                                }
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        BasicTextField(
                            value = profileDraft,
                            onValueChange = { profileDraft = it },
                            singleLine = true,
                            textStyle = TextStyle(color = colors.inkPrimary, fontSize = 10.sp),
                            cursorBrush = SolidColor(colors.inkPrimary),
                            modifier = Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(9.dp),
                            decorationBox = { inner ->
                                if (profileDraft.isEmpty()) Text("Profile name…", color = colors.inkMuted, fontSize = 9.sp)
                                inner()
                            }
                        )
                        SettingAction("SAVE CURRENT AS PROFILE", "Stores the current favorites, hidden apps, and aliases on this device.") {
                            store.saveCurrentAsProfile(profileDraft, repository)
                            activeProfile = store.activeProfileName()
                            profileNames = store.getProfileNames()
                            profileDraft = ""
                            statusMessage = "Profile saved locally."
                        }
                        SectionLabel("LOCKSCREEN")
                        Text("This is a local launcher overlay, not a replacement for Android's secure keyguard.", color = colors.inkMuted, fontSize = 9.sp)
                        ToggleSetting("ENABLE LOCKSCREEN PREVIEW", "Shows the cyberdeck overlay when preview is opened.", lockscreenEnabled) {
                            lockscreenEnabled = it
                            store.lockscreenEnabled = it
                            if (it) onPreviewLockscreen()
                        }
                        BasicTextField(
                            value = pinDraft,
                            onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) pinDraft = it },
                            singleLine = true,
                            textStyle = TextStyle(color = colors.inkPrimary, fontSize = 10.sp),
                            cursorBrush = SolidColor(colors.inkPrimary),
                            modifier = Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(9.dp),
                            decorationBox = { inner ->
                                if (pinDraft.isEmpty()) Text("Set local PIN (4–8 digits)", color = colors.inkMuted, fontSize = 9.sp)
                                inner()
                            }
                        )
                        SettingAction("SAVE LOCAL PIN", "Stored locally for the in-app preview; use device lock for real security.") {
                            if (pinDraft.length in 4..8) {
                                store.lockscreenPin = pinDraft
                                pinDraft = ""
                                statusMessage = "Local lock-screen PIN saved."
                            } else statusMessage = "PIN must contain 4–8 digits."
                        }
                        SettingAction("PREVIEW CYBERDECK LOCKSCREEN", "Open the in-app lock screen preview.", onPreviewLockscreen)
                    }
                    1 -> {
                        SectionLabel("DIGITAL WELLBEING")
                        SettingAction(
                            title = "TODAY: $usageSummary",
                            detail = if (hasUsageAccess) "Real foreground time from Android Usage Stats." else "Grant Usage Access to read genuine screen-time data.",
                            onClick = {
                                runCatching { usageRepository.openUsageAccessSettings() }
                                statusMessage = "Return here after granting Usage Access."
                            }
                        )
                        SettingAction("REFRESH USAGE PERMISSION STATUS", "Current permission: ${if (hasUsageAccess) "granted" else "not granted"}.") {
                            hasUsageAccess = usageRepository.hasUsageAccess()
                            statusMessage = if (hasUsageAccess) "Usage Access is enabled." else "Usage Access is still off."
                        }
                        ToggleSetting("FOCUS MODE", "Only favorite apps remain in the launcher list.", focusModeEnabled, onFocusModeChanged)
                        SectionLabel("APP TIMERS // TAP A ROW TO CYCLE LIMIT")
                        if (apps.isEmpty()) Text("Installed apps are still loading.", color = colors.inkMuted, fontSize = 9.sp)
                        key(timerRevision) {
                            apps.take(12).forEach { app ->
                                val timer = store.getAppTimerMinutes(app.packageName)
                                SettingAction(
                                    title = "${app.displayLabel}  //  ${timer?.let { "${it}m/day" } ?: "NO LIMIT"}",
                                    detail = "Usage today: ${app.screentimeText}. Tap to cycle 15 / 30 / 60 / off.",
                                    onClick = {
                                        val next = when (timer) { null -> 15; 15 -> 30; 30 -> 60; else -> null }
                                        store.setAppTimer(app.packageName, next)
                                        timerRevision++
                                    }
                                )
                            }
                        }
                    }
                    2 -> {
                        SectionLabel("THEME")
                        RetroBitThemeMode.entries.forEach { mode ->
                            SettingAction(
                                title = mode.name.replace('_', ' '),
                                detail = if (currentTheme == mode) "ACTIVE THEME" else "Tap to apply this built-in palette.",
                                onClick = {
                                    store.customPaletteJson = null
                                    onCustomPaletteChange(null)
                                    onThemeChange(mode)
                                    statusMessage = "Theme applied."
                                }
                            )
                        }
                        SectionLabel("ICON SIZE")
                        Text("${iconSizeDp.toInt()} dp", color = colors.inkSecondary, fontSize = 9.sp)
                        Slider(value = iconSizeDp, onValueChange = onIconSizeChanged, valueRange = 28f..72f)
                        SectionLabel("FONT SIZE")
                        Text(
                            "${(launcherTextScale * 100).toInt()}% OF NORMAL",
                            color = colors.inkSecondary,
                            fontSize = 9.sp
                        )
                        Slider(
                            value = launcherTextScale,
                            onValueChange = onLauncherTextScaleChanged,
                            valueRange = 0.8f..1.5f,
                            steps = 6
                        )
                        Text(
                            "Scales launcher text and keeps Android's system font scaling active.",
                            color = colors.inkMuted,
                            fontSize = 8.sp
                        )
                        SettingAction("RESET FONT SIZE", "Return launcher text to 100% of normal.") {
                            onLauncherTextScaleChanged(1f)
                            statusMessage = "Font size reset to 100%."
                        }
                        SectionLabel("ICON PACK")
                        SettingAction("SYSTEM ICONS", "Use the app icons supplied by Android.", onClick = {
                            store.selectedIconPack = null
                            onRefreshApps()
                            statusMessage = "System icons selected."
                        })
                        if (iconPacks.isEmpty()) {
                            Text("No compatible installed icon packs were found.", color = colors.inkMuted, fontSize = 9.sp)
                        } else {
                            iconPacks.forEach { pack ->
                                SettingAction(
                                    title = if (store.selectedIconPack == pack.packageName) "● ${pack.label}" else pack.label,
                                    detail = pack.packageName,
                                    onClick = {
                                        store.selectedIconPack = pack.packageName
                                        onRefreshApps()
                                        statusMessage = "Icon pack applied where appfilter mappings exist."
                                    }
                                )
                            }
                        }
                        SettingAction("IMPORT CUSTOM PALETTE JSON", "Choose a JSON file with all seven color fields.") {
                            palettePicker.launch(arrayOf("application/json", "text/*"))
                        }
                        SettingAction("RESET CUSTOM PALETTE", "Use the selected built-in theme again.") {
                            store.customPaletteJson = null
                            onCustomPaletteChange(null)
                            statusMessage = "Custom palette cleared."
                        }
                        Text(
                            "Schema: {\"background\":\"#F0F2EB\",\"surface\":\"#E4E7DD\",\"subtle\":\"#D8DCD0\",\"inkPrimary\":\"#111210\",\"inkSecondary\":\"#484B45\",\"inkMuted\":\"#797D74\",\"border\":\"#111210\"}",
                            color = colors.inkMuted,
                            fontSize = 7.sp
                        )
                    }
                    3 -> {
                        SectionLabel("VOICE // ID / EN")
                        SettingAction(
                            title = "VOICE LANGUAGE: ${store.voiceLanguage}",
                            detail = "Tap to switch between Indonesian and English speech recognition.",
                            onClick = {
                                store.voiceLanguage = if (store.voiceLanguage.startsWith("id")) "en-US" else "id-ID"
                                statusMessage = "Voice language: ${store.voiceLanguage}"
                            }
                        )
                        Text("Commands: search/cari, open/buka app, theme/tema, show apps, refresh, matrix, tiles, todo, calendar, help.", color = colors.inkMuted, fontSize = 9.sp)
                        SectionLabel("WEATHER + NETWORK")
                        SettingAction("CONFIGURE WEATHER CITY", "Uses live Open-Meteo data; tap to set the city.", onConfigureWeather)
                        Text("LIVE NETWORK: ${rememberNetworkStatus()}", color = colors.inkSecondary, fontSize = 9.sp)
                        SectionLabel("MEDIA SESSION + NOTIFICATIONS")
                        SettingAction("OPEN NOTIFICATION ACCESS SETTINGS", "Required for active media sessions and notification previews.") {
                            runCatching { NotificationMonitorService.openPermissionSettings(context) }
                        }
                        SettingAction("QUICK CONTROLS", "Volume, brightness, flashlight, and system connectivity settings.", onOpenQuickControls)
                    }
                    else -> {
                        SectionLabel("APP DRAWER")
                        ToggleSetting(
                            "AUTO-HIDE APP DRAWER",
                            "Hide the drawer after ${appDrawerAutoHideDelaySeconds}s without interaction. The STEALTH button is a shortcut for this setting.",
                            appDrawerAutoHideEnabled
                        ) {
                            onAppDrawerAutoHideEnabledChanged(it)
                            statusMessage = if (it) "App drawer auto-hide enabled." else "App drawer will stay open until closed."
                        }
                        Text("AUTO-HIDE DELAY // $appDrawerAutoHideDelaySeconds SECONDS", color = colors.inkSecondary, fontSize = 9.sp)
                        Slider(
                            value = appDrawerAutoHideDelaySeconds.toFloat(),
                            onValueChange = { onAppDrawerAutoHideDelayChanged(it.toInt()) },
                            valueRange = 2f..10f,
                            steps = 7
                        )
                        SectionLabel("HOME GESTURES")
                        ToggleSetting(
                            "DOUBLE TAP TO LOCK PHONE",
                            "Requires Android approval for Device Admin; RetroBit requests only FORCE LOCK.",
                            doubleTapLockEnabled
                        ) {
                            doubleTapLockEnabled = it
                            store.doubleTapToLockEnabled = it
                            if (it && !deviceAdminActive) onActivateDeviceAdmin()
                        }
                        SettingAction(
                            title = "SWIPE UP // ${swipeUpAction.label}",
                            detail = "Tap to cycle: app drawer, quick controls, edit home, Matrix Rain, lock phone, disabled.",
                            onClick = {
                                swipeUpAction = swipeUpAction.next()
                                store.homeSwipeUpAction = swipeUpAction
                                statusMessage = "Swipe up: ${swipeUpAction.label}"
                            }
                        )
                        SettingAction(
                            title = "SWIPE DOWN // ${swipeDownAction.label}",
                            detail = "Tap to cycle: app drawer, quick controls, edit home, Matrix Rain, lock phone, disabled.",
                            onClick = {
                                swipeDownAction = swipeDownAction.next()
                                store.homeSwipeDownAction = swipeDownAction
                                statusMessage = "Swipe down: ${swipeDownAction.label}"
                            }
                        )
                        SectionLabel("SWIPE SENSITIVITY")
                        Text(
                            "${homeSwipeThresholdDp.toInt()} dp // LOWER IS MORE SENSITIVE",
                            color = colors.inkSecondary,
                            fontSize = 9.sp
                        )
                        Slider(
                            value = homeSwipeThresholdDp,
                            onValueChange = onHomeSwipeThresholdChanged,
                            valueRange = 48f..160f,
                            steps = 6
                        )
                        SettingAction(
                            title = if (deviceAdminActive) "DEVICE ADMIN APPROVAL: GRANTED" else "REQUEST DEVICE ADMIN APPROVAL",
                            detail = if (deviceAdminActive) "Double-tap can lock the phone. Use the revoke action below to remove approval." else "Tap to show Android's system approval screen for the FORCE LOCK policy.",
                            onClick = {
                                if (deviceAdminActive) {
                                    statusMessage = "Device Admin approval is already granted."
                                } else {
                                    doubleTapLockEnabled = true
                                    store.doubleTapToLockEnabled = true
                                    onActivateDeviceAdmin()
                                }
                            }
                        )
                        if (deviceAdminActive) {
                            SettingAction("REVOKE DEVICE ADMIN APPROVAL", "Opens Android Security settings so you can deactivate RetroBit.", onRemoveDeviceAdmin)
                        }
                        Text("Home gestures work across the homescreen: double-tap to lock, fast swipes run the selected actions, and holding anywhere toggles edit mode.", color = colors.inkMuted, fontSize = 8.sp)
                        SectionLabel("PRODUCTIVITY")
                        SettingAction("OPEN TODO WIDGET", "Add, check off, and remove locally stored tasks.", onOpenTodo)
                        SettingAction("OPEN CALENDAR", "Month/day view; optionally reads device calendar events.", onOpenCalendar)
                        SectionLabel("EASTER EGGS")
                        SettingAction("MATRIX RAIN", "Launch the animated cyberdeck easter egg.", onShowMatrix)
                        Text("Secret console commands: matrix, konami, secret, tiles, todo, calendar, weather.", color = colors.inkMuted, fontSize = 9.sp)
                    }
                }
                if (statusMessage.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(statusMessage, color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Permissions are optional and requested only when a feature needs them.", color = colors.inkMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalRetroBitColors.current
    Text(text, color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
}

@Composable
private fun SettingAction(title: String, detail: String, onClick: () -> Unit) {
    val colors = LocalRetroBitColors.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .background(colors.surface).border(1.dp, colors.border)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(title, color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = colors.inkSecondary, fontSize = 8.sp)
    }
}

@Composable
private fun ToggleSetting(title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalRetroBitColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .background(colors.surface).border(1.dp, colors.border)
            .clickable { onCheckedChange(!checked) }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = colors.inkSecondary, fontSize = 8.sp)
        }
        Text(if (checked) "ON" else "OFF", color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
