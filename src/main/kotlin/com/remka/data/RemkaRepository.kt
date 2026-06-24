package com.remka.data

import com.remka.domain.MaintenancePlan
import com.remka.domain.Vehicle
import com.remka.domain.VehicleEvent

class RemkaRepository {
    private val vehicles = mutableListOf<Vehicle>()
    private val events = mutableListOf<VehicleEvent>()
    private val plans = mutableListOf<MaintenancePlan>()

    fun addVehicle(vehicle: Vehicle) {
        vehicles.add(vehicle)
    }

    fun addEvent(event: VehicleEvent) {
        events.add(event)
    }

    fun addPlan(plan: MaintenancePlan) {
        plans.add(plan)
    }

    fun getVehicles(): List<Vehicle> = vehicles.toList()

    fun getVehicleById(vehicleId: String): Vehicle? =
        vehicles.firstOrNull { vehicle -> vehicle.id == vehicleId }

    fun getEventsForVehicle(vehicleId: String): List<VehicleEvent> =
        events.filter { event -> event.vehicleId == vehicleId }

    fun getPlansForVehicle(vehicleId: String): List<MaintenancePlan> =
        plans.filter { plan -> plan.vehicleId == vehicleId }
}
