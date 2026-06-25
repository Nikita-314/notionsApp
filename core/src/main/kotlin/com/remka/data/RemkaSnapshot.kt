package com.remka.data

import com.remka.domain.Attachment
import com.remka.domain.MaintenancePlan
import com.remka.domain.Vehicle
import com.remka.domain.VehicleEvent
import com.remka.domain.VehicleFolder
import kotlinx.serialization.Serializable

@Serializable
data class RemkaSnapshot(
    val vehicles: List<Vehicle> = emptyList(),
    val events: List<VehicleEvent> = emptyList(),
    val plans: List<MaintenancePlan> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val folders: List<VehicleFolder> = emptyList()
)
