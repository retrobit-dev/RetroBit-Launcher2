package com.retrobit.launcher.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.ContextCompat
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class CalendarEvent(val title: String, val begin: Long, val allDay: Boolean)
private data class WritableCalendar(val id: Long, val displayName: String)

@Composable
fun CalendarDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalRetroBitColors.current
    val scope = rememberCoroutineScope()
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var hasReadCalendarPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    var hasWriteCalendarPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var eventRevision by remember { mutableIntStateOf(0) }
    var isEventEditorOpen by remember { mutableStateOf(false) }
    var eventTitle by remember { mutableStateOf("") }
    var eventNotes by remember { mutableStateOf("") }
    var eventIsAllDay by remember { mutableStateOf(false) }
    var startHour by remember { mutableIntStateOf(9) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(10) }
    var endMinute by remember { mutableIntStateOf(0) }
    var isSavingEvent by remember { mutableStateOf(false) }
    var eventSaveMessage by remember { mutableStateOf("") }
    var permissionMessage by remember { mutableStateOf("") }
    var calendars by remember { mutableStateOf<List<WritableCalendar>>(emptyList()) }
    var selectedCalendarId by remember { mutableLongStateOf(-1L) }

    fun beginEventDraft() {
        eventTitle = ""
        eventNotes = ""
        eventIsAllDay = false
        startHour = 9
        startMinute = 0
        endHour = 10
        endMinute = 0
        eventSaveMessage = ""
        isEventEditorOpen = true
    }

    val readPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasReadCalendarPermission = granted
    }
    val writePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasReadCalendarPermission = results[Manifest.permission.READ_CALENDAR]
            ?: ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        hasWriteCalendarPermission = results[Manifest.permission.WRITE_CALENDAR]
            ?: ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (hasWriteCalendarPermission && hasReadCalendarPermission) beginEventDraft()
        else if (hasWriteCalendarPermission) permissionMessage = "Grant read access so RetroBit can choose a writable calendar."
        else permissionMessage = "Calendar write access is required to save an agenda."
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

    LaunchedEffect(selectedDay, hasReadCalendarPermission, eventRevision) {
        events = if (hasReadCalendarPermission) withContext(Dispatchers.IO) {
            queryEvents(context, selectedDay)
        } else emptyList()
    }

    LaunchedEffect(hasWriteCalendarPermission) {
        calendars = if (hasWriteCalendarPermission) withContext(Dispatchers.IO) {
            queryWritableCalendars(context)
        } else emptyList()
        if (calendars.none { it.id == selectedCalendarId }) {
            selectedCalendarId = calendars.firstOrNull()?.id ?: -1L
        }
    }

    fun requestAddAgenda() {
        permissionMessage = ""
        if (hasWriteCalendarPermission && hasReadCalendarPermission) {
            beginEventDraft()
        } else {
            val missingPermissions = buildList {
                if (!hasReadCalendarPermission) add(Manifest.permission.READ_CALENDAR)
                if (!hasWriteCalendarPermission) add(Manifest.permission.WRITE_CALENDAR)
            }
            writePermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    fun cycleCalendar() {
        if (calendars.isEmpty()) return
        val currentIndex = calendars.indexOfFirst { it.id == selectedCalendarId }
        selectedCalendarId = calendars[(currentIndex + 1).mod(calendars.size)].id
    }

    fun saveAgenda() {
        val title = eventTitle.trim()
        if (title.isEmpty()) {
            eventSaveMessage = "Enter an agenda title first."
            return
        }
        if (!hasWriteCalendarPermission || selectedCalendarId < 0L) {
            eventSaveMessage = if (!hasWriteCalendarPermission) "Calendar write access is required." else "No writable calendar is available on this device."
            return
        }
        isSavingEvent = true
        eventSaveMessage = ""
        val calendarId = selectedCalendarId
        scope.launch {
            val created = withContext(Dispatchers.IO) {
                insertCalendarEvent(
                    context = context,
                    calendarId = calendarId,
                    title = title,
                    notes = eventNotes.trim(),
                    selectedDayMillis = selectedDay,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    allDay = eventIsAllDay
                )
            }
            isSavingEvent = false
            if (created) {
                isEventEditorOpen = false
                eventRevision++
                permissionMessage = if (hasReadCalendarPermission) "Agenda saved to your Android calendar." else "Agenda saved. Grant read access to see it in this list."
            } else {
                eventSaveMessage = "Could not save the agenda. Check calendar access and try again."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.88f)
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

            val weekdays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            Row(Modifier.fillMaxWidth()) {
                weekdays.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, color = colors.inkSecondary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // Calendar.DAY_OF_WEEK uses Sunday=1; shift so Monday is column zero.
            val mondayFirstOffset = (displayedMonth.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            val daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
            val today = Calendar.getInstance()
            val selectedCalendar = Calendar.getInstance().apply { timeInMillis = selectedDay }
            repeat(6) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - mondayFirstOffset + 1
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
            Spacer(Modifier.height(7.dp))
            val selectedLabel = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDay))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("AGENDA // ${selectedLabel.uppercase()}", color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("+ ADD AGENDA", color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = ::requestAddAgenda).padding(5.dp))
            }
            if (permissionMessage.isNotBlank()) {
                Text(permissionMessage, color = colors.inkSecondary, fontSize = 8.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
            Spacer(Modifier.height(4.dp))

            if (isEventEditorOpen) {
                CalendarEventEditor(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    title = eventTitle,
                    onTitleChange = { eventTitle = it },
                    notes = eventNotes,
                    onNotesChange = { eventNotes = it },
                    allDay = eventIsAllDay,
                    onToggleAllDay = { eventIsAllDay = !eventIsAllDay },
                    startTime = formatTime(startHour, startMinute),
                    endTime = formatTime(endHour, endMinute),
                    selectedCalendarName = calendars.firstOrNull { it.id == selectedCalendarId }?.displayName,
                    calendarCount = calendars.size,
                    saveMessage = eventSaveMessage,
                    saving = isSavingEvent,
                    onChooseCalendar = ::cycleCalendar,
                    onChooseStartTime = {
                        TimePickerDialog(context, { _, hour, minute ->
                            startHour = hour
                            startMinute = minute
                            if (endHour * 60 + endMinute <= hour * 60 + minute) {
                                val nextEnd = (hour * 60 + minute + 60) % (24 * 60)
                                endHour = nextEnd / 60
                                endMinute = nextEnd % 60
                            }
                        }, startHour, startMinute, DateFormat.is24HourFormat(context)).show()
                    },
                    onChooseEndTime = {
                        TimePickerDialog(context, { _, hour, minute ->
                            endHour = hour
                            endMinute = minute
                        }, endHour, endMinute, DateFormat.is24HourFormat(context)).show()
                    },
                    onSave = ::saveAgenda,
                    onCancel = { isEventEditorOpen = false }
                )
            } else {
                if (!hasReadCalendarPermission) {
                    Text(
                        "Grant read access to see existing events. Adding an agenda requires separate calendar write approval.",
                        color = colors.inkMuted,
                        fontSize = 8.sp
                    )
                    Text(
                        "GRANT READ ACCESS",
                        color = colors.inkPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { readPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) }.padding(vertical = 6.dp)
                    )
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    if (hasReadCalendarPermission && events.isEmpty()) {
                        Text("No events for this day.", color = colors.inkMuted, fontSize = 9.sp)
                    } else if (hasReadCalendarPermission) {
                        events.take(12).forEach { event ->
                            val time = if (event.allDay) "ALL DAY" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.begin))
                            Text("$time  ${event.title}", color = colors.inkSecondary, fontSize = 9.sp, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEventEditor(
    modifier: Modifier,
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    allDay: Boolean,
    onToggleAllDay: () -> Unit,
    startTime: String,
    endTime: String,
    selectedCalendarName: String?,
    calendarCount: Int,
    saveMessage: String,
    saving: Boolean,
    onChooseCalendar: () -> Unit,
    onChooseStartTime: () -> Unit,
    onChooseEndTime: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("NEW AGENDA", color = colors.inkSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        CalendarTextField("TITLE", title, "Agenda title", true, onTitleChange)
        CalendarTextField("NOTES // OPTIONAL", notes, "Add details", false, onNotesChange)
        Text(if (allDay) "[✓] ALL DAY" else "[ ] ALL DAY", color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onToggleAllDay).padding(vertical = 3.dp))
        if (!allDay) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalendarEditorButton("START // $startTime", Modifier.weight(1f), onChooseStartTime)
                CalendarEditorButton("END // $endTime", Modifier.weight(1f), onChooseEndTime)
            }
        }
        if (selectedCalendarName != null) {
            CalendarEditorButton(
                label = if (calendarCount > 1) "CALENDAR // $selectedCalendarName  ↻" else "CALENDAR // $selectedCalendarName",
                modifier = Modifier.fillMaxWidth(),
                onClick = onChooseCalendar
            )
        } else {
            Text("No writable calendar was found. Add or enable a calendar account on this device.", color = colors.inkMuted, fontSize = 8.sp)
        }
        if (saveMessage.isNotBlank()) Text(saveMessage, color = colors.inkSecondary, fontSize = 8.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalendarEditorButton("CANCEL", Modifier.weight(1f), onCancel)
            CalendarEditorButton(if (saving) "SAVING…" else "SAVE AGENDA", Modifier.weight(1f), onSave, enabled = !saving && title.isNotBlank() && selectedCalendarName != null)
        }
    }
}

@Composable
private fun CalendarTextField(
    label: String,
    value: String,
    placeholder: String,
    singleLine: Boolean,
    onValueChange: (String) -> Unit
) {
    val colors = LocalRetroBitColors.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = colors.inkMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = colors.inkPrimary, fontSize = 9.sp),
            cursorBrush = SolidColor(colors.inkPrimary),
            modifier = Modifier.fillMaxWidth().heightIn(min = if (singleLine) 34.dp else 58.dp, max = if (singleLine) 34.dp else 78.dp)
                .background(colors.surface).border(1.dp, colors.border).padding(7.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = colors.inkMuted, fontSize = 9.sp)
                inner()
            }
        )
    }
}

@Composable
private fun CalendarEditorButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val colors = LocalRetroBitColors.current
    Box(
        modifier = modifier.background(if (enabled) colors.surface else colors.background)
            .border(1.dp, colors.border)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (enabled) colors.inkPrimary else colors.inkMuted, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

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
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY
        )
        context.contentResolver.query(builder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            buildList {
                val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val allDayColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleColumn).orEmpty().ifBlank { "(Untitled event)" }
                    add(CalendarEvent(title, cursor.getLong(beginColumn), cursor.getInt(allDayColumn) != 0))
                }
            }
        }.orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun queryWritableCalendars(context: Context): List<WritableCalendar> {
    return try {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            buildList {
                val idColumn = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    add(WritableCalendar(cursor.getLong(idColumn), cursor.getString(nameColumn).orEmpty().ifBlank { "Calendar" }))
                }
            }
        }.orEmpty()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun insertCalendarEvent(
    context: Context,
    calendarId: Long,
    title: String,
    notes: String,
    selectedDayMillis: Long,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    allDay: Boolean
): Boolean {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) return false
    return try {
        val day = Calendar.getInstance().apply { timeInMillis = selectedDayMillis }
        val start: Long
        val end: Long
        val timezone: String
        if (allDay) {
            val utcStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
                clear()
                set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            }
            start = utcStart.timeInMillis
            utcStart.add(Calendar.DAY_OF_MONTH, 1)
            end = utcStart.timeInMillis
            timezone = "UTC"
        } else {
            val localStart = Calendar.getInstance().apply {
                timeInMillis = selectedDayMillis
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val localEnd = Calendar.getInstance().apply {
                timeInMillis = selectedDayMillis
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= localStart.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
            }
            start = localStart.timeInMillis
            end = localEnd.timeInMillis
            timezone = TimeZone.getDefault().id
        }
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, notes)
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, timezone)
            put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
        }
        context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) != null
    } catch (_: Exception) {
        false
    }
}
