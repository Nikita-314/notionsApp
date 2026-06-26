package com.nikita.notionsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikita.notionsapp.data.JournalSnapshot
import com.nikita.notionsapp.domain.AppUserPreferences
import com.nikita.notionsapp.domain.ChecklistItem
import com.nikita.notionsapp.domain.DayCompletedListMode
import com.nikita.notionsapp.domain.DayListDensity
import com.nikita.notionsapp.domain.JournalBook
import com.nikita.notionsapp.domain.JournalEntry
import com.nikita.notionsapp.domain.JournalEntryVisualStatus
import com.nikita.notionsapp.domain.ReminderRule
import com.nikita.notionsapp.domain.ReminderRuleKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

private val Paper = Color(0xFFFAF7F0)
private val Ink = Color(0xFF27231D)
private val InkSoft = Color(0xFF716B61)
private val Line = Color(0xFFE7DED0)
private val CardPaper = Color(0xFFFFFCF6)
private val Accent = Color(0xFF3C7D66)
private val AccentSoft = Color(0xFFDDEDE5)
private val Gold = Color(0xFFB88336)
private val Focus = Color(0xFF8D5CD6)
private val Blocked = Color(0xFFC4504D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotionsTheme {
                JournalApp()
            }
        }
    }
}

@Composable
private fun NotionsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            secondary = Gold,
            surface = CardPaper,
            background = Paper,
            onPrimary = Color.White,
            onSurface = Ink,
            onBackground = Ink,
            outline = Line,
            error = Blocked
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
            content()
        }
    }
}

@Composable
private fun JournalApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember {
        AndroidNotionsStore(context.filesDir.resolve("notions-journal.json"))
    }
    val loaded = remember { store.load() ?: demoJournalSnapshot() }
    val books = remember { mutableStateListOf<JournalBook>().apply { addAll(loaded.books) } }
    val entries = remember { mutableStateListOf<JournalEntry>().apply { addAll(loaded.entries) } }
    val checklist = remember { mutableStateListOf<ChecklistItem>().apply { addAll(loaded.checklistItems) } }
    val reminders = remember { mutableStateListOf<ReminderRule>().apply { addAll(loaded.reminderRules) } }
    var preferences by remember { mutableStateOf(loaded.preferences) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var tab by remember { mutableStateOf(AppTab.Day) }
    var editorEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var showEntryEditor by remember { mutableStateOf(false) }
    var showBookEditor by remember { mutableStateOf(false) }

    fun save() {
        store.save(
            JournalSnapshot(
                books = books.toList(),
                entries = entries.toList(),
                checklistItems = checklist.toList(),
                reminderRules = reminders.toList(),
                preferences = preferences,
                pendingSyncVersion = loaded.pendingSyncVersion + 1,
                lastSyncedVersion = loaded.lastSyncedVersion
            )
        )
    }

    LaunchedEffect(books.size, entries.size, checklist.size, reminders.size, preferences) {
        save()
    }

    Scaffold(
        floatingActionButton = {
            if (tab == AppTab.Day) {
                FloatingActionButton(
                    onClick = {
                        editorEntry = null
                        showEntryEditor = true
                    },
                    containerColor = Accent,
                    contentColor = Color.White
                ) {
                    Text("+", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = CardPaper) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.icon) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        containerColor = Paper
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                AppTab.Day -> DayScreen(
                    day = selectedDay,
                    books = books,
                    entries = entries,
                    checklist = checklist,
                    reminders = reminders,
                    preferences = preferences,
                    onPreviousDay = { selectedDay = selectedDay.minusDays(1) },
                    onNextDay = { selectedDay = selectedDay.plusDays(1) },
                    onToday = { selectedDay = LocalDate.now() },
                    onToggle = { entry ->
                        val index = entries.indexOfFirst { it.id == entry.id }
                        if (index != -1) {
                            entries[index] = entry.copy(
                                isCompleted = !entry.isCompleted,
                                completedAt = if (!entry.isCompleted) LocalDateTime.now().toString() else null,
                                updatedAt = LocalDateTime.now().toString()
                            )
                            save()
                        }
                    },
                    onToggleChecklist = { item ->
                        val index = checklist.indexOfFirst { it.id == item.id }
                        if (index != -1) {
                            checklist[index] = item.copy(isCompleted = !item.isCompleted)
                            save()
                        }
                    },
                    onEdit = {
                        editorEntry = it
                        showEntryEditor = true
                    },
                    onDelete = { entry ->
                        entries.removeAll { it.id == entry.id }
                        checklist.removeAll { it.entryId == entry.id }
                        reminders.removeAll { it.entryId == entry.id }
                        save()
                    }
                )
                AppTab.Calendar -> CalendarScreen(
                    selectedDay = selectedDay,
                    entries = entries,
                    onSelectDay = {
                        selectedDay = it
                        tab = AppTab.Day
                    }
                )
                AppTab.Books -> BooksScreen(
                    books = books,
                    entries = entries,
                    onAddBook = { showBookEditor = true },
                    onToggleArchive = { book ->
                        val index = books.indexOfFirst { it.id == book.id }
                        if (index != -1) {
                            books[index] = book.copy(isArchived = !book.isArchived, updatedAt = LocalDateTime.now().toString())
                            save()
                        }
                    }
                )
                AppTab.Settings -> SettingsScreen(
                    preferences = preferences,
                    onChange = {
                        preferences = it
                        save()
                    },
                    storageName = "notions-journal.json"
                )
            }
        }
    }

    if (showEntryEditor) {
        EntryEditorSheet(
            day = selectedDay,
            entry = editorEntry,
            books = books.filterNot { it.isArchived },
            checklist = checklist,
            reminders = reminders,
            onDismiss = { showEntryEditor = false },
            onSave = { savedEntry, checklistTitles, reminderAt ->
                val now = LocalDateTime.now().toString()
                val existingIndex = entries.indexOfFirst { it.id == savedEntry.id }
                val normalized = savedEntry.copy(updatedAt = now)
                if (existingIndex == -1) {
                    entries.add(normalized)
                } else {
                    entries[existingIndex] = normalized
                    checklist.removeAll { it.entryId == normalized.id }
                    reminders.removeAll { it.entryId == normalized.id }
                }
                checklistTitles.filter { it.isNotBlank() }.forEachIndexed { index, title ->
                    checklist.add(
                        ChecklistItem(
                            id = "check-${UUID.randomUUID()}",
                            entryId = normalized.id,
                            title = title.trim(),
                            manualPosition = index
                        )
                    )
                }
                if (reminderAt.isNotBlank()) {
                    reminders.add(
                        ReminderRule(
                            id = "reminder-${UUID.randomUUID()}",
                            entryId = normalized.id,
                            kind = ReminderRuleKind.DATE_TIME,
                            triggerAt = reminderAt.trim()
                        )
                    )
                }
                save()
                showEntryEditor = false
            }
        )
    }

    if (showBookEditor) {
        BookEditorDialog(
            nextPosition = books.size,
            onDismiss = { showBookEditor = false },
            onSave = {
                books.add(it)
                save()
                showBookEditor = false
            }
        )
    }
}

@Composable
private fun DayScreen(
    day: LocalDate,
    books: List<JournalBook>,
    entries: List<JournalEntry>,
    checklist: List<ChecklistItem>,
    reminders: List<ReminderRule>,
    preferences: AppUserPreferences,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onToggle: (JournalEntry) -> Unit,
    onToggleChecklist: (ChecklistItem) -> Unit,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit
) {
    val dayKey = day.toString()
    val visibleEntries = entries
        .filter { it.dayDate == dayKey }
        .sortedWith(compareBy<JournalEntry> { it.timeOfDayMinutes ?: Int.MAX_VALUE }.thenBy { it.manualPosition })
    val activeEntries = visibleEntries.filterNot { it.isCompleted }
    val completedEntries = visibleEntries.filter { it.isCompleted }
    val bookMap = books.associateBy { it.id }
    val densityPadding = if (preferences.listDensity == DayListDensity.COMPACT) 8.dp else 12.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DayHeader(day, activeEntries.size, completedEntries.size, onPreviousDay, onNextDay, onToday)
        }
        if (visibleEntries.isEmpty()) {
            item { EmptyDayCard() }
        }
        items(activeEntries, key = { it.id }) { entry ->
            EntryCard(
                entry = entry,
                book = bookMap[entry.bookId],
                checklist = checklist.filter { it.entryId == entry.id },
                reminders = reminders.filter { it.entryId == entry.id },
                showBookLabels = preferences.showBookLabels,
                padding = densityPadding,
                onToggle = { onToggle(entry) },
                onToggleChecklist = onToggleChecklist,
                onEdit = { onEdit(entry) },
                onDelete = { onDelete(entry) }
            )
        }
        if (completedEntries.isNotEmpty() && preferences.completedListMode != DayCompletedListMode.HIDDEN) {
            item {
                SectionLabel("Completed")
            }
            if (preferences.completedListMode == DayCompletedListMode.INLINE) {
                items(completedEntries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        book = bookMap[entry.bookId],
                        checklist = checklist.filter { it.entryId == entry.id },
                        reminders = reminders.filter { it.entryId == entry.id },
                        showBookLabels = preferences.showBookLabels,
                        padding = densityPadding,
                        onToggle = { onToggle(entry) },
                        onToggleChecklist = onToggleChecklist,
                        onEdit = { onEdit(entry) },
                        onDelete = { onDelete(entry) }
                    )
                }
            } else {
                item {
                    Text(
                        "${completedEntries.size} done today",
                        color = InkSoft,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    day: LocalDate,
    activeCount: Int,
    completedCount: Int,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardPaper),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        day.format(DateTimeFormatter.ofPattern("EEEE")),
                        color = InkSoft,
                        fontSize = 14.sp
                    )
                    Text(
                        day.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                        color = Ink,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(onClick = onToday) { Text("Today") }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreviousDay, modifier = Modifier.weight(1f)) { Text("Prev") }
                OutlinedButton(onClick = onNextDay, modifier = Modifier.weight(1f)) { Text("Next") }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("$activeCount active", AccentSoft, Accent)
                StatusPill("$completedCount done", Color(0xFFEDE7F8), Focus)
            }
        }
    }
}

@Composable
private fun EntryCard(
    entry: JournalEntry,
    book: JournalBook?,
    checklist: List<ChecklistItem>,
    reminders: List<ReminderRule>,
    showBookLabels: Boolean,
    padding: androidx.compose.ui.unit.Dp,
    onToggle: () -> Unit,
    onToggleChecklist: (ChecklistItem) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = entry.colorOverrideArgb?.let { Color(it.toInt()) } ?: book?.let { Color(it.colorArgb.toInt()) } ?: Accent
    val statusColor = when (entry.visualStatus) {
        JournalEntryVisualStatus.NEUTRAL -> accent
        JournalEntryVisualStatus.FOCUS -> Focus
        JournalEntryVisualStatus.BLOCKED -> Blocked
        JournalEntryVisualStatus.DONE -> Accent
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardPaper),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(padding)) {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = entry.isCompleted, onCheckedChange = { onToggle() })
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(entry.title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 18.sp)
                    }
                    val entryNotes = entry.notes
                    if (entryNotes?.isNotBlank() == true) {
                        Spacer(Modifier.height(4.dp))
                        Text(entryNotes, color = InkSoft)
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val entryTime = entry.timeOfDayMinutes
                        if (entryTime != null) {
                            StatusPill(minutesLabel(entryTime), Color(0xFFFFF1DA), Gold)
                        }
                        if (showBookLabels && book != null) {
                            StatusPill(book.title, AccentSoft, Accent)
                        }
                        reminders.firstOrNull()?.triggerAt?.let {
                            StatusPill("Reminder $it", Color(0xFFEDE7F8), Focus)
                        }
                    }
                    checklist.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.isCompleted, onCheckedChange = { onToggleChecklist(item) })
                            Text(item.title, color = if (item.isCompleted) InkSoft else Ink)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete", color = Blocked) }
            }
        }
    }
}

@Composable
private fun CalendarScreen(
    selectedDay: LocalDate,
    entries: List<JournalEntry>,
    onSelectDay: (LocalDate) -> Unit
) {
    var month by remember(selectedDay) { mutableStateOf(YearMonth.from(selectedDay)) }
    val first = month.atDay(1)
    val blanks = first.dayOfWeek.value % 7
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val counts = entries.groupBy { LocalDate.parse(it.dayDate) }.mapValues { it.value.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardPaper), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = { month = month.minusMonths(1) }) { Text("Prev") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { month = month.plusMonths(1) }) { Text("Next") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                                Text(it, color = InkSoft, modifier = Modifier.weight(1f).padding(6.dp))
                            }
                        }
                        val cells = List(blanks) { null } + days.map { it }
                        cells.chunked(7).forEach { week ->
                            Row {
                                week.forEach { day ->
                                    if (day == null) {
                                        Spacer(Modifier.weight(1f).height(58.dp))
                                    } else {
                                        val isSelected = day == selectedDay
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .padding(3.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) AccentSoft else Color.Transparent)
                                                .border(1.dp, if (isSelected) Accent else Line, RoundedCornerShape(8.dp))
                                                .clickable { onSelectDay(day) }
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(day.dayOfMonth.toString(), fontWeight = FontWeight.SemiBold)
                                            val count = counts[day] ?: 0
                                            if (count > 0) {
                                                Text("$count", color = Accent, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                                repeat(7 - week.size) {
                                    Spacer(Modifier.weight(1f).height(58.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BooksScreen(
    books: List<JournalBook>,
    entries: List<JournalEntry>,
    onAddBook: () -> Unit,
    onToggleArchive: (JournalBook) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Books", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = onAddBook) { Text("Add") }
            }
        }
        items(books, key = { it.id }) { book ->
            Card(colors = CardDefaults.cardColors(containerColor = CardPaper), shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(book.colorArgb.toInt()))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("${entries.count { it.bookId == book.id }} entries", color = InkSoft)
                    }
                    OutlinedButton(onClick = { onToggleArchive(book) }) {
                        Text(if (book.isArchived) "Restore" else "Archive")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    preferences: AppUserPreferences,
    onChange: (AppUserPreferences) -> Unit,
    storageName: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item {
            SettingsCard("Compact list") {
                Switch(
                    checked = preferences.listDensity == DayListDensity.COMPACT,
                    onCheckedChange = {
                        onChange(preferences.copy(listDensity = if (it) DayListDensity.COMPACT else DayListDensity.COMFORTABLE))
                    }
                )
            }
        }
        item {
            SettingsCard("Show book labels") {
                Switch(
                    checked = preferences.showBookLabels,
                    onCheckedChange = { onChange(preferences.copy(showBookLabels = it)) }
                )
            }
        }
        item {
            SettingsCard("Collapse completed") {
                Switch(
                    checked = preferences.completedListMode == DayCompletedListMode.COLLAPSED,
                    onCheckedChange = {
                        onChange(preferences.copy(completedListMode = if (it) DayCompletedListMode.COLLAPSED else DayCompletedListMode.INLINE))
                    }
                )
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardPaper), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Local-first storage", fontWeight = FontWeight.SemiBold)
                    Text(storageName, color = InkSoft)
                    Text("Data is encrypted with Android Keystore before it is written to app files.", color = InkSoft)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, control: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardPaper), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            control()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditorSheet(
    day: LocalDate,
    entry: JournalEntry?,
    books: List<JournalBook>,
    checklist: List<ChecklistItem>,
    reminders: List<ReminderRule>,
    onDismiss: () -> Unit,
    onSave: (JournalEntry, List<String>, String) -> Unit
) {
    val now = remember { LocalDateTime.now().toString() }
    var title by remember(entry) { mutableStateOf(entry?.title.orEmpty()) }
    var notes by remember(entry) { mutableStateOf(entry?.notes.orEmpty()) }
    var bookId by remember(entry, books) { mutableStateOf(entry?.bookId ?: books.firstOrNull()?.id.orEmpty()) }
    var time by remember(entry) { mutableStateOf(entry?.timeOfDayMinutes?.let(::minutesLabel).orEmpty()) }
    var statusIndex by remember(entry) { mutableStateOf(JournalEntryVisualStatus.entries.indexOf(entry?.visualStatus ?: JournalEntryVisualStatus.NEUTRAL)) }
    var checklistText by remember(entry) {
        mutableStateOf(checklist.filter { it.entryId == entry?.id }.joinToString("\n") { it.title })
    }
    var reminderAt by remember(entry) {
        mutableStateOf(reminders.firstOrNull { it.entryId == entry?.id }?.triggerAt.orEmpty())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardPaper) {
        LazyColumn(
            contentPadding = PaddingValues(18.dp, 4.dp, 18.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(if (entry == null) "New entry" else "Edit entry", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
            item {
                Column {
                    Text("Book", color = InkSoft)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        books.forEach { book ->
                            val selected = book.id == bookId
                            OutlinedButton(
                                onClick = { bookId = book.id },
                                modifier = Modifier.background(if (selected) AccentSoft else Color.Transparent, RoundedCornerShape(8.dp))
                            ) {
                                Text(book.title)
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time, HH:mm") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                TabRow(selectedTabIndex = statusIndex) {
                    JournalEntryVisualStatus.entries.forEachIndexed { index, status ->
                        Tab(
                            selected = statusIndex == index,
                            onClick = { statusIndex = index },
                            text = { Text(status.name.lowercase().replaceFirstChar { it.titlecase() }) }
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = checklistText,
                    onValueChange = { checklistText = it },
                    label = { Text("Checklist, one item per line") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
            item {
                OutlinedTextField(
                    value = reminderAt,
                    onValueChange = { reminderAt = it },
                    label = { Text("Reminder, e.g. 2026-06-26T18:30") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && bookId.isNotBlank()) {
                                onSave(
                                    JournalEntry(
                                        id = entry?.id ?: "entry-${UUID.randomUUID()}",
                                        bookId = bookId,
                                        dayDate = entry?.dayDate ?: day.toString(),
                                        title = title.trim(),
                                        notes = notes.trim().ifBlank { null },
                                        manualPosition = entry?.manualPosition ?: 0,
                                        timeOfDayMinutes = parseTimeMinutes(time),
                                        isCompleted = entry?.isCompleted ?: false,
                                        completedAt = entry?.completedAt,
                                        visualStatus = JournalEntryVisualStatus.entries[statusIndex],
                                        createdAt = entry?.createdAt ?: now,
                                        updatedAt = now
                                    ),
                                    checklistText.lines(),
                                    reminderAt
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun BookEditorDialog(
    nextPosition: Int,
    onDismiss: () -> Unit,
    onSave: (JournalBook) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val colors = listOf(0xFF3C7D66L, 0xFFB88336L, 0xFF8D5CD6L, 0xFFC4504DL, 0xFF2E6E9EL)
    var selectedColor by remember { mutableStateOf(colors.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New book") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { value ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(value.toInt()))
                                .border(3.dp, if (selectedColor == value) Ink else Color.Transparent, CircleShape)
                                .clickable { selectedColor = value }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    val now = LocalDateTime.now().toString()
                    onSave(
                        JournalBook(
                            id = "book-${UUID.randomUUID()}",
                            title = title.trim(),
                            colorArgb = selectedColor,
                            manualPosition = nextPosition,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmptyDayCard() {
    Card(colors = CardDefaults.cardColors(containerColor = CardPaper), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("A clean page", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Add the first thought, task, reminder, or tiny plan for this day.", color = InkSoft)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = InkSoft,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun StatusPill(text: String, background: Color, foreground: Color) {
    Text(
        text = text,
        color = foreground,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

private fun demoJournalSnapshot(): JournalSnapshot {
    val today = LocalDate.now()
    val now = LocalDateTime.now().toString()
    val life = JournalBook(
        id = "book-life",
        title = "Life",
        colorArgb = 0xFF3C7D66,
        manualPosition = 0,
        createdAt = now,
        updatedAt = now
    )
    val work = JournalBook(
        id = "book-work",
        title = "Work",
        colorArgb = 0xFF8D5CD6,
        manualPosition = 1,
        createdAt = now,
        updatedAt = now
    )
    val morning = JournalEntry(
        id = "entry-morning",
        bookId = life.id,
        dayDate = today.toString(),
        title = "Plan the day",
        notes = "Pick three important things and keep the page light.",
        manualPosition = 0,
        timeOfDayMinutes = 9 * 60,
        visualStatus = JournalEntryVisualStatus.FOCUS,
        createdAt = now,
        updatedAt = now
    )
    val later = JournalEntry(
        id = "entry-later",
        bookId = work.id,
        dayDate = today.toString(),
        title = "Review Kotlin port",
        notes = "Check screens, local save, and the first planner flow.",
        manualPosition = 1,
        timeOfDayMinutes = 16 * 60 + 30,
        createdAt = now,
        updatedAt = now
    )
    return JournalSnapshot(
        books = listOf(life, work),
        entries = listOf(morning, later),
        checklistItems = listOf(
            ChecklistItem("check-demo-1", morning.id, "Write the first entry"),
            ChecklistItem("check-demo-2", morning.id, "Set one reminder")
        ),
        reminderRules = listOf(
            ReminderRule("reminder-demo", later.id, ReminderRuleKind.DATE_TIME, "${today}T16:30")
        )
    )
}

private fun minutesLabel(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return "%02d:%02d".format(hours, mins)
}

private fun parseTimeMinutes(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}

private enum class AppTab(val label: String, val icon: String) {
    Day("Day", "D"),
    Calendar("Calendar", "C"),
    Books("Books", "B"),
    Settings("Settings", "S")
}
