package com.remka.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remka.data.RemkaSnapshot
import com.remka.domain.MaintenancePlan
import com.remka.domain.MaintenancePlanStatus
import com.remka.domain.Vehicle
import com.remka.domain.VehicleEvent
import com.remka.domain.VehicleEventType
import com.remka.domain.VehicleFolder
import com.remka.domain.VehicleType
import java.time.LocalDate
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RemkaTheme {
                RemkaApp()
            }
        }
    }
}

@Composable
private fun RemkaTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            content()
        }
    }
}

@Composable
private fun RemkaApp() {
    val context = LocalContext.current
    val store = remember {
        AndroidRemkaStore(context.filesDir.resolve("remka-data.json"))
    }
    val initialSnapshot = remember {
        store.load() ?: demoSnapshot()
    }
    var screen by remember { mutableStateOf(RemkaScreen.VehicleList) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var selectedPlanId by remember { mutableStateOf<String?>(null) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    val vehicles = remember {
        mutableStateListOf<Vehicle>().apply {
            addAll(initialSnapshot.vehicles)
        }
    }
    val events = remember {
        mutableStateListOf<VehicleEvent>().apply {
            addAll(initialSnapshot.events)
        }
    }
    val plans = remember {
        mutableStateListOf<MaintenancePlan>().apply {
            addAll(initialSnapshot.plans)
        }
    }
    val folders = remember {
        mutableStateListOf<VehicleFolder>().apply {
            addAll(initialSnapshot.folders)
        }
    }
    fun saveState() {
        store.save(
            RemkaSnapshot(
                vehicles = vehicles.toList(),
                events = events.toList(),
                plans = plans.toList(),
                folders = folders.toList()
            )
        )
    }
    fun touchVehicle(vehicleId: String) {
        val index = vehicles.indexOfFirst { vehicle -> vehicle.id == vehicleId }
        if (index != -1) {
            vehicles[index] = vehicles[index].copy(updatedAt = todayText())
        }
    }

    when (screen) {
        RemkaScreen.VehicleList -> VehicleListScreen(
            vehicles = vehicles
                .filter { vehicle -> vehicle.folderId == null }
                .sortedByDescending { vehicle -> vehicle.updatedAt },
            folders = folders.sortedWith(
                compareByDescending<VehicleFolder> { folder -> folder.isPinned }
                    .thenBy { folder -> folder.name.lowercase() }
            ),
            folderVehicleCounts = vehicles
                .groupingBy { vehicle -> vehicle.folderId }
                .eachCount(),
            onAddClick = { screen = RemkaScreen.AddChoice },
            onJournalClick = { screen = RemkaScreen.Journal },
            onFolderClick = { folder ->
                selectedFolderId = folder.id
                screen = RemkaScreen.FolderDetails
            },
            onRenameFolder = { folder ->
                selectedFolderId = folder.id
                screen = RemkaScreen.EditFolder
            },
            onDeleteFolder = { folder ->
                folders.removeAll { existingFolder -> existingFolder.id == folder.id }
                vehicles.indices.forEach { index ->
                    if (vehicles[index].folderId == folder.id) {
                        vehicles[index] = vehicles[index].copy(
                            folderId = null,
                            updatedAt = todayText()
                        )
                    }
                }
                saveState()
            },
            onTogglePinFolder = { folder ->
                val index = folders.indexOfFirst { existingFolder -> existingFolder.id == folder.id }
                if (index != -1) {
                    folders[index] = folders[index].copy(isPinned = !folders[index].isPinned)
                    saveState()
                }
            },
            onVehicleClick = { vehicle ->
                selectedVehicleId = vehicle.id
                screen = RemkaScreen.VehicleDetails
            }
        )

        RemkaScreen.AddChoice -> AddChoiceScreen(
            onBack = { screen = RemkaScreen.VehicleList },
            onAddVehicleClick = { screen = RemkaScreen.AddVehicle },
            onAddFolderClick = { screen = RemkaScreen.AddFolder }
        )

        RemkaScreen.AddFolder -> FolderFormScreen(
            folderToEdit = null,
            onBack = { screen = RemkaScreen.VehicleList },
            onSave = { folderName ->
                folders.add(
                    VehicleFolder(
                        id = UUID.randomUUID().toString(),
                        name = folderName.trim(),
                        createdAt = todayText()
                    )
                )
                saveState()
                screen = RemkaScreen.VehicleList
            }
        )

        RemkaScreen.EditFolder -> {
            val folderToEdit = folders.firstOrNull { folder -> folder.id == selectedFolderId }

            if (folderToEdit == null) {
                screen = RemkaScreen.VehicleList
            } else {
                FolderFormScreen(
                    folderToEdit = folderToEdit,
                    onBack = { screen = RemkaScreen.VehicleList },
                    onSave = { folderName ->
                        val index = folders.indexOfFirst { folder -> folder.id == folderToEdit.id }
                        if (index != -1) {
                            folders[index] = folders[index].copy(name = folderName.trim())
                            saveState()
                        }
                        screen = RemkaScreen.VehicleList
                    }
                )
            }
        }

        RemkaScreen.FolderDetails -> {
            val selectedFolder = folders.firstOrNull { folder -> folder.id == selectedFolderId }

            if (selectedFolder == null) {
                screen = RemkaScreen.VehicleList
            } else {
                FolderDetailsScreen(
                    folder = selectedFolder,
                    vehicles = vehicles
                        .filter { vehicle -> vehicle.folderId == selectedFolder.id }
                        .sortedByDescending { vehicle -> vehicle.updatedAt },
                    onBack = { screen = RemkaScreen.VehicleList },
                    onVehicleClick = { vehicle ->
                        selectedVehicleId = vehicle.id
                        screen = RemkaScreen.VehicleDetails
                    }
                )
            }
        }

        RemkaScreen.AddVehicle -> AddVehicleScreen(
            vehicleToEdit = null,
            folders = folders,
            onBack = { screen = RemkaScreen.VehicleList },
            onSave = { vehicle ->
                vehicles.add(vehicle.copy(updatedAt = todayText()))
                saveState()
                screen = RemkaScreen.VehicleList
            }
        )

        RemkaScreen.EditVehicle -> {
            val vehicleToEdit = vehicles.firstOrNull { vehicle -> vehicle.id == selectedVehicleId }

            if (vehicleToEdit == null) {
                screen = RemkaScreen.VehicleList
            } else {
                AddVehicleScreen(
                    vehicleToEdit = vehicleToEdit,
                    folders = folders,
                    onBack = { screen = RemkaScreen.VehicleList },
                    onSave = { vehicle ->
                        val index = vehicles.indexOfFirst { existingVehicle -> existingVehicle.id == vehicle.id }
                        if (index != -1) {
                            vehicles[index] = vehicle.copy(updatedAt = todayText())
                            saveState()
                        }
                        screen = RemkaScreen.VehicleList
                    }
                )
            }
        }

        RemkaScreen.VehicleDetails -> {
            val selectedVehicle = vehicles.firstOrNull { vehicle -> vehicle.id == selectedVehicleId }

            if (selectedVehicle == null) {
                screen = RemkaScreen.VehicleList
            } else {
                VehicleDetailsScreen(
                    vehicle = selectedVehicle,
                    events = events.filter { event -> event.vehicleId == selectedVehicle.id },
                    plans = plans.filter { plan -> plan.vehicleId == selectedVehicle.id },
                    onBack = {
                        if (selectedVehicle.folderId == null) {
                            screen = RemkaScreen.VehicleList
                        } else {
                            selectedFolderId = selectedVehicle.folderId
                            screen = RemkaScreen.FolderDetails
                        }
                    },
                    onAddEventClick = { screen = RemkaScreen.AddEvent },
                    onAddPlanClick = { screen = RemkaScreen.AddPlan },
                    onEditVehicleClick = { screen = RemkaScreen.EditVehicle },
                    onDeleteVehicleClick = {
                        vehicles.removeAll { vehicle -> vehicle.id == selectedVehicle.id }
                        events.removeAll { event -> event.vehicleId == selectedVehicle.id }
                        plans.removeAll { plan -> plan.vehicleId == selectedVehicle.id }
                        selectedVehicleId = null
                        saveState()
                        screen = RemkaScreen.VehicleList
                    },
                    onEditEventClick = { event ->
                        selectedEventId = event.id
                        screen = RemkaScreen.EditEvent
                    },
                    onDeleteEventClick = { event ->
                        events.removeAll { existingEvent -> existingEvent.id == event.id }
                        touchVehicle(selectedVehicle.id)
                        saveState()
                    },
                    onEditPlanClick = { plan ->
                        selectedPlanId = plan.id
                        screen = RemkaScreen.EditPlan
                    },
                    onDeletePlanClick = { plan ->
                        plans.removeAll { existingPlan -> existingPlan.id == plan.id }
                        touchVehicle(selectedVehicle.id)
                        saveState()
                    },
                    onPlanStatusChange = { plan, status ->
                        val index = plans.indexOfFirst { existingPlan -> existingPlan.id == plan.id }
                        if (index != -1) {
                            plans[index] = plan.copy(status = status)
                            touchVehicle(plan.vehicleId)
                            saveState()
                        }
                    }
                )
            }
        }

        RemkaScreen.AddEvent -> {
            val selectedVehicle = vehicles.firstOrNull { vehicle -> vehicle.id == selectedVehicleId }

            if (selectedVehicle == null) {
                screen = RemkaScreen.VehicleList
            } else {
                AddEventScreen(
                    vehicle = selectedVehicle,
                    eventToEdit = null,
                    onBack = { screen = RemkaScreen.VehicleDetails },
                    onSave = { event ->
                        events.add(event)
                        touchVehicle(event.vehicleId)
                        saveState()
                        screen = RemkaScreen.VehicleDetails
                    }
                )
            }
        }

        RemkaScreen.EditEvent -> {
            val selectedVehicle = vehicles.firstOrNull { vehicle -> vehicle.id == selectedVehicleId }
            val eventToEdit = events.firstOrNull { event -> event.id == selectedEventId }

            if (selectedVehicle == null || eventToEdit == null) {
                screen = RemkaScreen.VehicleDetails
            } else {
                AddEventScreen(
                    vehicle = selectedVehicle,
                    eventToEdit = eventToEdit,
                    onBack = { screen = RemkaScreen.VehicleDetails },
                    onSave = { event ->
                        val index = events.indexOfFirst { existingEvent -> existingEvent.id == event.id }
                        if (index != -1) {
                            events[index] = event
                            touchVehicle(event.vehicleId)
                            saveState()
                        }
                        screen = RemkaScreen.VehicleDetails
                    }
                )
            }
        }

        RemkaScreen.AddPlan -> {
            val selectedVehicle = vehicles.firstOrNull { vehicle -> vehicle.id == selectedVehicleId }

            if (selectedVehicle == null) {
                screen = RemkaScreen.VehicleList
            } else {
                AddPlanScreen(
                    vehicle = selectedVehicle,
                    planToEdit = null,
                    onBack = { screen = RemkaScreen.VehicleDetails },
                    onSave = { plan ->
                        plans.add(plan)
                        touchVehicle(plan.vehicleId)
                        saveState()
                        screen = RemkaScreen.VehicleDetails
                    }
                )
            }
        }

        RemkaScreen.EditPlan -> {
            val selectedVehicle = vehicles.firstOrNull { vehicle -> vehicle.id == selectedVehicleId }
            val planToEdit = plans.firstOrNull { plan -> plan.id == selectedPlanId }

            if (selectedVehicle == null || planToEdit == null) {
                screen = RemkaScreen.VehicleDetails
            } else {
                AddPlanScreen(
                    vehicle = selectedVehicle,
                    planToEdit = planToEdit,
                    onBack = { screen = RemkaScreen.VehicleDetails },
                    onSave = { plan ->
                        val index = plans.indexOfFirst { existingPlan -> existingPlan.id == plan.id }
                        if (index != -1) {
                            plans[index] = plan
                            touchVehicle(plan.vehicleId)
                            saveState()
                        }
                        screen = RemkaScreen.VehicleDetails
                    }
                )
            }
        }

        RemkaScreen.Journal -> JournalScreen(
            vehicles = vehicles,
            events = events,
            plans = plans,
            onBack = { screen = RemkaScreen.VehicleList }
        )
    }
}

private enum class RemkaScreen {
    VehicleList,
    Journal,
    AddChoice,
    AddFolder,
    EditFolder,
    FolderDetails,
    AddVehicle,
    EditVehicle,
    VehicleDetails,
    AddEvent,
    EditEvent,
    AddPlan,
    EditPlan
}

@Composable
private fun VehicleListScreen(
    vehicles: List<Vehicle>,
    folders: List<VehicleFolder>,
    folderVehicleCounts: Map<String?, Int>,
    onAddClick: () -> Unit,
    onJournalClick: () -> Unit,
    onFolderClick: (VehicleFolder) -> Unit,
    onRenameFolder: (VehicleFolder) -> Unit,
    onDeleteFolder: (VehicleFolder) -> Unit,
    onTogglePinFolder: (VehicleFolder) -> Unit,
    onVehicleClick: (Vehicle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Remka",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Транспорт и история работ",
                    color = Color(0xFF64748B)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onJournalClick) {
                    Text("Журнал")
                }

                Button(onClick = onAddClick) {
                    Text("+")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (folders.isNotEmpty()) {
                item {
                    SectionTitle("Папки")
                }
            }

            items(folders, key = { folder -> folder.id }) { folder ->
                FolderCard(
                    folder = folder,
                    vehicleCount = folderVehicleCounts[folder.id] ?: 0,
                    onClick = { onFolderClick(folder) },
                    onRenameClick = { onRenameFolder(folder) },
                    onDeleteClick = { onDeleteFolder(folder) },
                    onTogglePinClick = { onTogglePinFolder(folder) }
                )
            }

            item {
                SectionTitle("Транспорт")
            }

            if (vehicles.isEmpty()) {
                item {
                    EmptyText("Техники без папки пока нет")
                }
            } else {
                items(vehicles, key = { vehicle -> vehicle.id }) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        folderName = null,
                        onClick = { onVehicleClick(vehicle) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddChoiceScreen(
    onBack: () -> Unit,
    onAddVehicleClick: () -> Unit,
    onAddFolderClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Добавить",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Выбери, что создаём",
                    color = Color(0xFF64748B)
                )
            }

            OutlinedButton(onClick = onBack) {
                Text("Назад")
            }
        }

        ActionCard(
            title = "Новый транспорт",
            subtitle = "Мотоцикл, машина, лодка или другое",
            onClick = onAddVehicleClick
        )

        ActionCard(
            title = "Новая папка",
            subtitle = "Например Ростов, Гараж, Продажа",
            onClick = onAddFolderClick
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun FolderFormScreen(
    folderToEdit: VehicleFolder?,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(folderToEdit?.name ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (folderToEdit == null) "Новая папка" else "Переименовать папку",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Папки помогают не смешивать технику",
                        color = Color(0xFF64748B)
                    )
                }

                OutlinedButton(onClick = onBack) {
                    Text("Назад")
                }
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
                onClick = { onSave(name) }
            ) {
                Text("Сохранить")
            }
        }
    }
}

@Composable
private fun FolderDetailsScreen(
    folder: VehicleFolder,
    vehicles: List<Vehicle>,
    onBack: () -> Unit,
    onVehicleClick: (Vehicle) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = folder.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (folder.isPinned) "Закреплена" else "Папка техники",
                        color = Color(0xFF64748B)
                    )
                }

                OutlinedButton(onClick = onBack) {
                    Text("Назад")
                }
            }
        }

        if (vehicles.isEmpty()) {
            item {
                EmptyText("В папке пока нет техники")
            }
        } else {
            items(vehicles, key = { vehicle -> vehicle.id }) { vehicle ->
                VehicleCard(
                    vehicle = vehicle,
                    folderName = folder.name,
                    onClick = { onVehicleClick(vehicle) }
                )
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: VehicleFolder,
    vehicleCount: Int,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTogglePinClick: () -> Unit
) {
    var revealedAction by remember { mutableStateOf<String?>(null) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            revealedAction = when (value) {
                SwipeToDismissBoxValue.StartToEnd -> "manage"
                SwipeToDismissBoxValue.EndToStart -> "delete"
                SwipeToDismissBoxValue.Settled -> null
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (folder.isPinned) "${folder.name} *" else folder.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "$vehicleCount ед. техники",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = ">",
                        color = Color(0xFF64748B),
                        fontSize = 18.sp
                    )
                }

                if (revealedAction == "manage") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                revealedAction = null
                                onRenameClick()
                            }
                        ) {
                            Text("Переименовать")
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                revealedAction = null
                                onTogglePinClick()
                            }
                        ) {
                            Text(if (folder.isPinned) "Открепить" else "Закрепить")
                        }
                    }
                }

                if (revealedAction == "delete") {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            revealedAction = null
                            onDeleteClick()
                        }
                    ) {
                        Text("Удалить папку")
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalScreen(
    vehicles: List<Vehicle>,
    events: List<VehicleEvent>,
    plans: List<MaintenancePlan>,
    onBack: () -> Unit
) {
    var dateFrom by remember { mutableStateOf(todayText()) }
    var dateTo by remember { mutableStateOf(todayText()) }
    val vehicleNames = vehicles.associate { vehicle -> vehicle.id to vehicle.name }
    val journalEvents = events
        .filter { event -> event.date.isDateInRange(dateFrom, dateTo) }
        .map { event ->
            JournalEntry(
                date = event.date,
                title = event.title,
                vehicleName = vehicleNames[event.vehicleId] ?: "Техника удалена",
                type = event.type.displayName(),
                details = listOfNotNull(
                    event.mileage?.let { mileage -> "$mileage км" },
                    event.cost?.let { cost -> "$cost" },
                    event.shopName
                ).joinToString(" · ").ifBlank { event.comment ?: "Без деталей" }
            )
        }
    val journalPlans = plans
        .filter { plan -> plan.plannedDate.isDateInRange(dateFrom, dateTo) }
        .map { plan ->
            JournalEntry(
                date = plan.plannedDate,
                title = plan.title,
                vehicleName = vehicleNames[plan.vehicleId] ?: "Техника удалена",
                type = "План: ${plan.status.displayName()}",
                details = listOfNotNull(
                    plan.reminderDate?.let { reminder -> "напомнить $reminder" },
                    plan.targetMileage?.let { mileage -> "$mileage км" },
                    plan.placeToBuy,
                    plan.responsiblePerson
                ).joinToString(" · ").ifBlank { plan.comment ?: "Без деталей" }
            )
        }
    val entries = (journalEvents + journalPlans).sortedByDescending { entry -> entry.date }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Журнал",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Что делали по датам",
                        color = Color(0xFF64748B)
                    )
                }

                OutlinedButton(onClick = onBack) {
                    Text("Назад")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Период",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = dateFrom,
                            onValueChange = { dateFrom = it },
                            label = { Text("С") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = dateTo,
                            onValueChange = { dateTo = it },
                            label = { Text("По") },
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val yesterday = LocalDate.now().minusDays(1).toString()
                                dateFrom = yesterday
                                dateTo = yesterday
                            }
                        ) {
                            Text("Вчера")
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                dateFrom = todayText()
                                dateTo = todayText()
                            }
                        ) {
                            Text("Сегодня")
                        }
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                EmptyText("За этот период записей нет")
            }
        } else {
            items(entries, key = { entry -> "${entry.date}-${entry.vehicleName}-${entry.title}" }) { entry ->
                JournalCard(entry = entry)
            }
        }
    }
}

@Composable
private fun JournalCard(entry: JournalEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = entry.title,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "${entry.date} · ${entry.vehicleName} · ${entry.type}",
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
            Text(
                text = entry.details,
                color = Color(0xFF334155)
            )
        }
    }
}

private data class JournalEntry(
    val date: String,
    val title: String,
    val vehicleName: String,
    val type: String,
    val details: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleScreen(
    vehicleToEdit: Vehicle?,
    folders: List<VehicleFolder>,
    onBack: () -> Unit,
    onSave: (Vehicle) -> Unit
) {
    var type by remember { mutableStateOf(vehicleToEdit?.type ?: VehicleType.MOTORCYCLE) }
    var name by remember { mutableStateOf(vehicleToEdit?.name ?: "") }
    var manufacturer by remember { mutableStateOf(vehicleToEdit?.manufacturer ?: "") }
    var model by remember { mutableStateOf(vehicleToEdit?.model ?: "") }
    var year by remember { mutableStateOf(vehicleToEdit?.year?.toString() ?: "") }
    var registrationNumber by remember { mutableStateOf(vehicleToEdit?.registrationNumber ?: "") }
    var mileage by remember { mutableStateOf(vehicleToEdit?.currentMileage?.toString() ?: "") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var folderId by remember { mutableStateOf(vehicleToEdit?.folderId) }

    val canSave = name.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (vehicleToEdit == null) "Новый транспорт" else "Изменить транспорт",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Заполни основные данные",
                        color = Color(0xFF64748B)
                    )
                }

                OutlinedButton(onClick = onBack) {
                    Text("Назад")
                }
            }
        }

        item {
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                    value = type.displayName(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Тип") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    VehicleType.entries.forEach { vehicleType ->
                        DropdownMenuItem(
                            text = { Text(vehicleType.displayName()) },
                            onClick = {
                                type = vehicleType
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            ExposedDropdownMenuBox(
                expanded = folderMenuExpanded,
                onExpandedChange = { folderMenuExpanded = !folderMenuExpanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                    value = folders.firstOrNull { folder -> folder.id == folderId }?.name ?: "Без папки",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Папка") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderMenuExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = folderMenuExpanded,
                    onDismissRequest = { folderMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Без папки") },
                        onClick = {
                            folderId = null
                            folderMenuExpanded = false
                        }
                    )

                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = {
                                folderId = folder.id
                                folderMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                supportingText = {
                    if (!canSave) {
                        Text("Обязательное поле")
                    }
                }
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = manufacturer,
                onValueChange = { manufacturer = it },
                label = { Text("Производитель") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = model,
                onValueChange = { model = it },
                label = { Text("Модель") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = year,
                onValueChange = { year = it.onlyDigits() },
                label = { Text("Год") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = registrationNumber,
                onValueChange = { registrationNumber = it },
                label = { Text("Госномер") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = mileage,
                onValueChange = { mileage = it.onlyDigits() },
                label = { Text("Пробег") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = {
                    onSave(
                        Vehicle(
                            id = vehicleToEdit?.id ?: UUID.randomUUID().toString(),
                            type = type,
                            name = name.trim(),
                            manufacturer = manufacturer.trim().ifBlank { null },
                            model = model.trim().ifBlank { null },
                            year = year.toIntOrNull(),
                            registrationNumber = registrationNumber.trim().ifBlank { null },
                            currentMileage = mileage.toLongOrNull(),
                            folderId = folderId,
                            updatedAt = vehicleToEdit?.updatedAt ?: todayText()
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventScreen(
    vehicle: Vehicle,
    eventToEdit: VehicleEvent?,
    onBack: () -> Unit,
    onSave: (VehicleEvent) -> Unit
) {
    var type by remember { mutableStateOf(eventToEdit?.type ?: VehicleEventType.MAINTENANCE) }
    var title by remember { mutableStateOf(eventToEdit?.title ?: "") }
    var date by remember { mutableStateOf(eventToEdit?.date ?: "2026-06-24") }
    var mileage by remember { mutableStateOf(eventToEdit?.mileage?.toString() ?: "") }
    var cost by remember { mutableStateOf(eventToEdit?.cost?.toString() ?: "") }
    var shopName by remember { mutableStateOf(eventToEdit?.shopName ?: "") }
    var comment by remember { mutableStateOf(eventToEdit?.comment ?: "") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank() && date.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (eventToEdit == null) "Новое событие" else "Изменить событие",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = vehicle.name,
                        color = Color(0xFF64748B)
                    )
                }

                OutlinedButton(onClick = onBack) {
                    Text("Назад")
                }
            }
        }

        item {
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                    value = type.displayName(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Тип события") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    VehicleEventType.entries.forEach { eventType ->
                        DropdownMenuItem(
                            text = { Text(eventType.displayName()) },
                            onClick = {
                                type = eventType
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = date,
                onValueChange = { date = it },
                label = { Text("Дата") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = mileage,
                onValueChange = { mileage = it.onlyDigits() },
                label = { Text("Пробег") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = cost,
                onValueChange = { cost = it.onlyDecimalNumber() },
                label = { Text("Стоимость") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text("Магазин или место") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий") },
                minLines = 3
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = {
                    onSave(
                        VehicleEvent(
                            id = eventToEdit?.id ?: UUID.randomUUID().toString(),
                            vehicleId = vehicle.id,
                            type = type,
                            title = title.trim(),
                            date = date.trim(),
                            mileage = mileage.toLongOrNull(),
                            cost = cost.replace(',', '.').toDoubleOrNull(),
                            shopName = shopName.trim().ifBlank { null },
                            comment = comment.trim().ifBlank { null }
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        }
    }
}

@Composable
private fun AddPlanScreen(
    vehicle: Vehicle,
    planToEdit: MaintenancePlan?,
    onBack: () -> Unit,
    onSave: (MaintenancePlan) -> Unit
) {
    var title by remember { mutableStateOf(planToEdit?.title ?: "") }
    var plannedDate by remember { mutableStateOf(planToEdit?.plannedDate ?: "2026-07-10") }
    var reminderDate by remember { mutableStateOf(planToEdit?.reminderDate ?: "") }
    var targetMileage by remember { mutableStateOf(planToEdit?.targetMileage?.toString() ?: "") }
    var placeToBuy by remember { mutableStateOf(planToEdit?.placeToBuy ?: "") }
    var responsiblePerson by remember { mutableStateOf(planToEdit?.responsiblePerson ?: "") }
    var comment by remember { mutableStateOf(planToEdit?.comment ?: "") }

    val canSave = title.isNotBlank() && plannedDate.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (planToEdit == null) "Новый план" else "Изменить план",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = vehicle.name,
                        color = Color(0xFF64748B)
                    )
                }

                OutlinedButton(onClick = onBack) {
                    Text("Назад")
                }
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = plannedDate,
                onValueChange = { plannedDate = it },
                label = { Text("Дата выполнения") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = reminderDate,
                onValueChange = { reminderDate = it },
                label = { Text("Дата напоминания") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = targetMileage,
                onValueChange = { targetMileage = it.onlyDigits() },
                label = { Text("Пробег для выполнения") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = placeToBuy,
                onValueChange = { placeToBuy = it },
                label = { Text("Где купить") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = responsiblePerson,
                onValueChange = { responsiblePerson = it },
                label = { Text("Кто отвечает") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий") },
                minLines = 3
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = {
                    onSave(
                        MaintenancePlan(
                            id = planToEdit?.id ?: UUID.randomUUID().toString(),
                            vehicleId = vehicle.id,
                            title = title.trim(),
                            plannedDate = plannedDate.trim(),
                            reminderDate = reminderDate.trim().ifBlank { null },
                            targetMileage = targetMileage.toLongOrNull(),
                            placeToBuy = placeToBuy.trim().ifBlank { null },
                            responsiblePerson = responsiblePerson.trim().ifBlank { null },
                            comment = comment.trim().ifBlank { null },
                            status = planToEdit?.status ?: MaintenancePlanStatus.PLANNED
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        }
    }
}

@Composable
private fun VehicleDetailsScreen(
    vehicle: Vehicle,
    events: List<VehicleEvent>,
    plans: List<MaintenancePlan>,
    onBack: () -> Unit,
    onAddEventClick: () -> Unit,
    onAddPlanClick: () -> Unit,
    onEditVehicleClick: () -> Unit,
    onDeleteVehicleClick: () -> Unit,
    onEditEventClick: (VehicleEvent) -> Unit,
    onDeleteEventClick: (VehicleEvent) -> Unit,
    onEditPlanClick: (MaintenancePlan) -> Unit,
    onDeletePlanClick: (MaintenancePlan) -> Unit,
    onPlanStatusChange: (MaintenancePlan, MaintenancePlanStatus) -> Unit
) {
    var vehicleActionsVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vehicle.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = vehicle.type.displayName(),
                        color = Color(0xFF64748B)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { vehicleActionsVisible = !vehicleActionsVisible }) {
                        Text("✎")
                    }

                    OutlinedButton(onClick = onBack) {
                        Text("Назад")
                    }
                }
            }
        }

        if (vehicleActionsVisible) {
            item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onEditVehicleClick
                ) {
                    Text("Изменить")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDeleteVehicleClick
                ) {
                    Text("Удалить")
                }
            }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Сводка",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    DetailLine("Модель", listOfNotNull(vehicle.manufacturer, vehicle.model).joinToString(" ").ifBlank { "не указана" })
                    DetailLine("Год", vehicle.year?.toString() ?: "не указан")
                    DetailLine("Госномер", vehicle.registrationNumber ?: "не указан")
                    DetailLine("Пробег", vehicle.currentMileage?.let { "$it км" } ?: "не указан")
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("История")
                Button(onClick = onAddEventClick) {
                    Text("Добавить")
                }
            }
        }

        if (events.isEmpty()) {
            item {
                EmptyText("Событий пока нет")
            }
        } else {
            items(events, key = { event -> event.id }) { event ->
                EventCard(
                    event = event,
                    onEditClick = { onEditEventClick(event) },
                    onDeleteClick = { onDeleteEventClick(event) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Планы")
                Button(onClick = onAddPlanClick) {
                    Text("Добавить")
                }
            }
        }

        if (plans.isEmpty()) {
            item {
                EmptyText("Планов пока нет")
            }
        } else {
            items(plans, key = { plan -> plan.id }) { plan ->
                PlanCard(
                    plan = plan,
                    onEditClick = { onEditPlanClick(plan) },
                    onDeleteClick = { onDeletePlanClick(plan) },
                    onStatusChange = { status -> onPlanStatusChange(plan, status) }
                )
            }
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    folderName: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VehicleIcon(type = vehicle.type)

            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            ) {
                Text(
                    text = vehicle.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = listOfNotNull(vehicle.manufacturer, vehicle.model, vehicle.year?.toString())
                        .joinToString(" ")
                        .ifBlank { "Модель не указана" },
                    color = Color(0xFF475569)
                )

                Text(
                    text = "Пробег: ${vehicle.currentMileage ?: "не указан"} км",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )

                Text(
                    text = listOfNotNull(
                        folderName?.let { folder -> "Папка: $folder" },
                        vehicle.updatedAt.ifBlank { null }?.let { date -> "Изменено: $date" }
                    ).joinToString(" · ").ifBlank { "Без папки" },
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }

            Text(
                text = vehicle.registrationNumber ?: "без номера",
                color = Color(0xFF334155),
                fontSize = 13.sp
            )
        }

    }
}

@Composable
private fun EventCard(
    event: VehicleEvent,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var revealedAction by remember { mutableStateOf<String?>(null) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            revealedAction = when (value) {
                SwipeToDismissBoxValue.StartToEnd -> "edit"
                SwipeToDismissBoxValue.EndToStart -> "delete"
                SwipeToDismissBoxValue.Settled -> null
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${event.date} · ${event.type.displayName()}",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
                DetailLine("Пробег", event.mileage?.let { "$it км" } ?: "не указан")
                DetailLine("Стоимость", event.cost?.let { "$it" } ?: "не указана")
                DetailLine("Комментарий", event.comment ?: "нет")

                if (revealedAction == "edit") {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            revealedAction = null
                            onEditClick()
                        }
                    ) {
                        Text("Изменить")
                    }
                }

                if (revealedAction == "delete") {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            revealedAction = null
                            onDeleteClick()
                        }
                    ) {
                        Text("Удалить")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: MaintenancePlan,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusChange: (MaintenancePlanStatus) -> Unit
) {
    var revealedAction by remember { mutableStateOf<String?>(null) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            revealedAction = when (value) {
                SwipeToDismissBoxValue.StartToEnd -> "edit"
                SwipeToDismissBoxValue.EndToStart -> "delete"
                SwipeToDismissBoxValue.Settled -> null
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = plan.title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${plan.plannedDate} · ${plan.status.displayName()}",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
                DetailLine("Напомнить", plan.reminderDate ?: "не задано")
                DetailLine("Пробег", plan.targetMileage?.let { "$it км" } ?: "не указан")
                DetailLine("Комментарий", plan.comment ?: "нет")

                if (revealedAction == "edit") {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            revealedAction = null
                            onEditClick()
                        }
                    ) {
                        Text("Изменить")
                    }
                }

                if (revealedAction == "delete") {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            revealedAction = null
                            onDeleteClick()
                        }
                    ) {
                        Text("Удалить")
                    }
                }

                if (plan.status == MaintenancePlanStatus.PLANNED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusChange(MaintenancePlanStatus.DONE) }
                        ) {
                            Text("Выполнен")
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onStatusChange(MaintenancePlanStatus.CANCELLED) }
                        ) {
                            Text("Отменить")
                        }
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onStatusChange(MaintenancePlanStatus.PLANNED) }
                    ) {
                        Text("Вернуть в план")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF0F172A),
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        color = Color(0xFF64748B),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            color = Color(0xFF334155)
        )
    }
}

private fun VehicleType.displayName(): String =
    when (this) {
        VehicleType.MOTORCYCLE -> "Мотоцикл"
        VehicleType.CAR -> "Машина"
        VehicleType.SCOOTER -> "Скутер"
        VehicleType.BICYCLE -> "Велосипед"
        VehicleType.ATV -> "Квадроцикл"
        VehicleType.BOAT -> "Лодка"
        VehicleType.OTHER -> "Другое"
    }

private fun VehicleEventType.displayName(): String =
    when (this) {
        VehicleEventType.MAINTENANCE -> "Обслуживание"
        VehicleEventType.REPAIR -> "Ремонт"
        VehicleEventType.INSTALLED_PART -> "Установка детали"
        VehicleEventType.PURCHASE -> "Покупка"
        VehicleEventType.DIAGNOSTIC -> "Диагностика"
        VehicleEventType.WASH -> "Мойка"
        VehicleEventType.CUSTOM -> "Другое"
    }

private fun MaintenancePlanStatus.displayName(): String =
    when (this) {
        MaintenancePlanStatus.PLANNED -> "Запланирован"
        MaintenancePlanStatus.DONE -> "Выполнен"
        MaintenancePlanStatus.CANCELLED -> "Отменён"
    }

private fun demoSnapshot(): RemkaSnapshot {
    val rostovFolder = VehicleFolder(
        id = "demo-folder-rostov",
        name = "Ростов",
        createdAt = "2026-06-24"
    )
    val motorcycle = Vehicle(
        id = "demo-motorcycle",
        type = VehicleType.MOTORCYCLE,
        name = "Мой мотоцикл",
        manufacturer = "Honda",
        model = "CB400",
        year = 2007,
        registrationNumber = "A123BC",
        currentMileage = 42000,
        folderId = rostovFolder.id,
        updatedAt = "2026-06-24"
    )

    return RemkaSnapshot(
        folders = listOf(rostovFolder),
        vehicles = listOf(motorcycle),
        events = listOf(
            VehicleEvent(
                id = "demo-event-1",
                vehicleId = motorcycle.id,
                type = VehicleEventType.INSTALLED_PART,
                title = "Установил багажник",
                date = "2026-06-24",
                mileage = 42010,
                cost = 8500.0,
                shopName = "MotoParts",
                comment = "Позже проверить крепления."
            ),
            VehicleEvent(
                id = "demo-event-2",
                vehicleId = motorcycle.id,
                type = VehicleEventType.MAINTENANCE,
                title = "Поменял масло",
                date = "2026-06-24",
                mileage = 42100,
                cost = 3200.0,
                shopName = "Oil Market",
                comment = "Сливная пробка плохо закручивается."
            )
        ),
        plans = listOf(
            MaintenancePlan(
                id = "demo-plan-1",
                vehicleId = motorcycle.id,
                title = "Поменять лампочку",
                plannedDate = "2026-07-10",
                reminderDate = "2026-07-09",
                comment = "Перед покупкой проверить тип лампы."
            )
        )
    )
}

private fun todayText(): String =
    LocalDate.now().toString()

private fun String.isDateInRange(dateFrom: String, dateTo: String): Boolean {
    val start = dateFrom.trim()
    val end = dateTo.trim()

    return (start.isBlank() || this >= start) && (end.isBlank() || this <= end)
}

private fun String.onlyDigits(): String =
    filter { char -> char.isDigit() }

private fun String.onlyDecimalNumber(): String =
    filter { char -> char.isDigit() || char == '.' || char == ',' }

@Composable
private fun VehicleIcon(type: VehicleType) {
    val label = when (type) {
        VehicleType.MOTORCYCLE -> "M"
        VehicleType.CAR -> "A"
        VehicleType.SCOOTER -> "S"
        VehicleType.BICYCLE -> "B"
        VehicleType.ATV -> "Q"
        VehicleType.BOAT -> "L"
        VehicleType.OTHER -> "?"
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE0F2FE)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0369A1)
        )
    }
}
