package com.remka.ui

import com.remka.data.IdGenerator
import com.remka.domain.Attachment
import com.remka.domain.AttachmentOwnerType
import com.remka.domain.AttachmentType
import com.remka.domain.EventParticipant
import com.remka.domain.MaintenancePlan
import com.remka.domain.Vehicle
import com.remka.domain.VehicleEvent
import com.remka.domain.VehicleEventType
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

fun readVehicleEvent(idGenerator: IdGenerator, vehicleId: String): VehicleEvent {
    println("Добавление события")

    val type = readVehicleEventType()
    val title = readRequiredText("Название события")
    val date = readRequiredText("Дата")
    val mileage = readOptionalLong("Пробег")
    val cost = readOptionalDouble("Стоимость")
    val shopName = readOptionalText("Магазин")
    val comment = readOptionalText("Комментарий")
    val helperName = readOptionalText("Кто помогал")
    val participants = if (helperName == null) {
        emptyList()
    } else {
        listOf(
            EventParticipant(
                name = helperName,
                workDescription = readOptionalText("Что сделал помощник")
            )
        )
    }

    return VehicleEvent(
        id = idGenerator.nextEventId(),
        vehicleId = vehicleId,
        type = type,
        title = title,
        date = date,
        mileage = mileage,
        cost = cost,
        shopName = shopName,
        comment = comment,
        participants = participants
    )
}

fun readAttachments(
    idGenerator: IdGenerator,
    ownerType: AttachmentOwnerType,
    ownerId: String
): List<Attachment> {
    val attachments = mutableListOf<Attachment>()

    while (true) {
        print("Путь к фото/чеку/документу (пусто - закончить): ")
        val path = readln().trim()

        if (path.isBlank()) {
            return attachments
        }

        val type = readAttachmentType()
        val comment = readOptionalText("Комментарий к вложению")

        attachments.add(
            Attachment(
                id = idGenerator.nextAttachmentId(),
                ownerType = ownerType,
                ownerId = ownerId,
                type = type,
                path = path,
                comment = comment
            )
        )
    }
}

fun readMaintenancePlan(idGenerator: IdGenerator, vehicleId: String): MaintenancePlan {
    println("Добавление плана")

    val title = readRequiredText("Название плана")
    val plannedDate = readRequiredText("Дата выполнения")
    val reminderDate = readOptionalText("Дата напоминания")
    val targetMileage = readOptionalLong("Пробег для выполнения")
    val placeToBuy = readOptionalText("Где купить")
    val responsiblePerson = readOptionalText("Кто отвечает")
    val comment = readOptionalText("Комментарий")

    return MaintenancePlan(
        id = idGenerator.nextPlanId(),
        vehicleId = vehicleId,
        title = title,
        plannedDate = plannedDate,
        reminderDate = reminderDate,
        targetMileage = targetMileage,
        placeToBuy = placeToBuy,
        responsiblePerson = responsiblePerson,
        comment = comment
    )
}

private fun readAttachmentType(): AttachmentType {
    while (true) {
        println("Тип вложения:")
        println("1. Фото")
        println("2. Чек")
        println("3. Документ")
        println("4. Другое")
        print("Выбери номер: ")

        when (readln().trim()) {
            "1" -> return AttachmentType.PHOTO
            "2" -> return AttachmentType.RECEIPT
            "3" -> return AttachmentType.DOCUMENT
            "4" -> return AttachmentType.OTHER
            else -> println("Не понял выбор. Попробуй ещё раз.")
        }
    }
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

private fun readVehicleEventType(): VehicleEventType {
    while (true) {
        println("Тип события:")
        println("1. Обслуживание")
        println("2. Ремонт")
        println("3. Установка детали")
        println("4. Покупка")
        println("5. Диагностика")
        println("6. Мойка")
        println("7. Другое")
        print("Выбери номер: ")

        when (readln().trim()) {
            "1" -> return VehicleEventType.MAINTENANCE
            "2" -> return VehicleEventType.REPAIR
            "3" -> return VehicleEventType.INSTALLED_PART
            "4" -> return VehicleEventType.PURCHASE
            "5" -> return VehicleEventType.DIAGNOSTIC
            "6" -> return VehicleEventType.WASH
            "7" -> return VehicleEventType.CUSTOM
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

private fun readOptionalDouble(label: String): Double? {
    while (true) {
        print("$label (можно оставить пустым): ")
        val value = readln().trim()

        if (value.isBlank()) {
            return null
        }

        val number = value.replace(',', '.').toDoubleOrNull()
        if (number != null) {
            return number
        }

        println("Нужно ввести число.")
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
