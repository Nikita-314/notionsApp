package com.remka.domain

import kotlinx.serialization.Serializable

@Serializable
data class VehicleFolder(
    val id: String,
    val name: String,
    val createdAt: String = ""
)
