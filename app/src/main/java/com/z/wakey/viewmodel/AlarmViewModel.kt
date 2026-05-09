package com.z.wakey.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z.wakey.alarm.AlarmScheduler
import com.z.wakey.data.Alarm
import com.z.wakey.data.AlarmStorage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = AlarmStorage.get(application)

    val alarms: StateFlow<List<Alarm>> = storage.alarms

    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val saved = storage.save(alarm)
            if (saved.isEnabled) AlarmScheduler.schedule(getApplication(), saved)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), alarm)
            storage.save(alarm)
            if (alarm.isEnabled) AlarmScheduler.schedule(getApplication(), alarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), alarm)
            storage.delete(alarm.id)
        }
    }

    fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = enabled)
            storage.save(updated)
            if (enabled) AlarmScheduler.schedule(getApplication(), updated)
            else AlarmScheduler.cancel(getApplication(), alarm)
        }
    }

    fun getAlarmById(id: Int): Alarm? = storage.getById(id)
}
