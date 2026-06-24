package com.remka.ui

import com.remka.data.RemkaRepository
import com.remka.domain.Attachment
import com.remka.domain.AttachmentOwnerType
import com.remka.domain.Vehicle

fun printVehicleList(vehicles: List<Vehicle>) {
    if (vehicles.isEmpty()) {
        println("Транспорт пока не добавлен.")
        return
    }

    vehicles.forEachIndexed { index, vehicle ->
        val number = index + 1
        val model = listOfNotNull(vehicle.manufacturer, vehicle.model).joinToString(" ")
        val modelText = model.ifBlank { "модель не указана" }

        println("$number. ${vehicle.name} - $modelText")
    }
}

fun printVehicleCard(repository: RemkaRepository, vehicleId: String) {
    val vehicle = repository.getVehicleById(vehicleId)

    if (vehicle == null) {
        println("Транспорт не найден.")
        return
    }

    val events = repository.getEventsForVehicle(vehicle.id)
    val plans = repository.getPlansForVehicle(vehicle.id)

    println("=== ${vehicle.name} ===")
    println("Тип: ${vehicle.type}")
    println("Модель: ${vehicle.manufacturer ?: "не указана"} ${vehicle.model ?: ""}".trim())
    println("Год: ${vehicle.year ?: "не указан"}")
    println("Госномер: ${vehicle.registrationNumber ?: "не указан"}")
    println("Пробег: ${vehicle.currentMileage ?: "не указан"} км")
    println()

    println("История:")
    if (events.isEmpty()) {
        println("Событий пока нет.")
    }

    events.forEach { event ->
        println("- ${event.date}: ${event.title}")
        println("  Тип: ${event.type}")
        println("  Пробег: ${event.mileage ?: "не указан"} км")
        println("  Стоимость: ${event.cost ?: "не указана"}")
        println("  Магазин: ${event.shopName ?: "не указан"}")
        println("  Комментарий: ${event.comment ?: "нет"}")

        if (event.participants.isNotEmpty()) {
            println("  Участники:")
            event.participants.forEach { participant ->
                println("  - ${participant.name}: ${participant.workDescription ?: "работа не описана"}")
            }
        }

        printAttachments(repository.getAttachments(AttachmentOwnerType.EVENT, event.id))
    }

    println()
    println("Планы:")
    if (plans.isEmpty()) {
        println("Планов пока нет.")
    }

    plans.forEach { plan ->
        println("- ${plan.plannedDate}: ${plan.title}")
        println("  Статус: ${plan.status}")
        println("  Напомнить: ${plan.reminderDate ?: "не задано"}")
        println("  Пробег: ${plan.targetMileage ?: "не указан"} км")
        println("  Где купить: ${plan.placeToBuy ?: "не указано"}")
        println("  Ответственный: ${plan.responsiblePerson ?: "не указан"}")
        println("  Комментарий: ${plan.comment ?: "нет"}")
        printAttachments(repository.getAttachments(AttachmentOwnerType.PLAN, plan.id))
    }
}

private fun printAttachments(attachments: List<Attachment>) {
    if (attachments.isEmpty()) {
        return
    }

    println("  Вложения:")
    attachments.forEach { attachment ->
        val comment = attachment.comment?.let { " ($it)" } ?: ""
        println("  - ${attachment.type}: ${attachment.path}$comment")
    }
}
