package com.remka.mobile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remka.data.RemkaSnapshot
import com.remka.domain.MaintenancePlan
import com.remka.domain.MaintenancePlanStatus
import com.remka.domain.SharedAccess
import com.remka.domain.UserAccount
import com.remka.domain.Vehicle
import com.remka.domain.VehicleEvent
import com.remka.domain.VehicleEventType
import com.remka.domain.VehicleFolder
import com.remka.domain.VehicleType
import com.remka.domain.WorkAssignment
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID

private val PremiumInk = Color(0xFF17211D)
private val PremiumMuted = Color(0xFF64736B)
private val PremiumLine = Color(0xFFE3E9E4)
private val PremiumCard = Color(0xF7FFFFFF)
private val PremiumCardSoft = Color(0xEFFFFFFF)
private val PremiumAccent = Color(0xFF0F7A5A)
private val PremiumAccentSoft = Color(0xFFE6F4EE)
private val PremiumGold = Color(0xFFC8943F)
private val PremiumGoldSoft = Color(0xFFFFF4DD)
private val PremiumDanger = Color(0xFFB84A4A)
private val PremiumBgTop = Color(0xFFF7FAF7)
private val PremiumBgBottom = Color(0xFFEFF4F0)

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
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PremiumAccent,
            onPrimary = Color.White,
            secondary = PremiumGold,
            surface = PremiumCard,
            onSurface = PremiumInk,
            background = PremiumBgTop,
            onBackground = PremiumInk,
            outline = PremiumLine,
            error = PremiumDanger
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            PremiumBackground {
                content()
            }
        }
    }
}

@Composable
private fun PremiumBackground(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "premium-background")
    val driftOne by transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift-one"
    )
    val driftTwo by transition.animateFloat(
        initialValue = 20f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift-two"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PremiumBgTop, PremiumBgBottom)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-84).dp + driftOne.dp, y = 48.dp)
                .size(220.dp)
                .clip(RoundedCornerShape(110.dp))
                .background(PremiumAccent.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .offset(x = 228.dp + driftTwo.dp, y = 18.dp)
                .size(160.dp)
                .clip(RoundedCornerShape(80.dp))
                .background(PremiumGold.copy(alpha = 0.14f))
        )
        Box(
            modifier = Modifier
                .offset(x = 248.dp - driftOne.dp, y = 520.dp)
                .size(190.dp)
                .clip(RoundedCornerShape(95.dp))
                .background(PremiumCard.copy(alpha = 0.42f))
        )

        content()
    }
}

@Composable
private fun RemkaApp() {
    val context = LocalContext.current
    val store = remember {
        AndroidRemkaStore(context.filesDir.resolve("remka-data.json"))
    }
    val syncClient = remember {
        RemkaSyncClient(
            baseUrl = BuildConfig.REMKA_SYNC_URL,
            token = BuildConfig.REMKA_SYNC_TOKEN
        )
    }
    val mainHandler = remember {
        Handler(Looper.getMainLooper())
    }
    val initialSnapshot = remember {
        store.load() ?: demoSnapshot()
    }
    var screen by remember { mutableStateOf(RemkaScreen.VehicleList) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var selectedPlanId by remember { mutableStateOf<String?>(null) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteTitle by remember { mutableStateOf<String?>(null) }
    var pendingDeleteText by remember { mutableStateOf<String?>(null) }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var currentUser by remember { mutableStateOf(initialSnapshot.currentUser) }
    var pendingSyncVersion by remember { mutableStateOf(initialSnapshot.pendingSyncVersion) }
    var lastSyncedVersion by remember { mutableStateOf(initialSnapshot.lastSyncedVersion) }
    val knownUsers = remember {
        mutableStateListOf<UserAccount>().apply {
            addAll(initialSnapshot.knownUsers)
            initialSnapshot.currentUser?.let { user ->
                if (none { existingUser -> existingUser.id == user.id }) {
                    add(user)
                }
            }
        }
    }
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
        pendingSyncVersion += 1
        val snapshot = RemkaSnapshot(
            vehicles = vehicles.toList(),
            events = events.toList(),
            plans = plans.toList(),
            folders = folders.toList(),
            currentUser = currentUser,
            knownUsers = knownUsers.toList(),
            pendingSyncVersion = pendingSyncVersion,
            lastSyncedVersion = lastSyncedVersion
        )
        store.save(snapshot)

        val accountId = currentUser?.id
        if (accountId != null && syncClient.isConfigured()) {
            val encryptedPayload = store.encryptSnapshot(snapshot)
            val syncVersion = pendingSyncVersion
            Thread {
                if (syncClient.uploadEncryptedSnapshot(accountId, encryptedPayload)) {
                    mainHandler.post {
                        lastSyncedVersion = syncVersion
                        store.save(snapshot.copy(lastSyncedVersion = syncVersion))
                    }
                }
            }.start()
        }
    }
    fun touchVehicle(vehicleId: String) {
        val index = vehicles.indexOfFirst { vehicle -> vehicle.id == vehicleId }
        if (index != -1) {
            vehicles[index] = vehicles[index].copy(updatedAt = todayText())
        }
    }
    fun isFolderVisible(folder: VehicleFolder): Boolean {
        val userId = currentUser?.id ?: return true
        return folder.ownerUserId == null ||
            folder.ownerUserId == userId ||
            folder.sharedWith.any { access -> access.userId == userId }
    }
    fun isVehicleVisible(vehicle: Vehicle): Boolean {
        val userId = currentUser?.id ?: return true
        if (vehicle.hiddenForUserIds.contains(userId)) {
            return false
        }

        val folder = folders.firstOrNull { existingFolder -> existingFolder.id == vehicle.folderId }

        return vehicle.ownerUserId == null ||
            vehicle.ownerUserId == userId ||
            vehicle.sharedWith.any { access -> access.userId == userId } ||
            folder?.sharedWith?.any { access -> access.userId == userId } == true ||
            folder?.ownerUserId == userId
    }
    fun rememberUser(userId: String): UserAccount {
        val normalizedId = userId.trim()
        val existingUser = knownUsers.firstOrNull { user -> user.id == normalizedId }
        if (existingUser != null) {
            return existingUser
        }

        val newUser = UserAccount(
            id = normalizedId,
            email = "$normalizedId@shared.remka.local",
            displayName = normalizedId
        )
        knownUsers.add(newUser)
        return newUser
    }
    fun actorName(userId: String?): String =
        userId?.let { id ->
            knownUsers.firstOrNull { user -> user.id == id }?.displayName ?: id
        } ?: "Без профиля"
    fun requestDelete(title: String, text: String, action: () -> Unit) {
        pendingDeleteTitle = title
        pendingDeleteText = text
        pendingDeleteAction = action
    }
    fun navigateBack() {
        when (screen) {
            RemkaScreen.VehicleList -> Unit
            RemkaScreen.Journal,
            RemkaScreen.AddChoice,
            RemkaScreen.AddFolder,
            RemkaScreen.EditFolder,
            RemkaScreen.FolderDetails,
            RemkaScreen.AddVehicle,
            RemkaScreen.EditVehicle,
            RemkaScreen.Profile,
            RemkaScreen.ShareFolder -> {
                screen = RemkaScreen.VehicleList
            }

            RemkaScreen.VehicleDetails -> {
                val selectedVehicle = vehicles.firstOrNull { vehicle -> vehicle.id == selectedVehicleId }
                if (selectedVehicle?.folderId == null) {
                    screen = RemkaScreen.VehicleList
                } else {
                    selectedFolderId = selectedVehicle.folderId
                    screen = RemkaScreen.FolderDetails
                }
            }

            RemkaScreen.AddEvent,
            RemkaScreen.EditEvent,
            RemkaScreen.AddPlan,
            RemkaScreen.EditPlan,
            RemkaScreen.ShareVehicle -> {
                screen = RemkaScreen.VehicleDetails
            }
        }
    }

    BackHandler(enabled = screen != RemkaScreen.VehicleList) {
        navigateBack()
    }

    pendingDeleteAction?.let { action ->
        ConfirmDeleteDialog(
            title = pendingDeleteTitle ?: "Удалить?",
            text = pendingDeleteText ?: "Это действие нельзя отменить.",
            onDismiss = {
                pendingDeleteTitle = null
                pendingDeleteText = null
                pendingDeleteAction = null
            },
            onConfirm = {
                action()
                pendingDeleteTitle = null
                pendingDeleteText = null
                pendingDeleteAction = null
            }
        )
    }

    when (screen) {
        RemkaScreen.VehicleList -> VehicleListScreen(
            vehicles = vehicles
                .filter { vehicle -> vehicle.folderId == null && isVehicleVisible(vehicle) }
                .sortedByDescending { vehicle -> vehicle.updatedAt },
            folders = folders.sortedWith(
                compareByDescending<VehicleFolder> { folder -> folder.isPinned }
                    .thenBy { folder -> folder.name.lowercase() }
            ).filter { folder -> isFolderVisible(folder) },
            folderVehicleCounts = vehicles
                .filter { vehicle -> isVehicleVisible(vehicle) }
                .groupingBy { vehicle -> vehicle.folderId }
                .eachCount(),
            currentUser = currentUser,
            onProfileClick = { screen = RemkaScreen.Profile },
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
            onShareFolder = { folder ->
                selectedFolderId = folder.id
                screen = RemkaScreen.ShareFolder
            },
            onDeleteFolder = { folder ->
                requestDelete(
                    title = "Удалить папку?",
                    text = "Папка «${folder.name}» будет удалена, а техника из неё вернётся в общий список."
                ) {
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
                }
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
            },
            onEditVehicle = { vehicle ->
                selectedVehicleId = vehicle.id
                screen = RemkaScreen.EditVehicle
            },
            onShareVehicle = { vehicle ->
                selectedVehicleId = vehicle.id
                screen = RemkaScreen.ShareVehicle
            },
            onDeleteVehicle = { vehicle ->
                requestDelete(
                    title = "Удалить транспорт?",
                    text = "«${vehicle.name}» и связанные записи будут удалены."
                ) {
                    vehicles.removeAll { existingVehicle -> existingVehicle.id == vehicle.id }
                    events.removeAll { event -> event.vehicleId == vehicle.id }
                    plans.removeAll { plan -> plan.vehicleId == vehicle.id }
                    saveState()
                }
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
                        createdAt = todayText(),
                        ownerUserId = currentUser?.id
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
                        .filter { vehicle -> vehicle.folderId == selectedFolder.id && isVehicleVisible(vehicle) }
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
                vehicles.add(
                    vehicle.copy(
                        updatedAt = todayText(),
                        ownerUserId = currentUser?.id
                    )
                )
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
                            vehicles[index] = vehicle.copy(
                                updatedAt = todayText(),
                                ownerUserId = vehicles[index].ownerUserId ?: currentUser?.id,
                                sharedWith = vehicles[index].sharedWith,
                                hiddenForUserIds = vehicles[index].hiddenForUserIds
                            )
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
                    onShareVehicleClick = { screen = RemkaScreen.ShareVehicle },
                    onDeleteVehicleClick = {
                        requestDelete(
                            title = "Удалить транспорт?",
                            text = "«${selectedVehicle.name}» и связанные записи будут удалены."
                        ) {
                            vehicles.removeAll { vehicle -> vehicle.id == selectedVehicle.id }
                            events.removeAll { event -> event.vehicleId == selectedVehicle.id }
                            plans.removeAll { plan -> plan.vehicleId == selectedVehicle.id }
                            selectedVehicleId = null
                            saveState()
                            screen = RemkaScreen.VehicleList
                        }
                    },
                    actorName = { userId -> actorName(userId) },
                    onEditEventClick = { event ->
                        selectedEventId = event.id
                        screen = RemkaScreen.EditEvent
                    },
                    onDeleteEventClick = { event ->
                        requestDelete(
                            title = "Удалить запись?",
                            text = "Запись «${event.title}» будет удалена из истории."
                        ) {
                            events.removeAll { existingEvent -> existingEvent.id == event.id }
                            touchVehicle(selectedVehicle.id)
                            saveState()
                        }
                    },
                    onEditPlanClick = { plan ->
                        selectedPlanId = plan.id
                        screen = RemkaScreen.EditPlan
                    },
                    onDeletePlanClick = { plan ->
                        requestDelete(
                            title = "Удалить план?",
                            text = "План «${plan.title}» будет удалён."
                        ) {
                            plans.removeAll { existingPlan -> existingPlan.id == plan.id }
                            touchVehicle(selectedVehicle.id)
                            saveState()
                        }
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
                        events.add(event.copy(createdByUserId = currentUser?.id))
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
                            events[index] = event.copy(
                                createdByUserId = events[index].createdByUserId ?: currentUser?.id
                            )
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
                        plans.add(plan.copy(createdByUserId = currentUser?.id))
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
                            plans[index] = plan.copy(
                                createdByUserId = plans[index].createdByUserId ?: currentUser?.id
                            )
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

        RemkaScreen.Profile -> ProfileScreen(
            currentUser = currentUser,
            onBack = { screen = RemkaScreen.VehicleList },
            onSignIn = { email, password ->
                val normalizedEmail = email.normalizedEmail()
                val existingUser = knownUsers.firstOrNull { existing ->
                    existing.email.normalizedEmail() == normalizedEmail
                }
                when {
                    existingUser == null -> "Аккаунт с таким email не найден"
                    !existingUser.passwordMatches(password) -> "Неверный пароль"
                    else -> {
                        currentUser = existingUser
                        saveState()
                        screen = RemkaScreen.VehicleList
                        null
                    }
                }
            },
            onRegister = { email, password ->
                val normalizedEmail = email.normalizedEmail()
                val existingUser = knownUsers.firstOrNull { existing ->
                    existing.email.normalizedEmail() == normalizedEmail
                }
                if (existingUser != null) {
                    "Аккаунт с таким email уже есть"
                } else {
                    val user = userAccountFromEmail(normalizedEmail, password)
                    currentUser = user
                    knownUsers.add(user)
                    saveState()
                    screen = RemkaScreen.VehicleList
                    null
                }
            },
            onChangePassword = { password ->
                currentUser?.let { user ->
                    val updatedUser = user.copy(passwordHash = passwordFingerprint(password, user.email))
                    currentUser = updatedUser
                    val index = knownUsers.indexOfFirst { existingUser -> existingUser.id == updatedUser.id }
                    if (index == -1) {
                        knownUsers.add(updatedUser)
                    } else {
                        knownUsers[index] = updatedUser
                    }
                    saveState()
                }
            },
            onSignOut = {
                currentUser = null
                saveState()
                screen = RemkaScreen.VehicleList
            }
        )

        RemkaScreen.ShareFolder -> {
            val folder = folders.firstOrNull { existingFolder -> existingFolder.id == selectedFolderId }

            if (folder == null) {
                screen = RemkaScreen.VehicleList
            } else {
                AccessScreen(
                    title = "Доступ к папке",
                    subtitle = folder.name,
                    sharedWith = folder.sharedWith,
                    knownUsers = knownUsers,
                    onBack = { screen = RemkaScreen.VehicleList },
                    onAddUser = { userId ->
                        val user = rememberUser(userId)
                        val index = folders.indexOfFirst { existingFolder -> existingFolder.id == folder.id }
                        if (index != -1 && folders[index].sharedWith.none { access -> access.userId == user.id }) {
                            folders[index] = folders[index].copy(
                                sharedWith = folders[index].sharedWith + SharedAccess(userId = user.id)
                            )
                            saveState()
                        }
                    },
                    onRemoveUser = { userId ->
                        val index = folders.indexOfFirst { existingFolder -> existingFolder.id == folder.id }
                        if (index != -1) {
                            folders[index] = folders[index].copy(
                                sharedWith = folders[index].sharedWith.filterNot { access -> access.userId == userId }
                            )
                            saveState()
                        }
                    }
                )
            }
        }

        RemkaScreen.ShareVehicle -> {
            val vehicle = vehicles.firstOrNull { existingVehicle -> existingVehicle.id == selectedVehicleId }

            if (vehicle == null) {
                screen = RemkaScreen.VehicleList
            } else {
                val folderAccess = folders
                    .firstOrNull { folder -> folder.id == vehicle.folderId }
                    ?.sharedWith
                    .orEmpty()
                val visibleAccess = (vehicle.sharedWith + folderAccess)
                    .filterNot { access -> vehicle.hiddenForUserIds.contains(access.userId) }
                    .distinctBy { access -> access.userId }

                AccessScreen(
                    title = "Доступ к технике",
                    subtitle = vehicle.name,
                    sharedWith = visibleAccess,
                    knownUsers = knownUsers,
                    onBack = { screen = RemkaScreen.VehicleDetails },
                    onAddUser = { userId ->
                        val user = rememberUser(userId)
                        val index = vehicles.indexOfFirst { existingVehicle -> existingVehicle.id == vehicle.id }
                        if (index != -1) {
                            val currentVehicle = vehicles[index]
                            vehicles[index] = currentVehicle.copy(
                                sharedWith = if (currentVehicle.sharedWith.any { access -> access.userId == user.id }) {
                                    currentVehicle.sharedWith
                                } else {
                                    currentVehicle.sharedWith + SharedAccess(userId = user.id)
                                },
                                hiddenForUserIds = currentVehicle.hiddenForUserIds.filterNot { hiddenUserId -> hiddenUserId == user.id },
                                updatedAt = todayText()
                            )
                            saveState()
                        }
                    },
                    onRemoveUser = { userId ->
                        val index = vehicles.indexOfFirst { existingVehicle -> existingVehicle.id == vehicle.id }
                        if (index != -1) {
                            val currentVehicle = vehicles[index]
                            vehicles[index] = currentVehicle.copy(
                                sharedWith = currentVehicle.sharedWith.filterNot { access -> access.userId == userId },
                                hiddenForUserIds = (currentVehicle.hiddenForUserIds + userId).distinct(),
                                updatedAt = todayText()
                            )
                            saveState()
                        }
                    }
                )
            }
        }
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
    EditPlan,
    Profile,
    ShareFolder,
    ShareVehicle
}

@Composable
private fun PremiumSwipeActions(
    startLabel: String,
    endLabel: String,
    onStartSwipe: () -> Unit,
    onEndSwipe: () -> Unit,
    content: @Composable () -> Unit
) {
    val startAction = SwipeAction(
        icon = { FishEyeActionLabel(text = startLabel, color = PremiumAccent) },
        background = PremiumAccentSoft,
        isUndo = true,
        onSwipe = onStartSwipe
    )
    val endAction = SwipeAction(
        icon = { FishEyeActionLabel(text = endLabel, color = PremiumDanger) },
        background = PremiumGoldSoft,
        isUndo = true,
        onSwipe = onEndSwipe
    )

    SwipeableActionsBox(
        startActions = listOf(startAction),
        endActions = listOf(endAction)
    ) {
        content()
    }
}

@Composable
private fun FishEyeActionLabel(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
            .graphicsLayer {
                scaleX = 1.12f
                scaleY = 1.08f
                shadowElevation = 10f
            }
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.88f), color.copy(alpha = 0.20f))
                )
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun ProfileScreen(
    currentUser: UserAccount?,
    onBack: () -> Unit,
    onSignIn: (String, String) -> String?,
    onRegister: (String, String) -> String?,
    onChangePassword: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    var authMessage by remember { mutableStateOf<String?>(null) }
    val canSubmitAuth = email.contains("@") &&
        email.substringAfter("@").contains(".") &&
        password.length >= 6
    val canChangePassword = currentUser != null &&
        newPassword.length >= 6 &&
        newPassword == repeatedPassword

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
                        text = "Профиль",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumInk
                    )
                    Text(
                        text = "Email и ID для совместного доступа",
                        color = PremiumMuted
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
                colors = CardDefaults.cardColors(containerColor = PremiumCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = currentUser?.displayName ?: "Вход не выполнен",
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumInk
                    )
                    Text(
                        text = currentUser?.id?.let { accountId -> "ID: $accountId" }
                            ?: "После входа здесь появится ID, который можно дать другому человеку.",
                        color = PremiumMuted,
                        fontSize = 13.sp
                    )
                    if (currentUser != null) {
                        Text(
                            text = "Этот ID нужен, чтобы открыть доступ к папке или конкретной технике.",
                            color = PremiumMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = email,
                onValueChange = { email = it },
                label = { Text("Электронная почта") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = {
                    password = it
                    authMessage = null
                },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                supportingText = {
                    if (password.isNotBlank() && password.length < 6) {
                        Text("Минимум 6 символов")
                    }
                }
            )
        }

        if (authMessage != null) {
            item {
                Text(
                    text = authMessage.orEmpty(),
                    color = PremiumDanger,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = canSubmitAuth,
                    onClick = {
                        authMessage = onSignIn(email, password)
                        if (authMessage == null) {
                            password = ""
                        }
                    }
                ) {
                    Text("Войти")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = canSubmitAuth && currentUser == null,
                    onClick = {
                        authMessage = onRegister(email, password)
                        if (authMessage == null) {
                            password = ""
                        }
                    }
                ) {
                    Text("Создать")
                }
            }
        }

        if (currentUser != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = PremiumCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Пароль",
                            fontWeight = FontWeight.SemiBold,
                            color = PremiumInk
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Новый пароль") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = repeatedPassword,
                            onValueChange = { repeatedPassword = it },
                            label = { Text("Повторить пароль") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            supportingText = {
                                if (repeatedPassword.isNotBlank() && newPassword != repeatedPassword) {
                                    Text("Пароли не совпадают")
                                }
                            }
                        )

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canChangePassword,
                            onClick = {
                                onChangePassword(newPassword)
                                newPassword = ""
                                repeatedPassword = ""
                            }
                        ) {
                            Text("Поменять пароль")
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSignOut
                ) {
                    Text("Выйти")
                }
            }
        }
    }
}

@Composable
private fun AccessScreen(
    title: String,
    subtitle: String,
    sharedWith: List<SharedAccess>,
    knownUsers: List<UserAccount>,
    onBack: () -> Unit,
    onAddUser: (String) -> Unit,
    onRemoveUser: (String) -> Unit
) {
    var accountId by remember { mutableStateOf("") }
    val usersById = knownUsers.associateBy { user -> user.id }

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
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumInk
                    )
                    Text(
                        text = subtitle,
                        color = PremiumMuted
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
                value = accountId,
                onValueChange = { accountId = it },
                label = { Text("ID аккаунта") },
                singleLine = true
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = accountId.isNotBlank(),
                onClick = {
                    onAddUser(accountId)
                    accountId = ""
                }
            ) {
                Text("Добавить доступ")
            }
        }

        item {
            SectionTitle("Участники")
        }

        if (sharedWith.isEmpty()) {
            item {
                EmptyText("Доступ пока никому не открыт")
            }
        } else {
            items(sharedWith, key = { access -> access.userId }) { access ->
                AccessRow(
                    access = access,
                    user = usersById[access.userId],
                    onRemoveClick = { onRemoveUser(access.userId) }
                )
            }
        }
    }
}

@Composable
private fun AccessRow(
    access: SharedAccess,
    user: UserAccount?,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PremiumCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.displayName ?: access.userId,
                    fontWeight = FontWeight.SemiBold,
                    color = PremiumInk
                )
                Text(
                    text = user?.email ?: "ID: ${access.userId}",
                    color = PremiumMuted,
                    fontSize = 13.sp
                )
            }

            OutlinedButton(onClick = onRemoveClick) {
                Text("Убрать")
            }
        }
    }
}

@Composable
private fun VehicleListScreen(
    vehicles: List<Vehicle>,
    folders: List<VehicleFolder>,
    folderVehicleCounts: Map<String?, Int>,
    currentUser: UserAccount?,
    onProfileClick: () -> Unit,
    onAddClick: () -> Unit,
    onJournalClick: () -> Unit,
    onFolderClick: (VehicleFolder) -> Unit,
    onRenameFolder: (VehicleFolder) -> Unit,
    onShareFolder: (VehicleFolder) -> Unit,
    onDeleteFolder: (VehicleFolder) -> Unit,
    onTogglePinFolder: (VehicleFolder) -> Unit,
    onVehicleClick: (Vehicle) -> Unit,
    onEditVehicle: (Vehicle) -> Unit,
    onShareVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Remka",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumInk
                )
                Text(
                    text = "Транспорт и история работ",
                    color = PremiumMuted
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onProfileClick) {
                        Text(currentUser?.displayName?.take(1)?.uppercase()?.ifBlank { "@" } ?: "@")
                    }

                    OutlinedButton(onClick = onJournalClick) {
                        Text("Ж")
                    }

                    Button(onClick = onAddClick) {
                        Text("+")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(folders, key = { folder -> folder.id }) { folder ->
                FolderCard(
                    folder = folder,
                    vehicleCount = folderVehicleCounts[folder.id] ?: 0,
                    onClick = { onFolderClick(folder) },
                    onRenameClick = { onRenameFolder(folder) },
                    onShareClick = { onShareFolder(folder) },
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
                    VehicleSwipeCard(
                        vehicle = vehicle,
                        folderName = null,
                        onClick = { onVehicleClick(vehicle) },
                        onEditClick = { onEditVehicle(vehicle) },
                        onShareClick = { onShareVehicle(vehicle) },
                        onDeleteClick = { onDeleteVehicle(vehicle) }
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
                    color = PremiumInk
                )
                Text(
                    text = "Выбери, что создаём",
                    color = PremiumMuted
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
        colors = CardDefaults.cardColors(containerColor = PremiumCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = PremiumInk
            )
            Text(
                text = subtitle,
                color = PremiumMuted,
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
                        color = PremiumInk
                    )
                    Text(
                        text = "Папки помогают не смешивать технику",
                        color = PremiumMuted
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
                        color = PremiumInk
                    )
                    Text(
                        text = if (folder.isPinned) "Закреплена" else "Папка техники",
                        color = PremiumMuted
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
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTogglePinClick: () -> Unit
) {
    var revealedAction by remember { mutableStateOf<String?>(null) }
    PremiumSwipeActions(
        startLabel = "Ещё",
        endLabel = "Удалить",
        onStartSwipe = { revealedAction = "manage" },
        onEndSwipe = onDeleteClick
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = PremiumCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    Box(
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PremiumGoldSoft, PremiumAccentSoft)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PremiumAccent.copy(alpha = 0.88f))
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (folder.isPinned) "${folder.name} *" else folder.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PremiumInk
                        )
                        Text(
                            text = "$vehicleCount ед. техники",
                            color = PremiumMuted,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "›",
                        color = PremiumMuted,
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
                            Text("Имя")
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                revealedAction = null
                                onShareClick()
                            }
                        ) {
                            Text("Доступ")
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                revealedAction = null
                                onTogglePinClick()
                            }
                        ) {
                            Text(if (folder.isPinned) "Откр." else "Закр.")
                        }
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
                        color = PremiumInk
                    )
                    Text(
                        text = "Что делали по датам",
                        color = PremiumMuted
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
                colors = CardDefaults.cardColors(containerColor = PremiumCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Период",
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumInk
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
        colors = CardDefaults.cardColors(containerColor = PremiumCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = entry.title,
                fontWeight = FontWeight.SemiBold,
                color = PremiumInk
            )
            Text(
                text = "${entry.date} · ${entry.vehicleName} · ${entry.type}",
                color = PremiumMuted,
                fontSize = 13.sp
            )
            Text(
                text = entry.details,
                color = PremiumInk
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
                        color = PremiumInk
                    )
                    Text(
                        text = "Заполни основные данные",
                        color = PremiumMuted
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
                maxLines = 2,
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
    var assignmentsText by remember {
        mutableStateOf(eventToEdit?.assignments?.toAssignmentText() ?: "")
    }
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
                        color = PremiumInk
                    )
                    Text(
                        text = vehicle.name,
                        color = PremiumMuted
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
                maxLines = 2
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
                value = assignmentsText,
                onValueChange = { assignmentsText = it },
                label = { Text("Кто что сделал") },
                supportingText = { Text("Например: alex: заменил сливную пробку") },
                minLines = 2
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
                            comment = comment.trim().ifBlank { null },
                            assignments = assignmentsText.toAssignments()
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
    var assignmentsText by remember {
        mutableStateOf(planToEdit?.assignments?.toAssignmentText() ?: "")
    }
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
                        color = PremiumInk
                    )
                    Text(
                        text = vehicle.name,
                        color = PremiumMuted
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
                maxLines = 2
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
                value = assignmentsText,
                onValueChange = { assignmentsText = it },
                label = { Text("Кто что должен сделать") },
                supportingText = { Text("Например: alex: купить лампочку") },
                minLines = 2
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
                            assignments = assignmentsText.toAssignments(),
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
    onShareVehicleClick: () -> Unit,
    onDeleteVehicleClick: () -> Unit,
    actorName: (String?) -> String,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vehicle.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumInk
                    )
                    Text(
                        text = vehicle.type.displayName(),
                        color = PremiumMuted
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
                        Text("←")
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
                        onClick = onShareVehicleClick
                    ) {
                        Text("Доступ")
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
                colors = CardDefaults.cardColors(containerColor = PremiumCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Сводка",
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumInk
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
                    authorName = actorName(event.createdByUserId),
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
                    authorName = actorName(plan.createdByUserId),
                    onEditClick = { onEditPlanClick(plan) },
                    onDeleteClick = { onDeletePlanClick(plan) },
                    onStatusChange = { status -> onPlanStatusChange(plan, status) }
                )
            }
        }
    }
}

@Composable
private fun VehicleSwipeCard(
    vehicle: Vehicle,
    folderName: String?,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var revealedAction by remember { mutableStateOf<String?>(null) }

    PremiumSwipeActions(
        startLabel = "Ещё",
        endLabel = "Удалить",
        onStartSwipe = { revealedAction = "manage" },
        onEndSwipe = onDeleteClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VehicleCard(
                vehicle = vehicle,
                folderName = folderName,
                onClick = onClick
            )

            if (revealedAction == "manage") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            revealedAction = null
                            onEditClick()
                        }
                    ) {
                        Text("Изменить")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            revealedAction = null
                            onShareClick()
                        }
                    ) {
                        Text("Доступ")
                    }
                }
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
        colors = CardDefaults.cardColors(containerColor = PremiumCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    color = PremiumInk
                )

                Text(
                    text = listOfNotNull(vehicle.manufacturer, vehicle.model, vehicle.year?.toString())
                        .joinToString(" ")
                        .ifBlank { "Модель не указана" },
                    color = PremiumMuted
                )

                Text(
                    text = "Пробег: ${vehicle.currentMileage ?: "не указан"} км",
                    color = PremiumMuted,
                    fontSize = 13.sp
                )

                Text(
                    text = listOfNotNull(
                        folderName?.let { folder -> "Папка: $folder" },
                        vehicle.updatedAt.ifBlank { null }?.let { date -> "Изменено: $date" }
                    ).joinToString(" · ").ifBlank { "Без папки" },
                    color = PremiumMuted,
                    fontSize = 13.sp
                )
            }

            Text(
                text = vehicle.registrationNumber ?: "без номера",
                color = PremiumInk,
                fontSize = 13.sp
            )
        }

    }
}

@Composable
private fun EventCard(
    event: VehicleEvent,
    authorName: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    PremiumSwipeActions(
        startLabel = "Изменить",
        endLabel = "Удалить",
        onStartSwipe = onEditClick,
        onEndSwipe = onDeleteClick
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = PremiumCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.SemiBold,
                    color = PremiumInk
                )
                Text(
                    text = "${event.date} · ${event.type.displayName()}",
                    color = PremiumMuted,
                    fontSize = 13.sp
                )
                Text(
                    text = "Добавил: $authorName",
                    color = PremiumMuted,
                    fontSize = 13.sp
                )
                DetailLine("Пробег", event.mileage?.let { "$it км" } ?: "не указан")
                DetailLine("Стоимость", event.cost?.let { "$it" } ?: "не указана")
                if (event.assignments.isNotEmpty()) {
                    Text(
                        text = event.assignments.toAssignmentText(),
                        color = PremiumInk,
                        fontSize = 13.sp
                    )
                }
                DetailLine("Комментарий", event.comment ?: "нет")
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: MaintenancePlan,
    authorName: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStatusChange: (MaintenancePlanStatus) -> Unit
) {
    PremiumSwipeActions(
        startLabel = "Изменить",
        endLabel = "Удалить",
        onStartSwipe = onEditClick,
        onEndSwipe = onDeleteClick
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = PremiumCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = plan.title,
                    fontWeight = FontWeight.SemiBold,
                    color = PremiumInk
                )
                Text(
                    text = "${plan.plannedDate} · ${plan.status.displayName()}",
                    color = PremiumMuted,
                    fontSize = 13.sp
                )
                Text(
                    text = "Создал: $authorName",
                    color = PremiumMuted,
                    fontSize = 13.sp
                )
                DetailLine("Напомнить", plan.reminderDate ?: "не задано")
                DetailLine("Пробег", plan.targetMileage?.let { "$it км" } ?: "не указан")
                if (plan.assignments.isNotEmpty()) {
                    Text(
                        text = plan.assignments.toAssignmentText(),
                        color = PremiumInk,
                        fontSize = 13.sp
                    )
                }
                DetailLine("Комментарий", plan.comment ?: "нет")

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
        color = PremiumInk,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        color = PremiumMuted,
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
            color = PremiumMuted
        )
        Text(
            text = value,
            color = PremiumInk
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

private fun userAccountFromEmail(email: String, password: String): UserAccount {
    val normalizedEmail = email.normalizedEmail()
    val baseId = normalizedEmail.substringBefore("@")
        .filter { char -> char.isLetterOrDigit() || char == '.' || char == '_' || char == '-' }
        .ifBlank { "user" }

    return UserAccount(
        id = "usr-${UUID.randomUUID().toString().take(8)}",
        email = normalizedEmail,
        displayName = baseId.replaceFirstChar { char -> char.uppercase() },
        passwordHash = passwordFingerprint(password, normalizedEmail)
    )
}

private fun String.normalizedEmail(): String =
    trim().lowercase()

private fun UserAccount.passwordMatches(password: String): Boolean {
    val currentHash = passwordHash ?: return false
    return currentHash == passwordFingerprint(password, email) ||
        currentHash == legacyPasswordFingerprint(password)
}

private fun passwordFingerprint(password: String, email: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest("remka:${email.normalizedEmail()}:$password".encodeToByteArray())

    return "sha256:" + bytes.joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
}

private fun legacyPasswordFingerprint(password: String): String =
    password.hashCode().toString().replace("-", "m")

private fun String.toAssignments(): List<WorkAssignment> =
    lineSequence()
        .map { line -> line.trim() }
        .filter { line -> line.isNotBlank() }
        .map { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                WorkAssignment(
                    userId = parts[0].trim(),
                    description = parts[1].trim()
                )
            } else {
                WorkAssignment(
                    userId = "unknown",
                    description = line
                )
            }
        }
        .filter { assignment -> assignment.description.isNotBlank() }
        .toList()

private fun List<WorkAssignment>.toAssignmentText(): String =
    joinToString("\n") { assignment -> "${assignment.userId}: ${assignment.description}" }

private fun demoSnapshot(): RemkaSnapshot {
    val demoUser = UserAccount(
        id = "nikita-demo",
        email = "nikita@example.com",
        displayName = "Nikita",
        passwordHash = passwordFingerprint("123456", "nikita@example.com")
    )
    val rostovFolder = VehicleFolder(
        id = "demo-folder-rostov",
        name = "Ростов",
        createdAt = "2026-06-24",
        ownerUserId = demoUser.id
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
        updatedAt = "2026-06-24",
        ownerUserId = demoUser.id
    )

    return RemkaSnapshot(
        currentUser = demoUser,
        knownUsers = listOf(demoUser),
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
                comment = "Позже проверить крепления.",
                createdByUserId = demoUser.id,
                assignments = listOf(
                    WorkAssignment(userId = demoUser.id, description = "установил багажник")
                )
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
                comment = "Сливная пробка плохо закручивается.",
                createdByUserId = demoUser.id,
                assignments = listOf(
                    WorkAssignment(userId = demoUser.id, description = "поменял масло")
                )
            )
        ),
        plans = listOf(
            MaintenancePlan(
                id = "demo-plan-1",
                vehicleId = motorcycle.id,
                title = "Поменять лампочку",
                plannedDate = "2026-07-10",
                reminderDate = "2026-07-09",
                comment = "Перед покупкой проверить тип лампы.",
                createdByUserId = demoUser.id,
                assignments = listOf(
                    WorkAssignment(userId = demoUser.id, description = "заменить лампочку")
                )
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
            .background(
                Brush.linearGradient(
                    colors = listOf(PremiumAccentSoft, PremiumGoldSoft)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            color = PremiumAccent
        )
    }
}
