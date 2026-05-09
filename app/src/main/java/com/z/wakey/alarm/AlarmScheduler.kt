package com.z.wakey.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.z.wakey.data.Alarm
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
        }
        val repeatDays = parseDays(alarm.repeatDays)

        if (repeatDays.isEmpty()) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val pending = PendingIntent.getBroadcast(
                context, alarm.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
        } else {
            repeatDays.forEach { day -> scheduleForDay(context, alarmManager, alarm, day, intent) }
        }
    }

    private fun scheduleForDay(
        context: Context,
        alarmManager: AlarmManager,
        alarm: Alarm,
        day: Int,
        baseIntent: Intent
    ) {
        val calDay = when (day) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> return
        }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, calDay)
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        val requestCode = alarm.id * 10 + day
        val pending = PendingIntent.getBroadcast(
            context, requestCode, baseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
    }

    fun cancel(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val repeatDays = parseDays(alarm.repeatDays)

        if (repeatDays.isEmpty()) {
            val pending = PendingIntent.getBroadcast(
                context, alarm.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
        } else {
            for (day in 1..7) {
                val pending = PendingIntent.getBroadcast(
                    context, alarm.id * 10 + day, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pending)
            }
        }
    }

    fun rescheduleAfterFired(context: Context, alarm: Alarm) {
        if (parseDays(alarm.repeatDays).isNotEmpty()) {
            schedule(context, alarm)
        }
    }

    private fun parseDays(repeatDays: String): List<Int> =
        repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
}
