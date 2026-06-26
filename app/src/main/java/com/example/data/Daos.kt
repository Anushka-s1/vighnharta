package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY id DESC")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Int): Schedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long

    @Update
    suspend fun updateSchedule(schedule: Schedule)

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)

    @Query("DELETE FROM schedules")
    suspend fun clearAllSchedules()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryItem)

    @Query("DELETE FROM history_items")
    suspend fun clearHistory()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSettings)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}
