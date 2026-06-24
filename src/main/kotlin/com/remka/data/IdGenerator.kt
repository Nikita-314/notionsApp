package com.remka.data

class IdGenerator {
    private var nextVehicleNumber = 1

    fun nextVehicleId(): String {
        val id = "vehicle-$nextVehicleNumber"
        nextVehicleNumber++
        return id
    }
}
