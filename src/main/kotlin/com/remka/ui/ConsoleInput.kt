package com.remka.ui

import com.remka.data.IdGenerator
import com.remka.domain.Vehicle
import com.remka.domain.VehicleType

fun readVehicle(idGenerator: IdGenerator): Vehicle {
    println("Добавление транспорта")

    val type = readVehicleType()
    val name = readRequiredText("Название")
    val manufacturer = readOptionalText("Производитель")
    val model = readOptionalText("Модель")
    val year = readOptionalInt("Год")
    val registrationNumber = readOptionalText("Госномер")
    val mileage = readOptionalLong("Пробег")

    return Vehicle(
        id = idGenerator.nextVehicleId(),
        type = type,
        name = name,
        manufacturer = manufacturer,
        model = model,
        year = year,
        registrationNumber = registrationNumber,
        currentMileage = mileage
    )
}

private fun readVehicleType(): VehicleType {
    while (true) {
        println("Тип транспорта:")
        println("1. Мотоцикл")
        println("2. Машина")
        println("3. Скутер")
        println("4. Велосипед")
        println("5. Другое")
        print("Выбери номер: ")

        when (readln().trim()) {
            "1" -> return VehicleType.MOTORCYCLE
            "2" -> return VehicleType.CAR
            "3" -> return VehicleType.SCOOTER
            "4" -> return VehicleType.BICYCLE
            "5" -> return VehicleType.OTHER
            else -> println("Не понял выбор. Попробуй ещё раз.")
        }
    }
}

private fun readRequiredText(label: String): String {
    while (true) {
        print("$label: ")
        val value = readln().trim()

        if (value.isNotBlank()) {
            return value
        }

        println("Это поле обязательно.")
    }
}

private fun readOptionalText(label: String): String? {
    print("$label (можно оставить пустым): ")
    return readln().trim().ifBlank { null }
}

private fun readOptionalInt(label: String): Int? {
    while (true) {
        print("$label (можно оставить пустым): ")
        val value = readln().trim()

        if (value.isBlank()) {
            return null
        }

        val number = value.toIntOrNull()
        if (number != null) {
            return number
        }

        println("Нужно ввести целое число.")
    }
}

private fun readOptionalLong(label: String): Long? {
    while (true) {
        print("$label (можно оставить пустым): ")
        val value = readln().trim()

        if (value.isBlank()) {
            return null
        }

        val number = value.toLongOrNull()
        if (number != null) {
            return number
        }

        println("Нужно ввести целое число.")
    }
}
