package com.remka.domain

import kotlinx.serialization.Serializable

@Serializable
enum class MaintenancePlanStatus {
    PLANNED,
    DONE,
    CANCELLED
}

@Serializable
data class MaintenancePlan(
    val id: String,
    val vehicleId: String,
    val title: String,
    val plannedDate: String,
    val reminderDate: String? = null,
    val targetMileage: Long? = null,
    val placeToBuy: String? = null,
    val responsiblePerson: String? = null,
    val comment: String? = null,
    val status: MaintenancePlanStatus = MaintenancePlanStatus.PLANNED
)
