package com.nikita.notionsapp.data

import com.nikita.notionsapp.domain.AppUserPreferences
import com.nikita.notionsapp.domain.ChecklistItem
import com.nikita.notionsapp.domain.JournalAttachment
import com.nikita.notionsapp.domain.JournalBook
import com.nikita.notionsapp.domain.JournalEntry
import com.nikita.notionsapp.domain.RecurrenceRule
import com.nikita.notionsapp.domain.ReminderRule
import com.nikita.notionsapp.domain.SavedGeofencePlace
import com.nikita.notionsapp.domain.SavedWifiPlace
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
