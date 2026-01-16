package com.example.photozen.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.photozen.MainActivity
import com.example.photozen.R
import com.example.photozen.util.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver to handle daily reminder alarms.
 */
@AndroidEntryPoint
class DailyReminderReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var alarmScheduler: AlarmScheduler
    
    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)
        
        // Reschedule for next day
        val hour = intent.getIntExtra("hour", -1)
        val minute = intent.getIntExtra("minute", -1)
        
        if (hour != -1 && minute != -1) {
            alarmScheduler.scheduleDailyReminder(hour, minute)
        }
    }
    
    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "每日任务提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "提醒每日整理照片"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Content Intent (Open App)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Replace with valid icon
            .setContentTitle("该整理照片了！")
            .setContentText("每天整理一点点，告别杂乱相册。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    companion object {
        const val CHANNEL_ID = "daily_reminder_channel"
        const val NOTIFICATION_ID = 1001
    }
}
