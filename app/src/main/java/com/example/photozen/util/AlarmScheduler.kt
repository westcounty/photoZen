package com.example.photozen.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.photozen.receiver.DailyReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun scheduleDailyReminder(hour: Int, minute: Int) {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            putExtra("hour", hour)
            putExtra("minute", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        // Use setExactAndAllowWhileIdle for reliable reminders
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Handle case where SCHEDULE_EXACT_ALARM is not granted
            // Fallback to inexact or prompt user
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                10 * 60 * 1000, // 10 minute window
                pendingIntent
            )
        }
    }
    
    fun cancelDailyReminder() {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    companion object {
        const val DAILY_REMINDER_REQUEST_CODE = 1001
    }
}
