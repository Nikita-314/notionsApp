package com.remka.data

import com.remka.domain.Attachment
import com.remka.domain.AttachmentOwnerType
import com.remka.domain.MaintenancePlan
import com.remka.domain.Vehicle
import com.remka.domain.VehicleEvent

class RemkaRepository {
    private val vehicles = mutableListOf<Vehicle>()
    private val events = mutableListOf<VehicleEvent>()
    private val plans = mutableListOf<MaintenancePlan>()
    private val attachments = mutableListOf<Attachment>()

    fun addVehicle(vehicle: Vehicle) {
        vehicles.add(vehicle)
    }

    fun addEvent(event: VehicleEvent) {
        events.add(event)
    }

    fun addPlan(plan: MaintenancePlan) {
        plans.add(plan)
    }

    fun addAttachments(newAttachments: List<Attachment>) {
        attachments.addAll(newAttachments)
    }

    fun updatePlan(updatedPlan: MaintenancePlan): Boolean {
        val index = plans.indexOfFirst { plan -> plan.id == updatedPlan.id }

        if (index == -1) {
            return false
        }

        plans[index] = updatedPlan
        return true
    }

    fun loadSnapshot(snapshot: RemkaSnapshot) {
        vehicles.clear()
        vehicles.addAll(snapshot.vehicles)

        events.clear()
        events.addAll(snapshot.events)

        plans.clear()
        plans.addAll(snapshot.plans)

        attachments.clear()
        attachments.addAll(snapshot.attachments)
    }

    fun createSnapshot(): RemkaSnapshot =
        RemkaSnapshot(
            vehicles = vehicles.toList(),
            events = events.toList(),
            plans = plans.toList(),
            attachments = attachments.toList()
        )

    fun getVehicles(): List<Vehicle> = vehicles.toList()

    fun getVehicleById(vehicleId: String): Vehicle? =
        vehicles.firstOrNull { vehicle -> vehicle.id == vehicleId }

    fun getEventsForVehicle(vehicleId: String): List<VehicleEvent> =
        events.filter { event -> event.vehicleId == vehicleId }

    fun getPlansForVehicle(vehicleId: String): List<MaintenancePlan> =
        plans.filter { plan -> plan.vehicleId == vehicleId }

    fun getAttachments(ownerType: AttachmentOwnerType, ownerId: String): List<Attachment> =
        attachments.filter { attachment ->
            attachment.ownerType == ownerType && attachment.ownerId == ownerId
        }
}
