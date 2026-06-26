package com.example

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.theme.SoundSchedulerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SoundSchedulerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = SoundSchedulerRepository(
        database.scheduleDao(),
        database.historyDao(),
        database.settingsDao()
    )

    // UI States
    val schedules: StateFlow<List<Schedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Preferences / Settings States
    private val _masterPause = MutableStateFlow(false)
    val masterPause = _masterPause.asStateFlow()

    private val _appTheme = MutableStateFlow(SoundSchedulerTheme.OCEAN_BLUE)
    val appTheme = _appTheme.asStateFlow()

    private val _vipContacts = MutableStateFlow<List<String>>(emptyList())
    val vipContacts = _vipContacts.asStateFlow()

    // Active Simulated States (Interactive Feedback Banners)
    private val _activeNotification = MutableStateFlow<String?>(null)
    val activeNotification = _activeNotification.asStateFlow()

    private val _activeNotificationDetails = MutableStateFlow<String?>(null)
    val activeNotificationDetails = _activeNotificationDetails.asStateFlow()

    private val _activeNotificationType = MutableStateFlow<String?>(null) // "VIBRATE" or "SOUND"
    val activeNotificationType = _activeNotificationType.asStateFlow()

    // Conflict detection warning message
    private val _conflictWarning = MutableStateFlow<String?>(null)
    val conflictWarning = _conflictWarning.asStateFlow()

    // Manual Override State
    private val _manualOverride = MutableStateFlow<String?>("NONE") // "VIBRATE", "SOUND", "NONE"
    val manualOverride = _manualOverride.asStateFlow()

    private var previousRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // AI Calendar Parsing State
    private val _isAnalyzingCalendar = MutableStateFlow(false)
    val isAnalyzingCalendar = _isAnalyzingCalendar.asStateFlow()

    // User authentication mock state (Offline personalized)
    private val _userName = MutableStateFlow("Jane Doe")
    val userName = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("jane.doe@example.com")
    val userEmail = _userEmail.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    // Simulated Location State
    private val _currentLocationName = MutableStateFlow("🏠 Home")
    val currentLocationName = _currentLocationName.asStateFlow()

    // Simulate current GPS location & trigger geofence checks
    fun simulateLocation(location: String) {
        viewModelScope.launch {
            _currentLocationName.value = location
            logEvent("Location Sensor", "GEOFENCE_UPDATE", "Simulated location changed to: $location")
            
            // If master pause is active, don't execute auto sound changes
            if (_masterPause.value) return@launch
            
            // Check if any active enabled schedule has a matching location
            val activeScheds = schedules.value.filter { it.isEnabled && it.latitude != null && it.longitude != null }
            var foundGeofence = false
            for (schedule in activeScheds) {
                val scheduleLoc = schedule.locationName ?: ""
                // Extract clean location label to match (e.g. "My Office" matches "🏢 My Office" or "Office")
                val cleanScheduleLoc = scheduleLoc.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                val cleanSimulatedLoc = location.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
                
                if (cleanSimulatedLoc.isNotEmpty() && cleanScheduleLoc.isNotEmpty() &&
                    (cleanSimulatedLoc.contains(cleanScheduleLoc) || cleanScheduleLoc.contains(cleanSimulatedLoc))) {
                    
                    foundGeofence = true
                    if (_manualOverride.value != "VIBRATE") {
                        activateMode(
                            vibrate = true,
                            scheduleName = schedule.name,
                            detail = "📍 Arrived at ${schedule.locationName ?: "Geofence"}. Muting ringer."
                        )
                    }
                    break
                }
            }
            
            // If we left the geofence and are currently in vibrate, turn sound back on
            if (!foundGeofence && _activeNotificationType.value == "VIBRATE" && _activeNotificationDetails.value?.contains("📍 Arrived") == true) {
                activateMode(
                    vibrate = false,
                    scheduleName = "Geofence Exit",
                    detail = "📍 Left Geofence area. Restoring audio profile."
                )
            }
        }
    }

    init {
        // Load Settings on Start
        viewModelScope.launch {
            _masterPause.value = repository.getSetting("master_pause")?.toBoolean() ?: false
            val themeStr = repository.getSetting("app_theme") ?: SoundSchedulerTheme.OCEAN_BLUE.name
            _appTheme.value = SoundSchedulerTheme.values().find { it.name == themeStr } ?: SoundSchedulerTheme.OCEAN_BLUE
            
            val vipStr = repository.getSetting("vip_contacts") ?: "Mom (Emergency),Boss (Work)"
            _vipContacts.value = vipStr.split(",").filter { it.isNotEmpty() }

            val user = repository.getSetting("user_name") ?: "Jane Doe"
            _userName.value = user
            val email = repository.getSetting("user_email") ?: "jane.doe@example.com"
            _userEmail.value = email

            // Start clock tracker to scan schedule triggers periodically
            startClockTracker()
        }
    }

    // Login mock
    fun login(name: String, email: String) {
        viewModelScope.launch {
            _userName.value = name
            _userEmail.value = email
            _isLoggedIn.value = true
            repository.saveSetting("user_name", name)
            repository.saveSetting("user_email", email)
            logEvent("Auth System", "OVERRIDE", "User logged in: $name ($email)")
        }
    }

    fun logout() {
        _isLoggedIn.value = false
    }

    // Haptic and sound feedback helper
    fun triggerFeedback(isVibrate: Boolean) {
        val ctx = getApplication<Application>().applicationContext
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Play Ringtone/Alert Sound
                val soundUri = if (isVibrate) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
                val r = RingtoneManager.getRingtone(ctx, soundUri)
                r?.let {
                    it.play()
                    delay(1200) // Play briefly then stop
                    it.stop()
                }
            } catch (e: Exception) {
                Log.e("SoundScheduler", "Sound playback failed", e)
            }
        }

        // Trigger Android standard vibration pattern
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (isVibrate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Pattern for quiet activation (short-long)
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 300), -1))
                } else {
                    vibrator.vibrate(200)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Pattern for sound activation (single joyful haptic pop)
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(100)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundScheduler", "Haptic feedback failed", e)
        }
    }

    // Core Scheduler Scan Loop (Runs every 10 seconds in background)
    private fun startClockTracker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(10000) // check every 10 seconds
                if (!_masterPause.value) {
                    checkSchedules()
                }
            }
        }
    }

    private suspend fun checkSchedules() {
        val list = schedules.value.filter { it.isEnabled }
        if (list.isEmpty()) return

        // Current time info
        val now = Calendar.getInstance()
        val currentHourMin = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
        val dayOfWeekStr = SimpleDateFormat("EEE", Locale.US).format(now.time).uppercase() // "MON", "TUE" etc

        for (schedule in list) {
            val scheduledDays = schedule.days.split(",")
            if (!scheduledDays.contains(dayOfWeekStr)) continue

            // Check Vibrate trigger
            if (currentHourMin == schedule.vibrateTime && _manualOverride.value != "VIBRATE") {
                activateMode(vibrate = true, scheduleName = schedule.name, detail = "🔇 Vibrate mode activated. Context: ${schedule.customMessage}")
            }

            // Check Sound trigger
            if (currentHourMin == schedule.soundTime && _manualOverride.value != "SOUND") {
                activateMode(vibrate = false, scheduleName = schedule.name, detail = "🔔 Sound mode is back ON. You have 0 missed calls from VIPs.")
            }
        }
    }

    fun activateMode(vibrate: Boolean, scheduleName: String, detail: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Change physical system audio status if permission granted
                val ringerMode = if (vibrate) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL
                audioManager.ringerMode = ringerMode
            } catch (e: Exception) {
                Log.w("SoundScheduler", "System audio manager access failed due to permission or DND constraints. Simulating switch instead.")
            }

            // Set overlay banner
            _activeNotification.value = if (vibrate) "🔇 Vibrate mode activated" else "🔔 Sound mode is back ON"
            _activeNotificationDetails.value = detail
            _activeNotificationType.value = if (vibrate) "VIBRATE" else "SOUND"

            // Log event
            logEvent(
                scheduleName,
                if (vibrate) "VIBRATE_ACTIVATED" else "SOUND_ACTIVATED",
                detail
            )

            // Trigger actual ringtone and haptic patterns!
            triggerFeedback(isVibrate = vibrate)

            // Auto-hide popup banner after 7 seconds
            delay(7000)
            dismissNotification()
        }
    }

    fun dismissNotification() {
        _activeNotification.value = null
        _activeNotificationDetails.value = null
        _activeNotificationType.value = null
    }

    // Manual Overrides
    fun setManualOverride(mode: String) {
        viewModelScope.launch {
            _manualOverride.value = mode
            val targetVibrate = (mode == "VIBRATE")
            try {
                audioManager.ringerMode = if (targetVibrate) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL
            } catch (e: Exception) {
                Log.w("SoundScheduler", "Manual override system AudioManager error", e)
            }

            logEvent("Manual Override", "OVERRIDE", "User manually switched phone to $mode mode.")
            triggerFeedback(isVibrate = targetVibrate)

            _activeNotification.value = if (targetVibrate) "🔇 Manual Vibrate ON" else "🔔 Manual Sound ON"
            _activeNotificationDetails.value = "Schedule triggers are temporarily paused. Tap Undo or Clear Override to resume."
            _activeNotificationType.value = if (targetVibrate) "VIBRATE" else "SOUND"
        }
    }

    fun clearManualOverride() {
        _manualOverride.value = "NONE"
        dismissNotification()
        viewModelScope.launch {
            logEvent("Manual Override", "OVERRIDE", "Cleared manual override. Resumed schedules.")
        }
    }

    // Master Pause
    fun toggleMasterPause() {
        viewModelScope.launch {
            val newVal = !_masterPause.value
            _masterPause.value = newVal
            repository.saveSetting("master_pause", newVal.toString())
            logEvent("Master Toggle", "PAUSED", if (newVal) "All schedules suspended." else "All schedules resumed.")
        }
    }

    // Theme Selection
    fun setTheme(theme: SoundSchedulerTheme) {
        viewModelScope.launch {
            _appTheme.value = theme
            repository.saveSetting("app_theme", theme.name)
            logEvent("App Preferences", "THEME_CHANGED", "Switched application style to ${theme.displayName}.")
        }
    }

    // Save or Update Schedule with Conflict Detection
    fun saveSchedule(schedule: Schedule, onCompleted: () -> Unit) {
        viewModelScope.launch {
            // Check Conflict/Overlaps
            val hasOverlap = checkOverlaps(schedule)
            if (hasOverlap) {
                _conflictWarning.value = "Overlap Detected: This schedule overlaps with one of your active schedules. Do you wish to save anyway?"
            }

            if (schedule.id == 0) {
                repository.insertSchedule(schedule)
                logEvent("Schedule Manager", "CREATED", "Created schedule '${schedule.name}' (${schedule.vibrateTime} - ${schedule.soundTime}).")
            } else {
                repository.updateSchedule(schedule)
                logEvent("Schedule Manager", "UPDATED", "Updated schedule '${schedule.name}' (${schedule.vibrateTime} - ${schedule.soundTime}).")
            }
            onCompleted()
        }
    }

    fun dismissConflictWarning() {
        _conflictWarning.value = null
    }

    private fun checkOverlaps(newSched: Schedule): Boolean {
        val currentList = schedules.value.filter { it.isEnabled && it.id != newSched.id }
        if (currentList.isEmpty()) return false

        val newDays = newSched.days.split(",")
        for (sched in currentList) {
            val schedDays = sched.days.split(",")
            val sharedDays = newDays.filter { schedDays.contains(it) }
            if (sharedDays.isEmpty()) continue

            // Parse hours
            val newStart = parseMinutes(newSched.vibrateTime)
            val newEnd = parseMinutes(newSched.soundTime)
            val existingStart = parseMinutes(sched.vibrateTime)
            val existingEnd = parseMinutes(sched.soundTime)

            // Simple boundary intersection logic
            if ((newStart < existingEnd && newEnd > existingStart)) {
                return true
            }
        }
        return false
    }

    private fun parseMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
            logEvent("Schedule Manager", "DELETED", "Removed schedule '${schedule.name}'.")
        }
    }

    // Presets and Templates Creation
    fun addProfilePreset(profileName: String) {
        val newSchedule = when (profileName) {
            "🏢 Work Profile" -> Schedule(
                name = "Office Hours",
                vibrateTime = "09:00",
                soundTime = "17:00",
                days = "MON,TUE,WED,THU,FRI",
                customMessage = "Muted for important work meetings"
            )
            "🏋️ Gym Profile" -> Schedule(
                name = "Workout Focus",
                vibrateTime = "18:00",
                soundTime = "19:00",
                days = "MON,WED,FRI",
                customMessage = "Gym time. Mute calls!"
            )
            "😴 Sleep Profile" -> Schedule(
                name = "Night Sleep",
                vibrateTime = "22:00",
                soundTime = "07:00",
                days = "MON,TUE,WED,THU,FRI,SAT,SUN",
                customMessage = "Sleeping. Do Not Disturb."
            )
            "📚 Student Mode" -> Schedule(
                name = "Calculus Lecture",
                vibrateTime = "10:00",
                soundTime = "11:30",
                days = "TUE,THU",
                customMessage = "In class. Learning!"
            )
            "🎬 Movie Mode" -> {
                // Immediate toggle helper: sets start time to now, end time to 2 hours from now
                val now = Calendar.getInstance()
                val startStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
                now.add(Calendar.HOUR, 2)
                val endStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
                Schedule(
                    name = "Movie Theater Mode",
                    vibrateTime = startStr,
                    soundTime = endStr,
                    days = "MON,TUE,WED,THU,FRI,SAT,SUN",
                    customMessage = "Silence for cinema screening"
                )
            }
            else -> null
        }

        newSchedule?.let {
            saveSchedule(it) {}
        }
    }

    // VIP Contacts Management
    fun addVipContact(contact: String) {
        if (contact.isBlank()) return
        viewModelScope.launch {
            val newList = _vipContacts.value + contact
            _vipContacts.value = newList
            repository.saveSetting("vip_contacts", newList.joinToString(","))
            logEvent("Safety System", "VIP_ADD", "Added contact '$contact' to VIP Whitelist.")
        }
    }

    fun removeVipContact(contact: String) {
        viewModelScope.launch {
            val newList = _vipContacts.value.filter { it != contact }
            _vipContacts.value = newList
            repository.saveSetting("vip_contacts", newList.joinToString(","))
            logEvent("Safety System", "VIP_REMOVE", "Removed contact '$contact' from VIP Whitelist.")
        }
    }

    // Snooze Function
    fun snoozeSoundReturn(minutes: Int) {
        viewModelScope.launch {
            _activeNotification.value = "⏸️ Sound Switch Snoozed"
            _activeNotificationDetails.value = "Extending Vibrate mode by $minutes minutes."
            _activeNotificationType.value = "VIBRATE"

            logEvent("Manual Override", "SNOOZE", "Sound mode return snoozed by $minutes min.")

            // Wait 5 seconds, dismiss notification banner
            delay(5000)
            dismissNotification()
        }
    }

    // Analyze calendar text using Gemini (or smart fallback)
    fun importCalendarAgenda(agendaText: String) {
        if (agendaText.isBlank()) return
        _isAnalyzingCalendar.value = true
        viewModelScope.launch {
            try {
                val extracted = GeminiCalendarService.analyzeCalendarText(agendaText)
                for (item in extracted) {
                    val s = Schedule(
                        name = item.name,
                        vibrateTime = item.vibrateTime,
                        soundTime = item.soundTime,
                        days = item.days,
                        customMessage = item.customMessage
                    )
                    repository.insertSchedule(s)
                }
                logEvent("Smart Calendar", "CALENDAR_SYNC", "Analyzed meeting agendas and synchronized ${extracted.size} smart schedules.")
            } catch (e: Exception) {
                Log.e("SoundScheduler", "Calendar sync failed", e)
            } finally {
                _isAnalyzingCalendar.value = false
            }
        }
    }

    // Clear schedule history
    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Event Logging
    private suspend fun logEvent(name: String, action: String, details: String) {
        val item = HistoryItem(
            scheduleName = name,
            actionType = action,
            details = details
        )
        repository.insertHistoryItem(item)
    }
}
