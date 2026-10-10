package com.retrobit.launcher.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class CalendarEvent(val title: String, val begin: Long)

@Composable
fun CalendarDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalRetroBitColors.current
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var hasCalendarPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCalendarPermission = granted
    }

    val displayedMonth = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val monthLabel = remember(monthOffset) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayedMonth.time).uppercase()
    }

    LaunchedEffect(selectedDay, hasCalendarPermission) {
        events = if (hasCalendarPermission) withContext(Dispatchers.IO) {
            queryEvents(context, selectedDay)
        } else emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.85f)
                .background(colors.background).border(2.dp, colors.border).padding(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CALENDAR // DAY + MONTH", color = colors.inkPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("[X]", color = colors.inkPrimary, modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("◀", color = colors.inkPrimary, modifier = Modifier.clickable {
                    val target = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                    monthOffset--
                    selectedDay = target.timeInMillis
                }.padding(8.dp))
                Text(monthLabel, color = colors.inkPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("▶", color = colors.inkPrimary, modifier = Modifier.clickable {
                    val target = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    monthOffset++
                    selectedDay = target.timeInMillis
                }.padding(8.dp))
            }

            val weekdays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, color = colors.inkSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            val offset = displayedMonth.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            val daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val today = Calendar.getInstance()
            val selectedCalendar = Calendar.getInstance().apply { timeInMillis = selectedDay }
            repeat(6) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - offset + 1
                        val valid = day in 1..daysInMonth
                        val isSelected = valid && selectedCalendar.get(Calendar.YEAR) == displayedMonth.get(Calendar.YEAR) &&
                            selectedCalendar.get(Calendar.MONTH) == displayedMonth.get(Calendar.MONTH) && selectedCalendar.get(Calendar.DAY_OF_MONTH) == day
                        val isToday = valid && today.get(Calendar.YEAR) == displayedMonth.get(Calendar.YEAR) &&
                            today.get(Calendar.MONTH) == displayedMonth.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) == day
                        Box(
                            Modifier.weight(1f).padding(2.dp)
                                .background(if (isSelected) colors.inkPrimary else colors.surface)
                                .border(if (isToday) 1.dp else 0.5.dp, if (isSelected) colors.inkPrimary else colors.border)
                                .clickable(enabled = valid) {
                                    selectedDay = (displayedMonth.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, day)
                                        set(Calendar.HOUR_OF_DAY, 12)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                }
                                .height(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (valid) Text(
                                "$day",
                                color = if (isSelected) colors.background else colors.inkPrimary,
                                fontSize = 9.sp,
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val selectedLabel = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDay))
            Text("AGENDA // ${selectedLabel.uppercase()}", color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (!hasCalendarPermission) {
                Text(
                    "Grant calendar access to read events. The month view works without it.",
                    color = colors.inkMuted,
                    fontSize = 9.sp
                )
                Text(
                    "GRANT CALENDAR ACCESS",
                    color = colors.inkPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }.padding(vertical = 8.dp)
                )
            } else if (events.isEmpty()) {
                Text("No events for this day.", color = colors.inkMuted, fontSize = 9.sp)
            } else {
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    events.take(8).forEach { event ->
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.begin))
                        Text("$time  ${event.title}", color = colors.inkSecondary, fontSize = 9.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

private fun queryEvents(context: Context, selectedDayMillis: Long): List<CalendarEvent> {
    return try {
        val start = Calendar.getInstance().apply {
            timeInMillis = selectedDayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)
        val projection = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN)
        context.contentResolver.query(builder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            buildList {
                val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleColumn).orEmpty().ifBlank { "(Untitled event)" }
                    add(CalendarEvent(title, cursor.getLong(beginColumn)))
                }
            }
        }.orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}
