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
import androidx.compose.material3.Text
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
import com.remka.domain.VehicleType
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
    fun saveState() {
        store.save(
            RemkaSnapshot(
                vehicles = vehicles.toList(),
                events = events.toList(),
                plans = plans.toList()
            )
        )
    }

    when (screen) {
        RemkaScreen.VehicleList -> VehicleListScreen(
            vehicles = vehicles,
            onAddVehicleClick = { screen = RemkaScreen.AddVehicle },
            onVehicleClick = { vehicle ->
                selectedVehicleId = vehicle.id
                screen = RemkaScreen.VehicleDetails
            },
            onEditVehicleClick = { vehicle ->
                selectedVehicleId = vehicle.id
                screen = RemkaScreen.EditVehicle
            },
            onDeleteVehicleClick = { vehicle ->
                vehicles.removeAll { existingVehicle -> existingVehicle.id == vehicle.id }
                events.removeAll { event -> event.vehicleId == vehicle.id }
                plans.removeAll { plan -> plan.vehicleId == vehicle.id }
                if (selectedVehicleId == vehicle.id) {
                    selectedVehicleId = null
                }
                saveState()
            }
        )

        RemkaScreen.AddVehicle -> AddVehicleScreen(
            vehicleToEdit = null,
            onBack = { screen = RemkaScreen.VehicleList },
            onSave = { vehicle ->
                vehicles.add(vehicle)
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
                    onBack = { screen = RemkaScreen.VehicleList },
                    onSave = { vehicle ->
                        val index = vehicles.indexOfFirst { existingVehicle -> existingVehicle.id == vehicle.id }
                        if (index != -1) {
                            vehicles[index] = vehicle
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
                    onBack = { screen = RemkaScreen.VehicleList },
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
                        saveState()
                    },
                    onEditPlanClick = { plan ->
                        selectedPlanId = plan.id
                        screen = RemkaScreen.EditPlan
                    },
                    onDeletePlanClick = { plan ->
                        plans.removeAll { existingPlan -> existingPlan.id == plan.id }
                        saveState()
                    },
                    onPlanStatusChange = { plan, status ->
                        val index = plans.indexOfFirst { existingPlan -> existingPlan.id == plan.id }
                        if (index != -1) {
                            plans[index] = plan.copy(status = status)
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
                            saveState()
                        }
                        screen = RemkaScreen.VehicleDetails
                    }
                )
            }
        }
    }
}

private enum class RemkaScreen {
    VehicleList,
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
    onAddVehicleClick: () -> Unit,
    onVehicleClick: (Vehicle) -> Unit,
    onEditVehicleClick: (Vehicle) -> Unit,
    onDeleteVehicleClick: (Vehicle) -> Unit
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

            Button(
                onClick = onAddVehicleClick
            ) {
                Text("Добавить")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(vehicles, key = { vehicle -> vehicle.id }) { vehicle ->
                VehicleCard(
                    vehicle = vehicle,
                    onClick = { onVehicleClick(vehicle) },
                    onEditClick = { onEditVehicleClick(vehicle) },
                    onDeleteClick = { onDeleteVehicleClick(vehicle) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleScreen(
    vehicleToEdit: Vehicle?,
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
                            currentMileage = mileage.toLongOrNull()
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

                OutlinedButton(onClick = onBack) {
                    Text("Назад")
                }
            }
        }

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
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
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
            }

            Text(
                text = vehicle.registrationNumber ?: "без номера",
                color = Color(0xFF334155),
                fontSize = 13.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onEditClick
            ) {
                Text("Изменить")
            }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onDeleteClick
            ) {
                Text("Удалить")
            }
        }
    }
}

@Composable
private fun EventCard(
    event: VehicleEvent,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onEditClick
                ) {
                    Text("Изменить")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDeleteClick
                ) {
                    Text("Удалить")
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onEditClick
                ) {
                    Text("Изменить")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDeleteClick
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
    val motorcycle = Vehicle(
        id = "demo-motorcycle",
        type = VehicleType.MOTORCYCLE,
        name = "Мой мотоцикл",
        manufacturer = "Honda",
        model = "CB400",
        year = 2007,
        registrationNumber = "A123BC",
        currentMileage = 42000
    )

    return RemkaSnapshot(
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
