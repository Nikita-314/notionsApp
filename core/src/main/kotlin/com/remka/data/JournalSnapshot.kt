package com.remka.data

import com.remka.domain.AppUserPreferences
import com.remka.domain.ChecklistItem
import com.remka.domain.JournalAttachment
import com.remka.domain.JournalBook
import com.remka.domain.JournalEntry
import com.remka.domain.RecurrenceRule
import com.remka.domain.ReminderRule
import com.remka.domain.SavedGeofencePlace
import com.remka.domain.SavedWifiPlace
import kotlinx.serialization.Serializable

@Serializable
data class JournalSnapshot(
    val books: List<JournalBook> = emptyList(),
    val entries: List<JournalEntry> = emptyList(),
    val checklistItems: List<ChecklistItem> = emptyList(),
    val reminderRules: List<ReminderRule> = emptyList(),
    val recurrenceRules: List<RecurrenceRule> = emptyList(),
    val attachments: List<JournalAttachment> = emptyList(),
    val savedWifiPlaces: List<SavedWifiPlace> = emptyList(),
    val savedGeofencePlaces: List<SavedGeofencePlace> = emptyList(),
    val preferences: AppUserPreferences = AppUserPreferences(),
    val pendingSyncVersion: Long = 0,
    val lastSyncedVersion: Long = 0
)
