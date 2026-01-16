package com.example.photozen.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
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
    
    /**
     * Check if the app can schedule exact alarms.
     * On Android 12+ (API 31+), this requires the SCHEDULE_EXACT_ALARM permission.
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
    
    /**
     * Get the intent to open exact alarm settings.
     * Used to prompt the user to grant permission.
     */
    fun getExactAlarmSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            null
        }
    }
    
    fun scheduleDailyReminder(hour: Int, minute: Int) {
        Log.d(TAG, "Scheduling daily reminder for $hour:$minute")
        
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
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
        
        Log.d(TAG, "Alarm time: ${calendar.time}")
        
        try {
            // Try to schedule exact alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm successfully")
                } else {
                    // Can't schedule exact, use inexact with window
                    alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        5 * 60 * 1000L, // 5 minute window
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled window alarm (no exact permission)")
                }
            } else {
                // Pre-Android 12, can always use exact alarms
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm (pre-S)")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm", e)
            // Fallback to inexact alarm
            try {
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    10 * 60 * 1000L, // 10 minute window
                    pendingIntent
                )
                Log.d(TAG, "Fallback to window alarm after SecurityException")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule any alarm", e2)
            }
        }
    }
    
    fun cancelDailyReminder() {
        Log.d(TAG, "Cancelling daily reminder")
        
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    companion object {
        private const val TAG = "AlarmScheduler"
        const val DAILY_REMINDER_REQUEST_CODE = 1001
        const val ACTION_DAILY_REMINDER = "com.example.photozen.DAILY_REMINDER"
    }
}
