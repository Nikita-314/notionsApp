package com.remka.data

import java.util.UUID

class IdGenerator {
    fun nextVehicleId(): String = "vehicle-${UUID.randomUUID()}"

    fun nextEventId(): String = "event-${UUID.randomUUID()}"

    fun nextPlanId(): String = "plan-${UUID.randomUUID()}"

    fun nextAttachmentId(): String = "attachment-${UUID.randomUUID()}"
}
