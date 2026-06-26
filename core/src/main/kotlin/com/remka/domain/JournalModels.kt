package com.remka.domain

import kotlinx.serialization.Serializable

@Serializable
enum class JournalEntryVisualStatus {
    NEUTRAL,
    FOCUS,
    BLOCKED,
    DONE
}

@Serializable
enum class JournalEntrySourceType {
    MANUAL,
    RECURRING,
    CARRIED
}

@Serializable
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Serializable
enum class ReminderRuleKind {
    DATE_TIME,
    WIFI,
    GEOFENCE
}

@Serializable
enum class AttachmentKind {
    PHOTO,
    DOCUMENT,
    AUDIO,
    LINK,
    OTHER
}

@Serializable
enum class DayCompletedListMode {
    INLINE,
    COLLAPSED,
    HIDDEN
}

@Serializable
enum class DayListDensity {
    COMFORTABLE,
    COMPACT
}

@Serializable
data class JournalBook(
    val id: String,
    val title: String,
    val colorArgb: Long,
    val icon: String = "dot",
    val isArchived: Boolean = false,
    val manualPosition: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class JournalEntry(
    val id: String,
    val bookId: String,
    val dayDate: String,
    val title: String,
    val notes: String? = null,
    val manualPosition: Int = 0,
    val timeOfDayMinutes: Int? = null,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val visualStatus: JournalEntryVisualStatus = JournalEntryVisualStatus.NEUTRAL,
    val colorOverrideArgb: Long? = null,
    val sourceType: JournalEntrySourceType = JournalEntrySourceType.MANUAL,
    val originEntryId: String? = null,
    val parentRecurringId: String? = null,
    val carryFromDayKey: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ChecklistItem(
    val id: String,
    val entryId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val manualPosition: Int = 0
)

@Serializable
data class ReminderRule(
    val id: String,
    val entryId: String,
    val kind: ReminderRuleKind,
    val triggerAt: String? = null,
    val placeName: String? = null,
    val wifiSsid: String? = null,
    val isEnabled: Boolean = true
)

@Serializable
data class RecurrenceRule(
    val id: String,
    val entryId: String,
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val untilDate: String? = null
)

@Serializable
data class JournalAttachment(
    val id: String,
    val entryId: String,
    val kind: AttachmentKind,
    val uri: String,
    val title: String? = null
)

@Serializable
data class SavedWifiPlace(
    val id: String,
    val title: String,
    val ssid: String,
    val note: String? = null
)

@Serializable
data class SavedGeofencePlace(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 120
)

@Serializable
data class AppUserPreferences(
    val completedListMode: DayCompletedListMode = DayCompletedListMode.INLINE,
    val listDensity: DayListDensity = DayListDensity.COMFORTABLE,
    val groupCarriedSection: Boolean = true,
    val showBookLabels: Boolean = true,
    val startDayMinutes: Int = 7 * 60
)
