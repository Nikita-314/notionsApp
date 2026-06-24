package com.remka

import com.remka.data.IdGenerator
import com.remka.data.JsonFileStorage
import com.remka.data.RemkaRepository
import com.remka.domain.AttachmentOwnerType
import com.remka.domain.EventParticipant
import com.remka.domain.MaintenancePlan
import com.remka.domain.MaintenancePlanStatus
import com.remka.domain.Vehicle
import com.remka.domain.VehicleEvent
import com.remka.domain.VehicleEventType
import com.remka.domain.VehicleType
import com.remka.ui.printVehicleCard
import com.remka.ui.printVehicleList
import com.remka.ui.readAttachments
import com.remka.ui.readMaintenancePlan
import com.remka.ui.readVehicle
import com.remka.ui.readVehicleEvent
import java.io.PrintStream
import java.nio.charset.StandardCharsets

fun main() {
    configureUtf8Console()

    val repository = RemkaRepository()
    val idGenerator = IdGenerator()
    val storage = JsonFileStorage()

    val loadedSnapshot = storage.load()
    if (loadedSnapshot == null) {
        addDemoData(repository, idGenerator)
    } else {
        repository.loadSnapshot(loadedSnapshot)
        println("Данные загружены из remka-data.json")
    }

    runMenu(repository, idGenerator, storage)
}

private fun addDemoData(repository: RemkaRepository, idGenerator: IdGenerator) {
    val motorcycle = Vehicle(
        id = idGenerator.nextVehicleId(),
        type = VehicleType.MOTORCYCLE,
        name = "Мой мотоцикл",
        manufacturer = "Honda",
        model = "CB400",
        year = 2007,
        registrationNumber = "A123BC",
        currentMileage = 42000
    )

    repository.addVehicle(motorcycle)

    repository.addEvent(
        VehicleEvent(
            id = "event-1",
            vehicleId = motorcycle.id,
            type = VehicleEventType.INSTALLED_PART,
            title = "Установил багажник",
            date = "2026-06-24",
            mileage = 42010,
            cost = 8500.0,
            shopName = "MotoParts",
            comment = "Багажник встал хорошо, но нужно позже проверить крепления.",
            participants = listOf(
                EventParticipant(
                    name = "Никита",
                    workDescription = "Установка и проверка креплений"
                )
            )
        )
    )

    repository.addEvent(
        VehicleEvent(
            id = "event-2",
            vehicleId = motorcycle.id,
            type = VehicleEventType.MAINTENANCE,
            title = "Поменял масло",
            date = "2026-06-24",
            mileage = 42100,
            cost = 3200.0,
            shopName = "Oil Market",
            comment = "Сливная пробка плохо закручивается, нужно заменить."
        )
    )

    repository.addPlan(
        MaintenancePlan(
            id = "plan-1",
            vehicleId = motorcycle.id,
            title = "Поменять лампочку",
            plannedDate = "2026-07-10",
            reminderDate = "2026-07-09",
            placeToBuy = "Автомагазин у дома",
            comment = "Перед покупкой проверить тип лампы."
        )
    )
}

private fun configureUtf8Console() {
    System.setOut(PrintStream(System.out, true, StandardCharsets.UTF_8))
    System.setErr(PrintStream(System.err, true, StandardCharsets.UTF_8))
}

private fun runMenu(repository: RemkaRepository, idGenerator: IdGenerator, storage: JsonFileStorage) {
    while (true) {
        println()
        println("=== Remka ===")
        println("1. Показать транспорт")
        println("2. Добавить транспорт")
        println("3. Открыть карточку транспорта")
        println("4. Добавить событие")
        println("5. Добавить план")
        println("6. Изменить статус плана")
        println("0. Выход")
        print("Выбери действие: ")

        when (readln().trim()) {
            "1" -> printVehicleList(repository.getVehicles())
            "2" -> {
                val vehicle = readVehicle(idGenerator)
                repository.addVehicle(vehicle)
                saveRepository(repository, storage)
                println("Добавлено: ${vehicle.name}")
            }
            "3" -> openVehicleCard(repository)
            "4" -> addVehicleEvent(repository, idGenerator, storage)
            "5" -> addMaintenancePlan(repository, idGenerator, storage)
            "6" -> changePlanStatus(repository, storage)
            "0" -> {
                println("До встречи!")
                return
            }
            else -> println("Неизвестная команда.")
        }
    }
}

private fun openVehicleCard(repository: RemkaRepository) {
    val vehicle = chooseVehicle(repository) ?: return
    printVehicleCard(repository, vehicle.id)
}

private fun addVehicleEvent(repository: RemkaRepository, idGenerator: IdGenerator, storage: JsonFileStorage) {
    val vehicle = chooseVehicle(repository) ?: return
    val event = readVehicleEvent(idGenerator, vehicle.id)
    val attachments = readAttachments(idGenerator, AttachmentOwnerType.EVENT, event.id)

    repository.addEvent(event)
    repository.addAttachments(attachments)
    saveRepository(repository, storage)
    println("Событие добавлено: ${event.title}")
}

private fun addMaintenancePlan(repository: RemkaRepository, idGenerator: IdGenerator, storage: JsonFileStorage) {
    val vehicle = chooseVehicle(repository) ?: return
    val plan = readMaintenancePlan(idGenerator, vehicle.id)
    val attachments = readAttachments(idGenerator, AttachmentOwnerType.PLAN, plan.id)

    repository.addPlan(plan)
    repository.addAttachments(attachments)
    saveRepository(repository, storage)
    println("План добавлен: ${plan.title}")
}

private fun changePlanStatus(repository: RemkaRepository, storage: JsonFileStorage) {
    val vehicle = chooseVehicle(repository) ?: return
    val plans = repository.getPlansForVehicle(vehicle.id)

    if (plans.isEmpty()) {
        println("У этого транспорта пока нет планов.")
        return
    }

    plans.forEachIndexed { index, plan ->
        println("${index + 1}. ${plan.title} - ${plan.status}")
    }
    print("Введи номер плана: ")

    val planIndex = readln().trim().toIntOrNull()
    if (planIndex == null || planIndex !in 1..plans.size) {
        println("Такого плана нет.")
        return
    }

    val plan = plans[planIndex - 1]
    val newStatus = readPlanStatus() ?: return
    val updated = plan.copy(status = newStatus)

    if (repository.updatePlan(updated)) {
        saveRepository(repository, storage)
        println("Статус плана изменён: ${updated.title} - ${updated.status}")
    } else {
        println("Не удалось обновить план.")
    }
}

private fun readPlanStatus(): MaintenancePlanStatus? {
    println("Новый статус:")
    println("1. Запланирован")
    println("2. Выполнен")
    println("3. Отменён")
    print("Выбери номер: ")

    return when (readln().trim()) {
        "1" -> MaintenancePlanStatus.PLANNED
        "2" -> MaintenancePlanStatus.DONE
        "3" -> MaintenancePlanStatus.CANCELLED
        else -> {
            println("Неизвестный статус.")
            null
        }
    }
}

private fun saveRepository(repository: RemkaRepository, storage: JsonFileStorage) {
    storage.save(repository.createSnapshot())
    println("Данные сохранены в ${storage.displayPath()}")
}

private fun chooseVehicle(repository: RemkaRepository): Vehicle? {
    val vehicles = repository.getVehicles()

    if (vehicles.isEmpty()) {
        println("Сначала добавь транспорт.")
        return null
    }

    printVehicleList(vehicles)
    print("Введи номер транспорта: ")

    val index = readln().trim().toIntOrNull()
    if (index == null || index !in 1..vehicles.size) {
        println("Такого номера нет.")
        return null
    }

    return vehicles[index - 1]
}
