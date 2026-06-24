package com.remka.domain

import kotlinx.serialization.Serializable

@Serializable
enum class AttachmentOwnerType {
    EVENT,
    PLAN,
    VEHICLE
}

@Serializable
enum class AttachmentType {
    PHOTO,
    RECEIPT,
    DOCUMENT,
    OTHER
}

@Serializable
data class Attachment(
    val id: String,
    val ownerType: AttachmentOwnerType,
    val ownerId: String,
    val type: AttachmentType,
    val path: String,
    val comment: String? = null
)
