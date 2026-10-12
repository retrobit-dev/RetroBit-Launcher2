package com.retrobit.launcher.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.data.LauncherFeatureStore.WidgetPageModule
import com.retrobit.launcher.data.WeatherRepository
import com.retrobit.launcher.data.WeatherSnapshot
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun LockscreenWidgetsSection(
    store: LauncherFeatureStore,
    modules: List<WidgetPageModule>
) {
    val colors = LocalRetroBitColors.current
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("LOCKSCREEN WIDGETS", color = colors.inkPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "READ-ONLY WHILE LOCKED · SELECT MODULES IN SETTINGS",
                color = colors.inkSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        modules.forEach { module ->
            when (module) {
                WidgetPageModule.CALENDAR -> LockScreenCalendarCard()
                WidgetPageModule.NOTES -> LockScreenNotesCard(store)
                WidgetPageModule.TODO -> LockScreenTodoCard(store)
                WidgetPageModule.WEATHER -> LockScreenWeatherCard(store)
                WidgetPageModule.MEDIA -> LockScreenMediaCard()
            }
        }
    }
}

@Composable
private fun LockScreenCalendarCard() {
    val context = LocalContext.current
    val hasReadCalendarPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }
    val todayMillis = remember { System.currentTimeMillis() }
    val todayLabel = remember(todayMillis) {
        SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date(todayMillis)).uppercase()
    }
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }

    LaunchedEffect(todayLabel, hasReadCalendarPermission) {
        events = if (hasReadCalendarPermission) withContext(Dispatchers.IO) {
            queryEvents(context, todayMillis)
        } else emptyList()
    }

    LockScreenWidgetCard(title = "CALENDAR", detail = todayLabel) {
        when {
            !hasReadCalendarPermission -> Text(
                "Calendar read access is not enabled. Grant it in Android app settings while unlocked.",
                color = LocalRetroBitColors.current.inkSecondary,
                fontSize = 12.sp
            )
            events.isEmpty() -> Text("No events scheduled for today.", color = LocalRetroBitColors.current.inkSecondary, fontSize = 12.sp)
            else -> events.take(4).forEach { event ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val eventTime = if (event.allDay) "ALL DAY" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.begin))
                    Text(eventTime, color = LocalRetroBitColors.current.inkSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        event.title,
                        color = LocalRetroBitColors.current.inkPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LockScreenNotesCard(store: LauncherFeatureStore) {
    val colors = LocalRetroBitColors.current
    val note = store.widgetPageNote.trim()
    LockScreenWidgetCard(title = "NOTES", detail = "SAVED ON THIS DEVICE") {
        Text(
            text = note.ifBlank { "No note saved on the Widget Deck." },
            color = if (note.isBlank()) colors.inkSecondary else colors.inkPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            maxLines = 7,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LockScreenTodoCard(store: LauncherFeatureStore) {
    val colors = LocalRetroBitColors.current
    val openTasks = remember(store) { store.getTodoItems().filterNot { it.done } }
    LockScreenWidgetCard(title = "TO-DO", detail = "${openTasks.size} OPEN") {
        if (openTasks.isEmpty()) {
            Text("All caught up. No open tasks.", color = colors.inkSecondary, fontSize = 12.sp)
        } else {
            openTasks.take(4).forEach { task ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("○", color = colors.inkSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        task.title,
                        color = colors.inkPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (openTasks.size > 4) {
                Text("+ ${openTasks.size - 4} MORE TASKS", color = colors.inkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LockScreenWeatherCard(store: LauncherFeatureStore) {
    val colors = LocalRetroBitColors.current
    val city = store.weatherCity
    val repository = remember { WeatherRepository() }
    var snapshot by remember(city) { mutableStateOf<WeatherSnapshot?>(null) }
    var status by remember(city) { mutableStateOf("Loading current conditions…") }

    LaunchedEffect(city) {
        status = "Loading current conditions…"
        repository.currentWeather(city).fold(
            onSuccess = {
                snapshot = it
                status = ""
            },
            onFailure = {
                snapshot = null
                status = it.message ?: "Weather is unavailable right now."
            }
        )
    }

    LockScreenWidgetCard(title = "WEATHER", detail = city.uppercase()) {
        val current = snapshot
        if (current == null) {
            Text(status, color = colors.inkSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    if (current.temperatureC.isFinite()) "${current.temperatureC.roundToInt()}°C" else "—",
                    color = colors.inkPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(current.description, color = colors.inkPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(current.location, color = colors.inkSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            val extraConditions = listOfNotNull(
                current.humidityPercent?.let { "HUMIDITY  $it%" },
                current.windKph?.let { "WIND  ${it.roundToInt()} km/h" }
            ).joinToString("   ·   ")
            if (extraConditions.isNotBlank()) {
                Text(extraConditions, color = colors.inkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LockScreenMediaCard() {
    LockScreenWidgetCard(title = "MEDIA", detail = "READ-ONLY SESSION STATUS") {
        RetroMediaPlayerWidget(modifier = Modifier.fillMaxWidth(), readOnly = true)
    }
}

@Composable
private fun LockScreenWidgetCard(
    title: String,
    detail: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalRetroBitColors.current
    Column(
        Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = colors.inkPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(detail, color = colors.inkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.5f)))
        content()
    }
}
