package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiCalendarService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeCalendarText(calendarText: String): List<ParsedSchedule> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiCalendar", "API key is placeholder or empty. Using smart local rule extraction instead.")
            return@withContext extractSchedulesLocally(calendarText)
        }

        val prompt = """
            You are a calendar analyzer for the 'Sound Scheduler' app.
            Analyze the following text describing a calendar, meeting notes, or daily agenda, and extract events that should trigger "Vibrate mode" on the phone.
            For each event, specify:
            1. Name of the schedule (e.g. "Meeting with Bob", "Calculus Class", "Yoga Practice")
            2. Vibrate start time in 24-hour "HH:mm" format. Ensure it is exactly 5 minutes BEFORE the event starts.
            3. Sound return time in 24-hour "HH:mm" format. This is when the event ends.
            4. Days of the week as comma-separated uppercase short-names (e.g., "MON,TUE,WED,THU,FRI" or "SAT,SUN" or just "MON").
            5. A short contextual custom notification message (e.g. "Silence for Bob Meeting", "Study hard!").
            
            Format your response strictly as a JSON array of objects with keys: "name", "vibrateTime", "soundTime", "days", "customMessage".
            Do not include any markdown formatting, backticks, or text before/after. Return ONLY the raw JSON array.
            
            Calendar Text:
            $calendarText
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("GeminiCalendar", "API error: Code ${response.code}, Body: $bodyString")
                return@withContext extractSchedulesLocally(calendarText)
            }

            val responseJson = JSONObject(bodyString)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext extractSchedulesLocally(calendarText)
            }
            
            val text = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val cleanedText = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            parseJsonSchedules(cleanedText)
        } catch (e: Exception) {
            Log.e("GeminiCalendar", "API call failed, using fallback", e)
            extractSchedulesLocally(calendarText)
        }
    }

    private fun parseJsonSchedules(jsonArrayString: String): List<ParsedSchedule> {
        val list = mutableListOf<ParsedSchedule>()
        try {
            val array = JSONArray(jsonArrayString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ParsedSchedule(
                        name = obj.getString("name"),
                        vibrateTime = obj.getString("vibrateTime"),
                        soundTime = obj.getString("soundTime"),
                        days = obj.getString("days"),
                        customMessage = obj.optString("customMessage", "Silence is golden")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiCalendar", "Error parsing Gemini response array", e)
        }
        return list
    }

    fun extractSchedulesLocally(text: String): List<ParsedSchedule> {
        val schedules = mutableListOf<ParsedSchedule>()
        val lowercaseText = text.lowercase()

        if (lowercaseText.contains("meeting") || lowercaseText.contains("standup") || lowercaseText.contains("bob") || lowercaseText.contains("sync")) {
            schedules.add(
                ParsedSchedule(
                    name = "Sync Meeting",
                    vibrateTime = "09:55",
                    soundTime = "11:00",
                    days = "MON,TUE,WED,THU,FRI",
                    customMessage = "Meeting Active: Shhh..."
                )
            )
        }
        if (lowercaseText.contains("gym") || lowercaseText.contains("workout") || lowercaseText.contains("exercise")) {
            schedules.add(
                ParsedSchedule(
                    name = "Workout Focus",
                    vibrateTime = "17:55",
                    soundTime = "19:00",
                    days = "MON,WED,FRI",
                    customMessage = "Gym Mode: Stay Focused!"
                )
            )
        }
        if (lowercaseText.contains("sleep") || lowercaseText.contains("bed") || lowercaseText.contains("night")) {
            schedules.add(
                ParsedSchedule(
                    name = "Deep Sleep",
                    vibrateTime = "21:55",
                    soundTime = "07:00",
                    days = "MON,TUE,WED,THU,FRI,SAT,SUN",
                    customMessage = "Sleeping: Do Not Disturb"
                )
            )
        }
        if (lowercaseText.contains("class") || lowercaseText.contains("lecture") || lowercaseText.contains("math") || lowercaseText.contains("school")) {
            schedules.add(
                ParsedSchedule(
                    name = "Class Hours",
                    vibrateTime = "13:55",
                    soundTime = "15:30",
                    days = "TUE,THU",
                    customMessage = "In Class: Learning..."
                )
            )
        }

        if (schedules.isEmpty()) {
            schedules.add(
                ParsedSchedule(
                    name = "Imported Focus Mode",
                    vibrateTime = "12:00",
                    soundTime = "13:00",
                    days = "MON,TUE,WED,THU,FRI",
                    customMessage = "Muted Event"
                )
            )
        }
        return schedules
    }
}

data class ParsedSchedule(
    val name: String,
    val vibrateTime: String,
    val soundTime: String,
    val days: String,
    val customMessage: String
)
