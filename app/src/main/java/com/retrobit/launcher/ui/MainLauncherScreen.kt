package com.retrobit.launcher.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.retrobit.launcher.data.AppModel
import com.retrobit.launcher.data.AppRepository
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.data.NotificationMonitorService
import com.retrobit.launcher.data.UsageStatsRepository
import com.retrobit.launcher.ui.components.*
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import com.retrobit.launcher.ui.theme.RetroBitColors
import com.retrobit.launcher.ui.theme.RetroBitThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainLauncherScreen(
    repository: AppRepository,
    currentTheme: RetroBitThemeMode,
    onThemeChange: (RetroBitThemeMode) -> Unit,
    onCustomPaletteChange: (RetroBitColors?) -> Unit = {}
) {
    val colors = LocalRetroBitColors.current
    val context = LocalContext.current
    val featureStore = remember(context) { LauncherFeatureStore(context) }
    val usageStatsRepository = remember(context) { UsageStatsRepository(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var allApps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var favorites by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var isCurved by remember { mutableStateOf(true) }
    var isMediaVisible by remember { mutableStateOf(true) }
    var isFocusModeEnabled by remember { mutableStateOf(featureStore.focusModeEnabled) }
    var iconSizeDp by remember { mutableFloatStateOf(featureStore.iconSizeDp) }

    // ==========================================================================
    // STEALTH AUTO-HIDE ENGINE (Daftar aplikasi otomatis hilang & muncul saat disentuh)
    // ==========================================================================
    var isAppListRevealed by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    var isAutoHideEnabled by remember { mutableStateOf(true) }

    fun registerInteraction() {
        isAppListRevealed = true
        lastInteractionTime = System.currentTimeMillis()
    }

    // Timer Inactivity Auto-Hide (4.5 Detik)
    LaunchedEffect(isAppListRevealed, lastInteractionTime, isAutoHideEnabled) {
        if (isAppListRevealed && isAutoHideEnabled && searchQuery.isEmpty()) {
            delay(4500)
            isAppListRevealed = false
        }
    }

    // Dialog & UI states
    var selectedAppForModal by remember { mutableStateOf<AppModel?>(null) }
    var appToRename by remember { mutableStateOf<AppModel?>(null) }
    var appPendingMindfulLaunch by remember { mutableStateOf<AppModel?>(null) }
    var launchRestriction by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isTerminalOpen by remember { mutableStateOf(false) }
    var isMatrixRainOpen by remember { mutableStateOf(false) }
    var isWeatherConfigOpen by remember { mutableStateOf(false) }
    var isQuickControlsOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isLockScreenOpen by remember { mutableStateOf(featureStore.lockscreenEnabled && featureStore.lockscreenPin != null) }
    var isTodoOpen by remember { mutableStateOf(false) }
    var isCalendarOpen by remember { mutableStateOf(false) }
    var isEpaperRefreshing by remember { mutableStateOf(false) }

    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && featureStore.lockscreenEnabled && !featureStore.lockscreenPin.isNullOrBlank()) {
                isLockScreenOpen = true
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    // Live Notification Listener Stream (Niagara Preview)
    val notificationsByPackage by NotificationMonitorService.activeNotifications.collectAsState()

    val refreshApps: () -> Unit = {
        scope.launch {
            val list = repository.loadInstalledApps()
            allApps = list
            favorites = list.filter { it.isFavorite && !it.isHidden }
        }
    }

    LaunchedEffect(Unit) {
        refreshApps()
    }

    fun launchAppWithPolicies(app: AppModel, skipMindfulPause: Boolean = false) {
        if (isFocusModeEnabled && !app.isFavorite) {
            launchRestriction = "FOCUS MODE" to "Focus mode allows launching favorite apps only. Change favorites or disable Focus Mode in Settings."
            return
        }
        scope.launch {
            val limit = featureStore.getAppTimerMinutes(app.packageName)
            val usageMillis = if (limit != null) withContext(Dispatchers.IO) {
                if (!usageStatsRepository.hasUsageAccess()) null
                else usageStatsRepository.todayUsageByPackage()[app.packageName] ?: 0L
            } else null
            if (limit != null && usageMillis == null) {
                launchRestriction = "USAGE ACCESS REQUIRED" to "App timer for ${app.displayLabel} cannot be enforced until Usage Access is granted in Settings."
                return@launch
            }
            if (limit != null && usageMillis!! >= limit * 60_000L) {
                launchRestriction = "APP TIMER REACHED" to "${app.displayLabel} has used its $limit-minute daily limit. Adjust the timer in Settings to continue."
                return@launch
            }
            if (app.isDetoxRestricted && !skipMindfulPause) appPendingMindfulLaunch = app
            else repository.launchApp(app)
        }
    }

    fun handleVoiceCommand(spoken: String) {
        val command = spoken.trim().lowercase()
        when {
            command == "matrix" || command.contains("matrix rain") || command.contains("hujan matrix") -> {
                isMatrixRainOpen = true
            }
            command in setOf("tiles", "quick settings", "kontrol cepat", "quick controls") -> {
                isQuickControlsOpen = true
            }
            command in setOf("todo", "to do", "daftar tugas", "tugas") -> isTodoOpen = true
            command in setOf("calendar", "kalender", "agenda") -> isCalendarOpen = true
            command in setOf("settings", "pengaturan", "konfigurasi") -> isSettingsOpen = true
            command in setOf("weather", "cuaca") -> isWeatherConfigOpen = true
            command in setOf("help", "bantuan", "perintah") -> {
                isTerminalOpen = true
            }
            command in setOf("refresh", "segarkan", "refresh layar") -> isEpaperRefreshing = true
            command in setOf("show apps", "tampilkan aplikasi", "buka daftar aplikasi", "daftar aplikasi") -> {
                searchQuery = ""
                registerInteraction()
            }
            command.startsWith("cari ") || command.startsWith("search ") -> {
                searchQuery = command.substringAfter(' ')
                registerInteraction()
            }
            command.startsWith("tema ") || command.startsWith("theme ") -> {
                val theme = command.substringAfter(' ')
                val next = when (theme) {
                    "terang", "light", "paper" -> RetroBitThemeMode.LIGHT_EINK
                    "gelap", "dark", "onyx" -> RetroBitThemeMode.DARK_EINK
                    "pocket", "gameboy" -> RetroBitThemeMode.POCKET_90S
                    "stark", "noir" -> RetroBitThemeMode.STARK_1BIT
                    else -> null
                }
                if (next != null) {
                    onThemeChange(next)
                    isEpaperRefreshing = true
                } else {
                    searchQuery = spoken
                }
            }
            command.startsWith("buka ") || command.startsWith("open ") -> {
                val appQuery = command.substringAfter(' ')
                if (appQuery in setOf("daftar", "apps", "aplikasi")) {
                    searchQuery = ""
                    registerInteraction()
                } else {
                    val match = allApps
                        .mapNotNull { app -> FuzzySearch.score(appQuery, app.displayLabel)?.let { app to it } }
                        .maxByOrNull { it.second }
                    if (match != null && match.second >= 250) {
                        launchAppWithPolicies(match.first)
                    } else {
                        searchQuery = appQuery
                        registerInteraction()
                    }
                }
            }
            else -> {
                searchQuery = spoken
                registerInteraction()
            }
        }
    }

    // Attach active notification preview to apps
    val appsWithNotifications = remember(allApps, notificationsByPackage) {
        allApps.map { app ->
            val notif = notificationsByPackage[app.packageName]
            if (notif != null) app.copy(notificationPreview = notif) else app
        }
    }

    // Inline Math calculation evaluator
    val mathResult = remember(searchQuery) {
        val trimmed = searchQuery.trim()
        val isMath = trimmed.matches(Regex("^[0-9+\\-*/().\\s^%]+$")) && trimmed.any { it in "+-*/" }
        if (isMath) {
            try {
                evalMathSimple(trimmed)
            } catch (e: Exception) { null }
        } else null
    }

    val hiddenCount = remember(appsWithNotifications) { appsWithNotifications.count { it.isHidden } }

    val filteredApps = remember(appsWithNotifications, searchQuery, selectedCategory, isFocusModeEnabled) {
        var pool = appsWithNotifications

        if (selectedCategory == "HIDDEN") {
            pool = pool.filter { it.isHidden }
        } else {
            pool = pool.filter { !it.isHidden }
            if (selectedCategory == "FAV") {
                pool = pool.filter { it.isFavorite }
            } else if (selectedCategory != "ALL") {
                pool = pool.filter { it.categoryTag == selectedCategory }
            }
        }

        if (isFocusModeEnabled && selectedCategory != "HIDDEN") {
            pool = pool.filter { it.isFavorite }
        }

        if (searchQuery.isNotEmpty()) {
            pool.mapNotNull { app ->
                val score = listOf(app.displayLabel, app.packageName, app.categoryTag)
                    .mapNotNull { FuzzySearch.score(searchQuery, it) }
                    .maxOrNull()
                score?.let { app to it }
            }.sortedByDescending { it.second }
                .map { it.first }
        } else pool
    }

    val availableLetters = remember(filteredApps) {
        filteredApps.map { it.displayLabel.take(1).uppercase() }.toSet()
    }

    fun triggerRefreshCycle() {
        isEpaperRefreshing = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Header Telemetry Widget
            HeaderTelemetryWidget(
                onTriggerRefresh = { triggerRefreshCycle() }
            )

            WeatherWidget(
                store = featureStore,
                onConfigure = { isWeatherConfigOpen = true }
            )

            // 2. Retro Media Player Cassette Widget
            if (isMediaVisible) {
                RetroMediaPlayerWidget(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                )
            }

            // 3. Category Filter Chips
            CategoryChipsRow(
                selectedCategory = selectedCategory,
                hiddenCount = hiddenCount,
                onSelectCategory = {
                    selectedCategory = it
                    registerInteraction()
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )

            // 4. Command / Search Input Bar with Inline Calculator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .background(colors.surface)
                    .border(1.dp, colors.border)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.inkPrimary
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        registerInteraction()
                    },
                    textStyle = TextStyle(
                        color = colors.inkPrimary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(colors.inkPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search or calc (e.g. 45*12)...",
                                fontSize = 10.5.sp,
                                color = colors.inkMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                )

                mathResult?.let { res ->
                    Box(
                        modifier = Modifier
                            .background(colors.inkPrimary)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "= $res",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.background
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }

                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "[×]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.inkPrimary,
                        modifier = Modifier.clickable { searchQuery = "" }
                    )
                }
                VoiceCommandButton(onRecognized = ::handleVoiceCommand)
            }

            // 5. Main Body: Stealth Standby Mode vs Niagara Curved App List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // KONDISI 1: STANDBY STEALTH BANNER (Muncul saat daftar aplikasi otomatis tersembunyi)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isAppListRevealed,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { registerInteraction() }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(2.dp))
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "[✦] STEALTH DECK // ACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.inkPrimary,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "[░░░░░░░░░░ STANDBY ░░░░░░░░░░]",
                                fontSize = 9.5.sp,
                                color = colors.inkSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .border(1.dp, colors.border)
                                    .background(colors.background)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "TOUCH TO DEPLOY APPS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.inkPrimary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "// TAP HERE OR SLIDE A-Z SCRUBBER //",
                                fontSize = 8.sp,
                                color = colors.inkMuted,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // KONDISI 2: DAFTAR APLIKASI (MUNCUL SAAT DISENTUH)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isAppListRevealed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { registerInteraction() }
                    ) {
                        NiagaraCurvedAppList(
                            apps = filteredApps,
                            favorites = favorites,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            listState = listState,
                            isCurved = isCurved,
                            iconSizeDp = iconSizeDp,
                            onAppClick = { app ->
                                registerInteraction()
                                launchAppWithPolicies(app)
                            },
                            onAppLongClick = { app ->
                                registerInteraction()
                                selectedAppForModal = app
                            }
                        )
                    }
                }

                // Alphabet Scrubber di kanan (Menyentuh scrubber langsung memunculkan daftar aplikasi)
                AlphabetScrubber(
                    availableLetters = availableLetters,
                    onLetterSelected = { targetLetter ->
                        registerInteraction()
                        if (targetLetter == "★") {
                            scope.launch { listState.scrollToItem(0) }
                        } else {
                            val index = filteredApps.indexOfFirst {
                                it.displayLabel.startsWith(targetLetter, ignoreCase = true)
                            }
                            if (index >= 0) {
                                val targetScrollIndex = if (favorites.isNotEmpty() && selectedCategory == "ALL") {
                                    index + favorites.size + 2
                                } else index + 1
                                scope.launch {
                                    listState.scrollToItem(targetScrollIndex.coerceAtLeast(0))
                                }
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // 6. Bottom Chassis Deck Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .border(0.5.dp, colors.border)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeckButton(label = "⟳ REFRESH") { triggerRefreshCycle() }
                    DeckButton(label = ">_ TERM") { isTerminalOpen = true }
                    DeckButton(label = if (isAutoHideEnabled) "STEALTH: ON" else "STEALTH: OFF") {
                        isAutoHideEnabled = !isAutoHideEnabled
                        if (!isAutoHideEnabled) isAppListRevealed = true
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeckButton(label = if (isCurved) "ARC: ON" else "ARC: OFF") {
                        isCurved = !isCurved
                    }
                    DeckButton(label = "SETTINGS") { isSettingsOpen = true }
                }
            }
        }

        // 7. Modals
        launchRestriction?.let { (title, message) ->
            LaunchRestrictionDialog(
                title = title,
                message = message,
                onDismiss = { launchRestriction = null },
                onOpenSettings = {
                    launchRestriction = null
                    isSettingsOpen = true
                }
            )
        }

        selectedAppForModal?.let { app ->
            AppPopupDialog(
                app = app,
                onDismiss = { selectedAppForModal = null },
                onLaunch = {
                    selectedAppForModal = null
                    launchAppWithPolicies(app)
                },
                onRename = {
                    appToRename = app
                    selectedAppForModal = null
                },
                onToggleFavorite = {
                    repository.toggleFavorite(app.packageName)
                    selectedAppForModal = null
                    refreshApps()
                },
                onToggleHide = {
                    repository.toggleHideApp(app.packageName)
                    selectedAppForModal = null
                    refreshApps()
                },
                onToggleDetox = {
                    repository.toggleDetoxRestricted(app.packageName)
                    selectedAppForModal = null
                    refreshApps()
                },
                onOpenDetails = {
                    selectedAppForModal = null
                    repository.openAppDetails(app.packageName)
                },
                onUninstall = {
                    selectedAppForModal = null
                    repository.uninstallApp(app.packageName)
                }
            )
        }

        appPendingMindfulLaunch?.let { app ->
            MindfulPauseDialog(
                app = app,
                onProceed = {
                    appPendingMindfulLaunch = null
                    launchAppWithPolicies(app, skipMindfulPause = true)
                },
                onCancel = {
                    appPendingMindfulLaunch = null
                }
            )
        }

        appToRename?.let { app ->
            RenameAppDialog(
                app = app,
                onDismiss = { appToRename = null },
                onSaveRename = { newName ->
                    repository.renameApp(app.packageName, newName)
                    appToRename = null
                    refreshApps()
                }
            )
        }

        if (isTerminalOpen) {
            CyberTerminalDialog(
                onDismiss = { isTerminalOpen = false },
                onTriggerRefresh = { triggerRefreshCycle() },
                onChangeTheme = { newTheme ->
                    onThemeChange(newTheme)
                    triggerRefreshCycle()
                },
                onShowMatrixRain = { isMatrixRainOpen = true },
                onOpenQuickControls = { isQuickControlsOpen = true },
                onOpenTodo = { isTodoOpen = true },
                onOpenCalendar = { isCalendarOpen = true },
                onConfigureWeather = { isWeatherConfigOpen = true }
            )
        }

        if (isMatrixRainOpen) {
            MatrixRainDialog(onDismiss = { isMatrixRainOpen = false })
        }
        if (isWeatherConfigOpen) {
            WeatherCityDialog(
                store = featureStore,
                onDismiss = { isWeatherConfigOpen = false }
            )
        }
        if (isQuickControlsOpen) {
            QuickControlsDialog(onDismiss = { isQuickControlsOpen = false })
        }
        if (isTodoOpen) {
            TodoDialog(store = featureStore, onDismiss = { isTodoOpen = false })
        }
        if (isCalendarOpen) {
            CalendarDialog(onDismiss = { isCalendarOpen = false })
        }
        if (isSettingsOpen) {
            SettingsPanelDialog(
                repository = repository,
                store = featureStore,
                apps = allApps,
                currentTheme = currentTheme,
                focusModeEnabled = isFocusModeEnabled,
                iconSizeDp = iconSizeDp,
                onFocusModeChanged = {
                    isFocusModeEnabled = it
                    featureStore.focusModeEnabled = it
                },
                onIconSizeChanged = {
                    iconSizeDp = it
                    featureStore.iconSizeDp = it
                },
                onThemeChange = { onThemeChange(it); triggerRefreshCycle() },
                onCustomPaletteChange = onCustomPaletteChange,
                onRefreshApps = refreshApps,
                onOpenQuickControls = { isSettingsOpen = false; isQuickControlsOpen = true },
                onOpenTodo = { isSettingsOpen = false; isTodoOpen = true },
                onOpenCalendar = { isSettingsOpen = false; isCalendarOpen = true },
                onShowMatrix = { isSettingsOpen = false; isMatrixRainOpen = true },
                onConfigureWeather = { isSettingsOpen = false; isWeatherConfigOpen = true },
                onPreviewLockscreen = { isSettingsOpen = false; isLockScreenOpen = true },
                onDismiss = { isSettingsOpen = false }
            )
        }
        if (isLockScreenOpen) {
            CyberLockScreenDialog(store = featureStore, onDismiss = { isLockScreenOpen = false })
        }

        // 8. E-Paper Refresh Effect Overlay
        EpaperRefreshEffect(
            isRefreshing = isEpaperRefreshing,
            onRefreshFinished = { isEpaperRefreshing = false }
        )
    }
}

/**
 * Tombol toolbar kecil bergaya chassis cyberdeck untuk barisan kontrol bawah.
 */
@Composable
fun DeckButton(
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = colors.inkPrimary,
            letterSpacing = 0.5.sp
        )
    }
}

fun evalMathSimple(expr: String): String? {
    val clean = expr.replace(" ", "")
    val opIdx = clean.indexOfAny(charArrayOf('+', '-', '*', '/'))
    if (opIdx <= 0 || opIdx == clean.length - 1) return null

    val op = clean[opIdx]
    val left = clean.substring(0, opIdx).toDoubleOrNull() ?: return null
    val right = clean.substring(opIdx + 1).toDoubleOrNull() ?: return null

    val res = when (op) {
        '+' -> left + right
        '-' -> left - right
        '*' -> left * right
        '/' -> if (right != 0.0) left / right else return null
        else -> return null
    }

    return if (res % 1.0 == 0.0) res.toInt().toString() else String.format("%.2f", res)
}
