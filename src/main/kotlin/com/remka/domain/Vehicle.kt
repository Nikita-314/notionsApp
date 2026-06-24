package com.remka.domain

import kotlinx.serialization.Serializable

@Serializable
enum class VehicleType {
    MOTORCYCLE,
    CAR,
    SCOOTER,
    BICYCLE,
    ATV,
    BOAT,
    OTHER
}

@Serializable
data class Vehicle(
    val id: String,
    val type: VehicleType,
    val name: String,
    val manufacturer: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val registrationNumber: String? = null,
    val currentMileage: Long? = null
)
