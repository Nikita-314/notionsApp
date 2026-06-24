package com.remka.data

class IdGenerator {
    private var nextVehicleNumber = 1
    private var nextEventNumber = 1

    fun nextVehicleId(): String {
        val id = "vehicle-$nextVehicleNumber"
        nextVehicleNumber++
        return id
    }

    fun nextEventId(): String {
        val id = "event-$nextEventNumber"
        nextEventNumber++
        return id
    }
}
