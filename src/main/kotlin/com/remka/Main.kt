package com.remka

import com.remka.data.IdGenerator
import com.remka.data.RemkaRepository
import com.remka.domain.EventParticipant
import com.remka.domain.MaintenancePlan
import com.remka.domain.Vehicle
import com.remka.domain.VehicleEvent
import com.remka.domain.VehicleEventType
import com.remka.domain.VehicleType
import com.remka.ui.printVehicleCard
import com.remka.ui.printVehicleList
import com.remka.ui.readVehicle

fun main() {
    val repository = RemkaRepository()
    val idGenerator = IdGenerator()

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

    runMenu(repository, idGenerator)
}

private fun runMenu(repository: RemkaRepository, idGenerator: IdGenerator) {
    while (true) {
        println()
        println("=== Remka ===")
        println("1. Показать транспорт")
        println("2. Добавить транспорт")
        println("3. Открыть карточку транспорта")
        println("0. Выход")
        print("Выбери действие: ")

        when (readln().trim()) {
            "1" -> printVehicleList(repository.getVehicles())
            "2" -> {
                val vehicle = readVehicle(idGenerator)
                repository.addVehicle(vehicle)
                println("Добавлено: ${vehicle.name}")
            }
            "3" -> openVehicleCard(repository)
            "0" -> {
                println("До встречи!")
                return
            }
            else -> println("Неизвестная команда.")
        }
    }
}

private fun openVehicleCard(repository: RemkaRepository) {
    val vehicles = repository.getVehicles()

    if (vehicles.isEmpty()) {
        println("Сначала добавь транспорт.")
        return
    }

    printVehicleList(vehicles)
    print("Введи номер транспорта: ")

    val index = readln().trim().toIntOrNull()
    if (index == null || index !in 1..vehicles.size) {
        println("Такого номера нет.")
        return
    }

    val vehicle = vehicles[index - 1]
    printVehicleCard(repository, vehicle.id)
}
