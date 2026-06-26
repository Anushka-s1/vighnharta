package com.example.data

import kotlinx.coroutines.flow.Flow

class SoundSchedulerRepository(
    private val scheduleDao: ScheduleDao,
    private val historyDao: HistoryDao,
    private val settingsDao: SettingsDao
) {
    val allSchedules: Flow<List<Schedule>> = scheduleDao.getAllSchedules()
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistory()

    suspend fun getScheduleById(id: Int): Schedule? = scheduleDao.getScheduleById(id)

    suspend fun insertSchedule(schedule: Schedule): Long = scheduleDao.insertSchedule(schedule)

    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.updateSchedule(schedule)

    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.deleteSchedule(schedule)

    suspend fun clearAllSchedules() = scheduleDao.clearAllSchedules()

    suspend fun insertHistoryItem(item: HistoryItem) = historyDao.insertHistoryItem(item)

    suspend fun clearHistory() = historyDao.clearHistory()

    suspend fun getSetting(key: String): String? = settingsDao.getSetting(key)?.value

    suspend fun saveSetting(key: String, value: String) {
        settingsDao.insertSetting(AppSettings(key, value))
    }

    suspend fun deleteSetting(key: String) {
        settingsDao.deleteSetting(key)
    }
}
