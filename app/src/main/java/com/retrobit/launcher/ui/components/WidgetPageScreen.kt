package com.retrobit.launcher.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.retrobit.launcher.data.LauncherFeatureStore
import com.retrobit.launcher.data.LauncherFeatureStore.WidgetPageModule
import com.retrobit.launcher.ui.theme.LocalRetroBitColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WidgetPageScreen(
    store: LauncherFeatureStore,
    modules: List<WidgetPageModule>,
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onToggleModule: (WidgetPageModule) -> Unit,
    onMoveModule: (WidgetPageModule, Int) -> Unit,
    onReturnHome: () -> Unit,
    onConfigureWeather: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRetroBitColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        item(key = "widget-page-header") {
            Column(Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(11.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("WIDGET DECK // PAGE 02", color = colors.inkPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (isEditing) "Edit mode // hold a blank area to close." else "Calendar, notes and tasks · swipe left for Home.",
                            color = colors.inkSecondary,
                            fontSize = 8.sp
                        )
                    }
                    if (isEditing) {
                        PageAction("DONE", onClick = onToggleEdit)
                    } else {
                        PageAction("← HOME", onClick = onReturnHome)
                    }
                }
            }
        }

        if (isEditing) {
            item(key = "widget-page-module-editor") {
                Column(Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(9.dp)) {
                    Text("PAGE MODULES // TAP TO SHOW OR HIDE", color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    WidgetPageModule.entries.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(bottom = 5.dp)) {
                            row.forEach { module ->
                                ModuleToggleChip(
                                    module = module,
                                    selected = module in modules,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onToggleModule(module) }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Text("Use ↑ ↓ on a section to change its order. Notes and tasks save on this device.", color = colors.inkMuted, fontSize = 7.sp)
                }
            }
        }

        if (modules.isEmpty()) {
            item(key = "widget-page-empty") {
                Column(
                    Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NO PAGE MODULES", color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Hold a blank area, then enable Calendar, Notes, To-do, Weather or Media.", color = colors.inkMuted, fontSize = 8.sp)
                }
            }
        } else {
            itemsIndexed(modules, key = { _, module -> "widget-page-${module.name}" }) { index, module ->
                val move: (Int) -> Unit = { offset -> onMoveModule(module, offset) }
                when (module) {
                    WidgetPageModule.CALENDAR -> CalendarPageModule(
                        position = index,
                        total = modules.size,
                        isEditing = isEditing,
                        onMove = move
                    )
                    WidgetPageModule.NOTES -> NotesPageModule(
                        store = store,
                        position = index,
                        total = modules.size,
                        isEditing = isEditing,
                        onMove = move
                    )
                    WidgetPageModule.TODO -> TodoPageModule(
                        store = store,
                        position = index,
                        total = modules.size,
                        isEditing = isEditing,
                        onMove = move
                    )
                    WidgetPageModule.WEATHER -> WidgetModuleCard(
                        title = module.label,
                        detail = "LIVE CONDITIONS",
                        position = index,
                        total = modules.size,
                        isEditing = isEditing,
                        onMove = move
                    ) {
                        WeatherWidget(store = store, onConfigure = onConfigureWeather)
                    }
                    WidgetPageModule.MEDIA -> WidgetModuleCard(
                        title = module.label,
                        detail = "MEDIA SESSION",
                        position = index,
                        total = modules.size,
                        isEditing = isEditing,
                        onMove = move
                    ) {
                        RetroMediaPlayerWidget()
                    }
                }
            }
        }

        item(key = "widget-page-footer") {
            Text(
                if (isEditing) "PAGE EDIT // hold empty space or press DONE to finish" else "SWIPE LEFT ← TO RETURN HOME · HOLD EMPTY SPACE TO EDIT PAGE",
                color = colors.inkMuted,
                fontSize = 7.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CalendarPageModule(
    position: Int,
    total: Int,
    isEditing: Boolean,
    onMove: (Int) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalRetroBitColors.current
    val scope = rememberCoroutineScope()
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var hasReadCalendarPermission by remember(context) {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    var hasWriteCalendarPermission by remember(context) {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var monthEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var eventRevision by remember { mutableIntStateOf(0) }
    var isEventEditorOpen by remember { mutableStateOf(false) }
    var eventTitle by remember { mutableStateOf("") }
    var eventNotes by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf(CalendarEntryType.EVENT) }
    var eventRecurrence by remember { mutableStateOf(CalendarRecurrence.NEVER) }
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
        eventType = CalendarEntryType.EVENT
        eventRecurrence = CalendarRecurrence.NEVER
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
        else permissionMessage = "Calendar read and write access are needed to add an agenda."
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
    LaunchedEffect(monthOffset, hasReadCalendarPermission, eventRevision) {
        monthEvents = if (hasReadCalendarPermission) withContext(Dispatchers.IO) {
            val monthStart = displayedMonth.timeInMillis
            val monthEnd = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
            queryEventsInRange(context, monthStart, monthEnd)
        } else emptyList()
    }
    LaunchedEffect(hasWriteCalendarPermission, hasReadCalendarPermission) {
        calendars = if (hasWriteCalendarPermission && hasReadCalendarPermission) withContext(Dispatchers.IO) {
            queryWritableCalendars(context)
        } else emptyList()
        if (calendars.none { it.id == selectedCalendarId }) selectedCalendarId = calendars.firstOrNull()?.id ?: -1L
    }

    fun moveMonth(offset: Int) {
        val target = (displayedMonth.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
        monthOffset += offset
        selectedDay = target.timeInMillis
        isEventEditorOpen = false
        eventSaveMessage = ""
    }

    fun requestAddAgenda() {
        permissionMessage = ""
        if (hasReadCalendarPermission && hasWriteCalendarPermission) {
            beginEventDraft()
        } else {
            val missing = buildList {
                if (!hasReadCalendarPermission) add(Manifest.permission.READ_CALENDAR)
                if (!hasWriteCalendarPermission) add(Manifest.permission.WRITE_CALENDAR)
            }
            writePermissionLauncher.launch(missing.toTypedArray())
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
            eventSaveMessage = "Calendar write access and a writable calendar are required."
            return
        }
        isSavingEvent = true
        eventSaveMessage = ""
        val calendarId = selectedCalendarId
        val agendaDayMillis = selectedDay
        val notes = eventNotes.trim()
        val agendaStartHour = startHour
        val agendaStartMinute = startMinute
        val agendaEndHour = endHour
        val agendaEndMinute = endMinute
        val isAllDay = eventIsAllDay
        val entryType = eventType
        val recurrence = eventRecurrence
        scope.launch {
            val created = withContext(Dispatchers.IO) {
                insertCalendarEvent(
                    context = context,
                    calendarId = calendarId,
                    title = title,
                    notes = notes,
                    selectedDayMillis = agendaDayMillis,
                    startHour = agendaStartHour,
                    startMinute = agendaStartMinute,
                    endHour = agendaEndHour,
                    endMinute = agendaEndMinute,
                    allDay = isAllDay,
                    entryType = entryType,
                    recurrence = recurrence
                )
            }
            isSavingEvent = false
            if (created) {
                isEventEditorOpen = false
                eventRevision++
                permissionMessage = "Agenda saved to your Android calendar."
            } else {
                eventSaveMessage = "Could not save the agenda. Check calendar access and try again."
            }
        }
    }

    WidgetModuleCard(
        title = "CALENDAR",
        detail = "MONDAY-FIRST · TAP DATE FOR AGENDA",
        position = position,
        total = total,
        isEditing = isEditing,
        onMove = onMove
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            PageAction("◀", onClick = { moveMonth(-1) })
            Text(monthLabel, color = colors.inkPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            PageAction("▶", onClick = { moveMonth(1) })
        }

        CalendarMonthGrid(
            displayedMonth = displayedMonth,
            selectedDayMillis = selectedDay,
            events = monthEvents,
            dayCellHeight = 31.dp,
            onDateSelected = { dayMillis ->
                selectedDay = dayMillis
                isEventEditorOpen = false
                eventSaveMessage = ""
                permissionMessage = ""
            }
        )
        Text("TAP A DATE TO VIEW ITS AGENDA · • = DAYS WITH EVENTS", color = colors.inkMuted, fontSize = 7.sp)

        Spacer(Modifier.height(5.dp))
        val selectedLabel = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(selectedDay))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("AGENDA // ${selectedLabel.uppercase()}", color = colors.inkPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            PageAction("+ ADD", onClick = ::requestAddAgenda)
        }
        if (permissionMessage.isNotBlank()) {
            Text(permissionMessage, color = colors.inkSecondary, fontSize = 7.sp, modifier = Modifier.padding(vertical = 2.dp))
        }
        if (isEventEditorOpen) {
            CalendarEventEditor(
                modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                title = eventTitle,
                onTitleChange = { eventTitle = it },
                notes = eventNotes,
                onNotesChange = { eventNotes = it },
                entryType = eventType,
                onEntryTypeChange = { type ->
                    eventType = type
                    eventIsAllDay = type != CalendarEntryType.EVENT
                    eventRecurrence = if (type == CalendarEntryType.BIRTHDAY) CalendarRecurrence.YEARLY else CalendarRecurrence.NEVER
                },
                recurrence = eventRecurrence,
                onRecurrenceChange = { eventRecurrence = it },
                allDay = eventIsAllDay,
                onToggleAllDay = { eventIsAllDay = !eventIsAllDay },
                startTime = formatCalendarTime(startHour, startMinute),
                endTime = formatCalendarTime(endHour, endMinute),
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
        } else if (hasReadCalendarPermission) {
            if (events.isEmpty()) {
                Text("No events for this day.", color = colors.inkMuted, fontSize = 8.sp, modifier = Modifier.padding(top = 3.dp))
            } else {
                events.forEach { event ->
                    val time = if (event.allDay) "ALL DAY" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.begin))
                    Text("$time  ${event.title}", color = colors.inkSecondary, fontSize = 8.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        } else {
            Text(
                "Calendar access is optional. Grant read access to show events; add agenda asks for separate approval.",
                color = colors.inkMuted,
                fontSize = 7.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
            PageAction("GRANT READ ACCESS", onClick = { readPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) })
        }
    }
}

@Composable
private fun NotesPageModule(
    store: LauncherFeatureStore,
    position: Int,
    total: Int,
    isEditing: Boolean,
    onMove: (Int) -> Unit
) {
    val colors = LocalRetroBitColors.current
    var note by remember(store) { mutableStateOf(store.widgetPageNote) }
    WidgetModuleCard("NOTES", "AUTO-SAVED ON THIS DEVICE", position, total, isEditing, onMove) {
        BasicTextField(
            value = note,
            onValueChange = {
                note = it
                store.widgetPageNote = it
            },
            textStyle = TextStyle(color = colors.inkPrimary, fontSize = 10.sp),
            cursorBrush = SolidColor(colors.inkPrimary),
            modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp, max = 210.dp)
                .verticalScroll(rememberScrollState())
                .background(colors.background).border(1.dp, colors.border).padding(8.dp),
            decorationBox = { inner ->
                if (note.isEmpty()) Text("Write a note, reminder or idea…", color = colors.inkMuted, fontSize = 9.sp)
                inner()
            }
        )
    }
}

@Composable
private fun TodoPageModule(
    store: LauncherFeatureStore,
    position: Int,
    total: Int,
    isEditing: Boolean,
    onMove: (Int) -> Unit
) {
    val colors = LocalRetroBitColors.current
    var items by remember(store) { mutableStateOf(store.getTodoItems()) }
    var draft by remember { mutableStateOf("") }

    fun addTask() {
        if (draft.isBlank()) return
        store.addTodo(draft)
        items = store.getTodoItems()
        draft = ""
    }

    WidgetModuleCard("TO-DO", "${items.count { !it.done }} OPEN", position, total, isEditing, onMove) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.inkPrimary, fontSize = 9.sp),
                cursorBrush = SolidColor(colors.inkPrimary),
                modifier = Modifier.weight(1f).background(colors.background).border(1.dp, colors.border).padding(8.dp),
                decorationBox = { inner ->
                    if (draft.isEmpty()) Text("Add a task…", color = colors.inkMuted, fontSize = 8.sp)
                    inner()
                }
            )
            Spacer(Modifier.width(5.dp))
            PageAction("+ ADD", onClick = ::addTask)
        }
        if (items.isEmpty()) {
            Text("Your task list is empty.", color = colors.inkMuted, fontSize = 8.sp, modifier = Modifier.padding(top = 6.dp))
        } else {
            Spacer(Modifier.height(5.dp))
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp).background(colors.background).border(1.dp, colors.border)
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (item.done) "[✓]" else "[ ]",
                        color = colors.inkPrimary,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable {
                            store.toggleTodo(item.id)
                            items = store.getTodoItems()
                        }.padding(end = 7.dp)
                    )
                    Text(
                        item.title,
                        color = if (item.done) colors.inkMuted else colors.inkPrimary,
                        textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                        fontSize = 8.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "×",
                        color = colors.inkSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            store.removeTodo(item.id)
                            items = store.getTodoItems()
                        }.padding(start = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetModuleCard(
    title: String,
    detail: String,
    position: Int,
    total: Int,
    isEditing: Boolean,
    onMove: (Int) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalRetroBitColors.current
    Column(Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border).padding(9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                if (detail.isNotBlank()) Text(detail, color = colors.inkMuted, fontSize = 7.sp)
            }
            if (isEditing) {
                PageAction("↑", onClick = { onMove(-1) }, enabled = position > 0)
                Spacer(Modifier.width(3.dp))
                PageAction("↓", onClick = { onMove(1) }, enabled = position < total - 1)
            }
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun ModuleToggleChip(
    module: WidgetPageModule,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = LocalRetroBitColors.current
    Text(
        text = "${if (selected) "✓" else "+"}  ${module.label}",
        color = if (selected) colors.background else colors.inkPrimary,
        fontSize = 7.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(if (selected) colors.inkPrimary else colors.background)
            .border(1.dp, colors.border)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 7.dp)
    )
}

@Composable
private fun PageAction(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = LocalRetroBitColors.current
    Text(
        label,
        color = if (enabled) colors.inkPrimary else colors.inkMuted,
        fontSize = 7.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.border(1.dp, if (enabled) colors.border else colors.inkMuted)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp)
    )
}

private fun formatCalendarTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
