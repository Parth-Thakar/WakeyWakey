package com.z.wakey.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AlarmStorage private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("wakey_alarms", Context.MODE_PRIVATE)
    private val _alarms = MutableStateFlow(load())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    companion object {
        @Volatile private var INSTANCE: AlarmStorage? = null

        fun get(context: Context): AlarmStorage =
            INSTANCE ?: synchronized(this) {
                AlarmStorage(context.applicationContext).also { INSTANCE = it }
            }
    }

    fun getAll(): List<Alarm> = _alarms.value

    fun getById(id: Int): Alarm? = _alarms.value.find { it.id == id }

    fun save(alarm: Alarm): Alarm {
        val list = _alarms.value.toMutableList()
        return if (alarm.id == 0) {
            val newId = (list.maxOfOrNull { it.id } ?: 0) + 1
            val saved = alarm.copy(id = newId)
            list.add(saved)
            persist(list)
            _alarms.value = list.toList()
            saved
        } else {
            val idx = list.indexOfFirst { it.id == alarm.id }
            if (idx >= 0) list[idx] = alarm else list.add(alarm)
            persist(list)
            _alarms.value = list.toList()
            alarm
        }
    }

    fun delete(id: Int) {
        val list = _alarms.value.filter { it.id != id }
        persist(list)
        _alarms.value = list
    }

    private fun load(): List<Alarm> = runCatching {
        val json = prefs.getString("alarms", "[]") ?: "[]"
        val arr = JSONArray(json)
        List(arr.length()) { i -> arr.getJSONObject(i).toAlarm() }
    }.getOrDefault(emptyList())

    private fun persist(alarms: List<Alarm>) = runCatching {
        val arr = JSONArray()
        alarms.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("alarms", arr.toString()).apply()
    }

    private fun JSONObject.toAlarm() = Alarm(
        id = getInt("id"),
        hour = getInt("hour"),
        minute = getInt("minute"),
        label = optString("label", ""),
        isEnabled = optBoolean("isEnabled", true),
        repeatDays = optString("repeatDays", ""),
        ringtonePath = optString("ringtonePath", ""),
        mathQuestionCount = optInt("mathQuestionCount", 5),
        isVibrate = optBoolean("isVibrate", true),
        dismissType = runCatching { DismissType.valueOf(optString("dismissType", "MATH")) }.getOrDefault(DismissType.MATH),
        fingerRoundCount = optInt("fingerRoundCount", 5)
    )

    private fun Alarm.toJson() = JSONObject().apply {
        put("id", id)
        put("hour", hour)
        put("minute", minute)
        put("label", label)
        put("isEnabled", isEnabled)
        put("repeatDays", repeatDays)
        put("ringtonePath", ringtonePath)
        put("mathQuestionCount", mathQuestionCount)
        put("isVibrate", isVibrate)
        put("dismissType", dismissType.name)
        put("fingerRoundCount", fingerRoundCount)
    }
}
