package com.nikita.notionsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.Locale
import java.util.UUID

private val PaperBackground = Color(0xFFF6F3EE)
private val PaperSurface = Color(0xFFFFFBF7)
private val PaperMuted = Color(0xFFEDE8E0)
private val InkPrimary = Color(0xFF1E1E1E)
private val InkSecondary = Color(0xFF6B6560)
private val InkTertiary = Color(0xFF9A948C)
private val AccentLine = Color(0xFFC9B8A6)
private val AccentSoft = Color(0xFF3D4A5C)
private val SuccessSoft = Color(0xFF4A6B55)
private val Danger = Color(0xFFC23A3A)
private val Shadow = Color(0x22000000)

private val Russian = Locale.forLanguageTag("ru-RU")
private val DayTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM y 'г'.", Russian)
private val DaySubtitleFormatter = DateTimeFormatter.ofPattern("d MMMM", Russian)
private val MonthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Russian)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotionsTheme {
                NotionsApp()
            }
        }
    }
}

@Composable
private fun NotionsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AccentSoft,
            secondary = AccentLine,
            background = PaperBackground,
            surface = PaperSurface,
            onBackground = InkPrimary,
            onSurface = InkPrimary,
            onPrimary = Color.White,
            outline = PaperMuted,
            error = Danger
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = PaperBackground) {
            content()
        }
    }
}

@Composable
private fun NotionsApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AndroidNotionsStore(context.filesDir.resolve("notions-journal.json")) }
    val initial = remember { store.load() ?: demoJournalSnapshot() }
    val books = remember { mutableStateListOf<JournalBook>().apply { addAll(initial.books) } }
    val entries = remember { mutableStateListOf<JournalEntry>().apply { addAll(initial.entries) } }
    val checklist = remember { mutableStateListOf<ChecklistItem>().apply { addAll(initial.checklistItems) } }
    val reminders = remember { mutableStateListOf<ReminderRule>().apply { addAll(initial.reminderRules) } }
    val expanded = remember { mutableStateListOf<String>() }

    var preferences by remember { mutableStateOf(initial.preferences) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var screen by remember { mutableStateOf(AppScreen.Day) }
    var editedEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var showEntryEditor by remember { mutableStateOf(false) }
    var showBookEditor by remember { mutableStateOf(false) }

    fun snapshot() = JournalSnapshot(
        books = books.toList(),
        entries = entries.toList(),
        checklistItems = checklist.toList(),
        reminderRules = reminders.toList(),
        preferences = preferences,
        pendingSyncVersion = initial.pendingSyncVersion + 1,
        lastSyncedVersion = initial.lastSyncedVersion
    )

    fun save() {
        store.save(snapshot())
    }

    LaunchedEffect(books.size, entries.size, checklist.size, reminders.size, preferences) {
        save()
    }

    when (screen) {
        AppScreen.Day -> DayJournalScreen(
            day = selectedDay,
            books = books,
            entries = entries,
            checklist = checklist,
            reminders = reminders,
            preferences = preferences,
            expandedIds = expanded,
            onPreviousDay = { selectedDay = selectedDay.minusDays(1) },
            onNextDay = { selectedDay = selectedDay.plusDays(1) },
            onOpenCalendar = { screen = AppScreen.Calendar },
            onOpenSettings = { screen = AppScreen.Settings },
            onAdd = {
                editedEntry = null
                showEntryEditor = true
            },
            onToggleExpanded = { id ->
                if (expanded.contains(id)) expanded.remove(id) else expanded.add(id)
            },
            onToggleEntry = { entry ->
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
                editedEntry = it
                showEntryEditor = true
            },
            onDelete = { entry ->
                entries.removeAll { it.id == entry.id }
                checklist.removeAll { it.entryId == entry.id }
                reminders.removeAll { it.entryId == entry.id }
                expanded.remove(entry.id)
                save()
            }
        )
        AppScreen.Calendar -> CalendarScreen(
            selectedDay = selectedDay,
            entries = entries,
            books = books,
            onBack = { screen = AppScreen.Day },
            onOpenBooks = { screen = AppScreen.Books },
            onSelectDay = {
                selectedDay = it
                screen = AppScreen.Day
            }
        )
        AppScreen.Books -> BooksScreen(
            books = books,
            entries = entries,
            onBack = { screen = AppScreen.Calendar },
            onAddBook = { showBookEditor = true },
            onToggleArchive = { book ->
                val index = books.indexOfFirst { it.id == book.id }
                if (index != -1) {
                    books[index] = book.copy(isArchived = !book.isArchived, updatedAt = LocalDateTime.now().toString())
                    save()
                }
            }
        )
        AppScreen.Settings -> SettingsScreen(
            preferences = preferences,
            onBack = { screen = AppScreen.Day },
            onChange = {
                preferences = it
                save()
            }
        )
    }

    if (showEntryEditor) {
        EntryEditorDialog(
            day = selectedDay,
            entry = editedEntry,
            books = books.filterNot { it.isArchived },
            checklist = checklist,
            reminders = reminders,
            onDismiss = { showEntryEditor = false },
            onSave = { saved, checklistLines, reminderAt ->
                val now = LocalDateTime.now().toString()
                val normalized = saved.copy(updatedAt = now)
                val entryIndex = entries.indexOfFirst { it.id == normalized.id }
                if (entryIndex == -1) entries.add(normalized) else entries[entryIndex] = normalized

                checklist.removeAll { it.entryId == normalized.id }
                checklistLines.filter { it.isNotBlank() }.forEachIndexed { index, title ->
                    checklist.add(
                        ChecklistItem(
                            id = "check-${UUID.randomUUID()}",
                            entryId = normalized.id,
                            title = title.trim(),
                            manualPosition = index
                        )
                    )
                }

                reminders.removeAll { it.entryId == normalized.id }
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
            position = books.size,
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
private fun DayJournalScreen(
    day: LocalDate,
    books: List<JournalBook>,
    entries: List<JournalEntry>,
    checklist: List<ChecklistItem>,
    reminders: List<ReminderRule>,
    preferences: AppUserPreferences,
    expandedIds: List<String>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onAdd: () -> Unit,
    onToggleExpanded: (String) -> Unit,
    onToggleEntry: (JournalEntry) -> Unit,
    onToggleChecklist: (ChecklistItem) -> Unit,
    onEdit: (JournalEntry) -> Unit,
    onDelete: (JournalEntry) -> Unit
) {
    val dayEntries = entries
        .filter { it.dayDate == day.toString() }
        .sortedWith(compareBy<JournalEntry> { it.timeOfDayMinutes ?: Int.MAX_VALUE }.thenBy { it.manualPosition })
    val active = dayEntries.filterNot { it.isCompleted }
    val completed = dayEntries.filter { it.isCompleted }
    val bookMap = books.associateBy { it.id }
    val visibleCompleted = preferences.completedListMode != DayCompletedListMode.HIDDEN

    Scaffold(
        containerColor = PaperBackground,
        floatingActionButton = {
            TextButton(
                onClick = onAdd,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(AccentSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentSoft.copy(alpha = 0.065f), PaperBackground, PaperBackground),
                        startY = 0f,
                        endY = 520f
                    )
                )
                .padding(padding),
            contentPadding = PaddingValues(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 120.dp)
        ) {
            item {
                DayHeader(day, onOpenCalendar, onOpenSettings)
            }
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuietButton("←") { onPreviousDay() }
                    QuietButton("Сегодня") { }
                    QuietButton("→") { onNextDay() }
                }
            }
            item {
                JournalPageCard {
                    if (dayEntries.isEmpty()) {
                        EmptyDayPlaceholder()
                    } else {
                        Column {
                            SectionLabel(sectionLabel(day))
                            Spacer(Modifier.height(10.dp))
                            active.forEach { entry ->
                                DayEntryTile(
                                    entry = entry,
                                    book = bookMap[entry.bookId],
                                    checklist = checklist.filter { it.entryId == entry.id },
                                    reminders = reminders.filter { it.entryId == entry.id },
                                    expanded = expandedIds.contains(entry.id),
                                    preferences = preferences,
                                    onToggleExpanded = { onToggleExpanded(entry.id) },
                                    onToggle = { onToggleEntry(entry) },
                                    onToggleChecklist = onToggleChecklist,
                                    onEdit = { onEdit(entry) },
                                    onDelete = { onDelete(entry) }
                                )
                            }
                            if (completed.isNotEmpty() && visibleCompleted) {
                                Spacer(Modifier.height(12.dp))
                                CompletedSubheader(completed.size)
                                if (preferences.completedListMode == DayCompletedListMode.INLINE) {
                                    completed.forEach { entry ->
                                        DayEntryTile(
                                            entry = entry,
                                            book = bookMap[entry.bookId],
                                            checklist = checklist.filter { it.entryId == entry.id },
                                            reminders = reminders.filter { it.entryId == entry.id },
                                            expanded = expandedIds.contains(entry.id),
                                            preferences = preferences,
                                            onToggleExpanded = { onToggleExpanded(entry.id) },
                                            onToggle = { onToggleEntry(entry) },
                                            onToggleChecklist = onToggleChecklist,
                                            onEdit = { onEdit(entry) },
                                            onDelete = { onDelete(entry) }
                                        )
                                    }
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
private fun DayHeader(day: LocalDate, onOpenCalendar: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 4.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                day.format(DayTitleFormatter).replaceFirstChar { it.titlecase(Russian) },
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                lineHeight = 33.sp,
                color = InkPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                day.format(DaySubtitleFormatter),
                color = InkTertiary,
                fontSize = 15.sp,
                letterSpacing = 0.2.sp
            )
        }
        CircleTextButton("≡", onOpenSettings)
        CircleTextButton("□", onOpenCalendar)
    }
}

@Composable
private fun JournalPageCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(PaperSurface)
            .border(1.dp, PaperMuted.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
    ) {
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .width(3.dp)
                .height(2000.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentSoft.copy(alpha = 0.45f), AccentLine.copy(alpha = 0.55f))
                    )
                )
        )
        Column(modifier = Modifier.padding(start = 28.dp, top = 22.dp, end = 12.dp, bottom = 22.dp)) {
            content()
        }
    }
}

@Composable
private fun DayEntryTile(
    entry: JournalEntry,
    book: JournalBook?,
    checklist: List<ChecklistItem>,
    reminders: List<ReminderRule>,
    expanded: Boolean,
    preferences: AppUserPreferences,
    onToggleExpanded: () -> Unit,
    onToggle: () -> Unit,
    onToggleChecklist: (ChecklistItem) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val densityCompact = preferences.listDensity == DayListDensity.COMPACT
    val tileTop = if (densityCompact) 10.dp else 14.dp
    val tileBottom = if (densityCompact) 10.dp else 14.dp
    val titleColor = statusTitleColor(entry)
    val notes = entry.notes?.trim().orEmpty()
    val preview = when {
        entry.title.isBlank() && notes.isBlank() -> "Без названия"
        notes.isBlank() -> entry.title
        entry.title.isBlank() -> notes
        else -> "${entry.title}\n$notes"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (densityCompact) 7.dp else 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (expanded) PaperSurface.copy(alpha = 0.96f) else PaperSurface.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = if (expanded) PaperMuted.copy(alpha = 0.72f) else PaperMuted.copy(alpha = 0.42f),
                shape = RoundedCornerShape(16.dp)
            )
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, top = tileTop, end = 12.dp, bottom = tileBottom)) {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = entry.isCompleted, onCheckedChange = { onToggle() })
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleExpanded() },
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            preview,
                            modifier = Modifier.weight(1f),
                            maxLines = if (expanded) 8 else 3,
                            overflow = TextOverflow.Ellipsis,
                            color = titleColor,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (entry.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(if (expanded) "⌃" else "⌄", color = InkTertiary, fontSize = 21.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        subtitleLine(book, entry, reminders),
                        color = InkTertiary,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        letterSpacing = 0.15.sp
                    )
                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            if (notes.isNotBlank()) {
                                Text(notes, color = InkSecondary, lineHeight = 20.sp)
                                Spacer(Modifier.height(10.dp))
                            }
                            reminders.firstOrNull()?.triggerAt?.let {
                                ReminderBlock(it)
                                Spacer(Modifier.height(10.dp))
                            }
                            checklist.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = item.isCompleted, onCheckedChange = { onToggleChecklist(item) })
                                    Text(
                                        item.title,
                                        color = if (item.isCompleted) InkTertiary else InkPrimary,
                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = onEdit) { Text("Редактировать", color = AccentSoft) }
                                TextButton(onClick = onDelete) { Text("Удалить", color = Danger) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(PaperMuted.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✎", color = InkTertiary, fontSize = 36.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text("Чистая страница", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = InkPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Добавьте запись — она останется в контексте этого дня. Напоминание можно включить в редакторе.",
            color = InkTertiary,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun CalendarScreen(
    selectedDay: LocalDate,
    entries: List<JournalEntry>,
    books: List<JournalBook>,
    onBack: () -> Unit,
    onOpenBooks: () -> Unit,
    onSelectDay: (LocalDate) -> Unit
) {
    var visibleMonth by remember(selectedDay) { mutableStateOf(YearMonth.from(selectedDay)) }
    val first = visibleMonth.atDay(1)
    val leading = (first.dayOfWeek.value - 1 + 7) % 7
    val days = (1..visibleMonth.lengthOfMonth()).map { visibleMonth.atDay(it) }
    val cells = List<LocalDate?>(leading) { null } + days
    val counts = entries.groupBy { LocalDate.parse(it.dayDate) }.mapValues { it.value.map { entry -> entry.bookId } }
    val bookColors = books.associate { it.id to Color(it.colorArgb.toInt()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperBackground),
        contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 40.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleTextButton("‹", onBack)
                Spacer(Modifier.weight(1f))
                CircleTextButton("▤", onOpenBooks)
            }
        }
        item {
            Text("Календарь", fontFamily = FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            CalendarMonthHeader(
                title = visibleMonth.format(MonthFormatter).replaceFirstChar { it.titlecase(Russian) },
                year = visibleMonth.year,
                onPrevious = { visibleMonth = visibleMonth.minusMonths(1) },
                onNext = { visibleMonth = visibleMonth.plusMonths(1) },
                onToday = { visibleMonth = YearMonth.now() }
            )
            if (days.none { (counts[it] ?: emptyList()).isNotEmpty() }) {
                EmptyHintStrip("В этом месяце пока нет записей. Нажмите на день или вернитесь к сегодняшнему.")
            }
            MonthGrid(
                cells = cells,
                selectedDay = selectedDay,
                markers = counts,
                bookColors = bookColors,
                onSelectDay = onSelectDay
            )
        }
    }
}

@Composable
private fun BooksScreen(
    books: List<JournalBook>,
    entries: List<JournalEntry>,
    onBack: () -> Unit,
    onAddBook: () -> Unit,
    onToggleArchive: (JournalBook) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperBackground),
        contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleTextButton("‹", onBack)
                Spacer(Modifier.weight(1f))
                QuietButton("Добавить") { onAddBook() }
            }
            Text("Ежедневники", fontFamily = FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Medium)
        }
        items(books, key = { it.id }) { book ->
            PaperCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(book.colorArgb.toInt()))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(book.title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        Text("${entries.count { it.bookId == book.id }} записей", color = InkTertiary, fontSize = 13.sp)
                    }
                    TextButton(onClick = { onToggleArchive(book) }) {
                        Text(if (book.isArchived) "Вернуть" else "Архив", color = AccentSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    preferences: AppUserPreferences,
    onBack: () -> Unit,
    onChange: (AppUserPreferences) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperBackground),
        contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleTextButton("‹", onBack)
            }
            Text("Настройки", fontFamily = FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Medium)
        }
        item { ProductSectionLabel("День") }
        item {
            PaperCard {
                SettingsSwitchRow(
                    title = "Отдельная группа «Перенесено»",
                    checked = preferences.groupCarriedSection,
                    onChange = { onChange(preferences.copy(groupCarriedSection = it)) }
                )
                ThinDivider()
                SettingsSwitchRow(
                    title = "Показывать ежедневник в строке",
                    checked = preferences.showBookLabels,
                    onChange = { onChange(preferences.copy(showBookLabels = it)) }
                )
                ThinDivider()
                SettingsSwitchRow(
                    title = "Компактный список",
                    checked = preferences.listDensity == DayListDensity.COMPACT,
                    onChange = {
                        onChange(preferences.copy(listDensity = if (it) DayListDensity.COMPACT else DayListDensity.COMFORTABLE))
                    }
                )
            }
        }
        item { ProductSectionLabel("Завершённые задачи") }
        item {
            PaperCard {
                ModeRow("В списке", preferences.completedListMode == DayCompletedListMode.INLINE) {
                    onChange(preferences.copy(completedListMode = DayCompletedListMode.INLINE))
                }
                ThinDivider()
                ModeRow("Свернуть вниз", preferences.completedListMode == DayCompletedListMode.COLLAPSED) {
                    onChange(preferences.copy(completedListMode = DayCompletedListMode.COLLAPSED))
                }
                ThinDivider()
                ModeRow("Скрывать", preferences.completedListMode == DayCompletedListMode.HIDDEN) {
                    onChange(preferences.copy(completedListMode = DayCompletedListMode.HIDDEN))
                }
            }
        }
    }
}

@Composable
private fun EntryEditorDialog(
    day: LocalDate,
    entry: JournalEntry?,
    books: List<JournalBook>,
    checklist: List<ChecklistItem>,
    reminders: List<ReminderRule>,
    onDismiss: () -> Unit,
    onSave: (JournalEntry, List<String>, String) -> Unit
) {
    val now = remember { LocalDateTime.now().toString() }
    var text by remember(entry) {
        mutableStateOf(
            listOfNotNull(entry?.title, entry?.notes)
                .filter { it.isNotBlank() }
                .joinToString("\n")
        )
    }
    var bookId by remember(entry, books) { mutableStateOf(entry?.bookId ?: books.firstOrNull()?.id.orEmpty()) }
    var time by remember(entry) { mutableStateOf(entry?.timeOfDayMinutes?.let(::minutesLabel).orEmpty()) }
    var checklistText by remember(entry) {
        mutableStateOf(checklist.filter { it.entryId == entry?.id }.joinToString("\n") { it.title })
    }
    var reminderAt by remember(entry) {
        mutableStateOf(reminders.firstOrNull { it.entryId == entry?.id }?.triggerAt.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        title = { Text(if (entry == null) "Новая запись" else "Запись", fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст записи") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                BookChooser(books, bookId) { bookId = it }
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Время, HH:mm") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = checklistText,
                    onValueChange = { checklistText = it },
                    label = { Text("Чеклист, по пункту на строку") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reminderAt,
                    onValueChange = { reminderAt = it },
                    label = { Text("Напоминание, 2026-06-27T18:30") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val firstLine = text.lineSequence().firstOrNull()?.trim().orEmpty()
                    val rest = text.lines().drop(1).joinToString("\n").trim()
                    if (firstLine.isNotBlank() && bookId.isNotBlank()) {
                        onSave(
                            JournalEntry(
                                id = entry?.id ?: "entry-${UUID.randomUUID()}",
                                bookId = bookId,
                                dayDate = entry?.dayDate ?: day.toString(),
                                title = firstLine,
                                notes = rest.ifBlank { null },
                                manualPosition = entry?.manualPosition ?: 0,
                                timeOfDayMinutes = parseTimeMinutes(time),
                                isCompleted = entry?.isCompleted ?: false,
                                completedAt = entry?.completedAt,
                                visualStatus = entry?.visualStatus ?: JournalEntryVisualStatus.NEUTRAL,
                                createdAt = entry?.createdAt ?: now,
                                updatedAt = now
                            ),
                            checklistText.lines(),
                            reminderAt
                        )
                    }
                }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun BookEditorDialog(position: Int, onDismiss: () -> Unit, onSave: (JournalBook) -> Unit) {
    var title by remember { mutableStateOf("") }
    val colors = listOf(0xFF3D4A5CL, 0xFF4A6B55L, 0xFFC9B8A6L, 0xFF8A5D5DL, 0xFF7667A8L)
    var selected by remember { mutableStateOf(colors.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        title = { Text("Новый ежедневник", fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(color.toInt()))
                                .border(3.dp, if (selected == color) InkPrimary else Color.Transparent, CircleShape)
                                .clickable { selected = color }
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
                            colorArgb = selected,
                            manualPosition = position,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            }) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun CalendarMonthHeader(title: String, year: Int, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    PaperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleTextButton("‹", onPrevious)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onToday) { Text("$year", color = InkTertiary) }
            }
            CircleTextButton("›", onNext)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onToday) { Text("Сегодня", color = AccentSoft) }
    }
}

@Composable
private fun MonthGrid(
    cells: List<LocalDate?>,
    selectedDay: LocalDate,
    markers: Map<LocalDate, List<String>>,
    bookColors: Map<String, Color>,
    onSelectDay: (LocalDate) -> Unit
) {
    PaperCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
                    Text(it, color = InkTertiary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            cells.chunked(7).forEach { week ->
                Row {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(Modifier.weight(1f).height(48.dp))
                        } else {
                            CalendarDayCell(
                                day = day,
                                selected = day == selectedDay,
                                markerColors = (markers[day] ?: emptyList()).mapNotNull { bookColors[it] }.take(3),
                                onClick = { onSelectDay(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(Modifier.weight(1f).height(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(day: LocalDate, selected: Boolean, markerColors: List<Color>, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .height(52.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AccentSoft.copy(alpha = 0.10f) else Color.Transparent)
            .border(1.dp, if (selected) AccentSoft.copy(alpha = 0.7f) else PaperMuted.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(day.dayOfMonth.toString(), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            markerColors.forEach {
                Box(Modifier.size(5.dp).clip(CircleShape).background(it))
            }
        }
    }
}

@Composable
private fun BookChooser(books: List<JournalBook>, selectedBookId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Ежедневник", color = InkTertiary, fontSize = 12.sp)
        books.forEach { book ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (book.id == selectedBookId) AccentSoft.copy(alpha = 0.08f) else Color.Transparent)
                    .clickable { onSelect(book.id) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(book.colorArgb.toInt())))
                Spacer(Modifier.width(8.dp))
                Text(book.title)
            }
        }
    }
}

@Composable
private fun ProductSectionLabel(text: String) {
    Text(text.uppercase(Russian), color = InkTertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
}

@Composable
private fun PaperCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PaperMuted.copy(alpha = 0.65f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(modifier = Modifier.background(PaperSurface).padding(14.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), color = InkPrimary)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ModeRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AccentSoft.copy(alpha = 0.06f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (selected) "●" else "○", color = if (selected) AccentSoft else InkTertiary)
        Spacer(Modifier.width(10.dp))
        Text(title)
    }
}

@Composable
private fun ThinDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(PaperMuted.copy(alpha = 0.8f)))
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = InkTertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
}

@Composable
private fun CompletedSubheader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(PaperMuted))
        Text("  Завершено · $count  ", color = InkTertiary, fontSize = 12.sp)
        Box(Modifier.weight(1f).height(1.dp).background(PaperMuted))
    }
}

@Composable
private fun ReminderBlock(triggerAt: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PaperMuted.copy(alpha = 0.35f))
            .border(1.dp, PaperMuted.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text("Напоминание · $triggerAt", color = InkSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun EmptyHintStrip(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PaperMuted.copy(alpha = 0.35f))
            .border(1.dp, PaperMuted.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(text, color = InkSecondary, lineHeight = 19.sp)
    }
}

@Composable
private fun QuietButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, color = AccentSoft, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CircleTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = InkSecondary.copy(alpha = 0.88f), fontSize = 23.sp)
    }
}

private fun subtitleLine(book: JournalBook?, entry: JournalEntry, reminders: List<ReminderRule>): String {
    val parts = mutableListOf<String>()
    book?.title?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
    entry.timeOfDayMinutes?.let { parts.add(minutesLabel(it)) }
    reminders.firstOrNull()?.triggerAt?.let { parts.add("напоминание") }
    if (entry.sourceType.name != "MANUAL") parts.add(entry.sourceType.name.lowercase())
    return parts.joinToString(" · ").ifBlank { "Без времени" }
}

private fun statusTitleColor(entry: JournalEntry): Color =
    when (entry.visualStatus) {
        JournalEntryVisualStatus.NEUTRAL -> if (entry.isCompleted) InkTertiary else InkPrimary
        JournalEntryVisualStatus.FOCUS -> Color(0xFF67518A)
        JournalEntryVisualStatus.BLOCKED -> Danger
        JournalEntryVisualStatus.DONE -> SuccessSoft
    }

private fun sectionLabel(day: LocalDate): String =
    when (day) {
        LocalDate.now() -> "СЕГОДНЯ"
        LocalDate.now().minusDays(1) -> "ВЧЕРА"
        LocalDate.now().plusDays(1) -> "ЗАВТРА"
        else -> day.format(DateTimeFormatter.ofPattern("d MMMM", Russian)).uppercase(Russian)
    }

private fun demoJournalSnapshot(): JournalSnapshot {
    val today = LocalDate.now()
    val now = LocalDateTime.now().toString()
    val personal = JournalBook(
        id = "book-personal",
        title = "Личное",
        colorArgb = 0xFF3D4A5C,
        manualPosition = 0,
        createdAt = now,
        updatedAt = now
    )
    val work = JournalBook(
        id = "book-work",
        title = "Работа",
        colorArgb = 0xFF4A6B55,
        manualPosition = 1,
        createdAt = now,
        updatedAt = now
    )
    val morning = JournalEntry(
        id = "entry-morning",
        bookId = personal.id,
        dayDate = today.toString(),
        title = "Собрать страницу дня",
        notes = "Оставить только важное: задачи, заметки и напоминания в одном спокойном листе.",
        manualPosition = 0,
        timeOfDayMinutes = 9 * 60,
        visualStatus = JournalEntryVisualStatus.FOCUS,
        createdAt = now,
        updatedAt = now
    )
    val review = JournalEntry(
        id = "entry-review",
        bookId = work.id,
        dayDate = today.toString(),
        title = "Проверить перенос на Kotlin",
        notes = "Сверить экран дня, календарь и настройки с исходным Flutter-приложением.",
        manualPosition = 1,
        timeOfDayMinutes = 16 * 60 + 30,
        createdAt = now,
        updatedAt = now
    )
    return JournalSnapshot(
        books = listOf(personal, work),
        entries = listOf(morning, review),
        checklistItems = listOf(
            ChecklistItem("check-1", morning.id, "Открыть календарь"),
            ChecklistItem("check-2", morning.id, "Добавить запись")
        ),
        reminderRules = listOf(ReminderRule("reminder-1", review.id, ReminderRuleKind.DATE_TIME, "${today}T16:30"))
    )
}

private fun minutesLabel(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

private fun parseTimeMinutes(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}

private enum class AppScreen {
    Day,
    Calendar,
    Books,
    Settings
}
