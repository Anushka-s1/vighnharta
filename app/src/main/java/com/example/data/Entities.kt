package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val vibrateTime: String, // "HH:mm"
    val soundTime: String, // "HH:mm"
    val days: String, // "MON,TUE,WED,THU,FRI"
    val isEnabled: Boolean = true,
    val customMessage: String = "Silence is golden",
    val ringtoneName: String = "Default Bell",
    val hapticPattern: String = "Gentle",
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius: Double? = null,
    val isSmartCalendar: Boolean = false
)

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scheduleName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // "VIBRATE_ACTIVATED", "SOUND_ACTIVATED", "OVERRIDE", "PAUSED"
    val details: String
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val key: String,
    val value: String
)
