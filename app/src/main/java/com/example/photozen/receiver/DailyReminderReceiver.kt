package com.example.photozen.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.photozen.MainActivity
import com.example.photozen.R
import com.example.photozen.util.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random

/**
 * BroadcastReceiver to handle daily reminder alarms.
 */
@AndroidEntryPoint
class DailyReminderReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var alarmScheduler: AlarmScheduler
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "DailyReminderReceiver onReceive called")
        
        // Show the notification
        showNotification(context)
        
        // Reschedule for next day
        val hour = intent.getIntExtra("hour", -1)
        val minute = intent.getIntExtra("minute", -1)
        
        Log.d(TAG, "Rescheduling reminder - hour: $hour, minute: $minute")
        
        if (hour != -1 && minute != -1) {
            alarmScheduler.scheduleDailyReminder(hour, minute)
        }
    }
    
    /**
     * Get a random notification message with title and content.
     */
    private fun getRandomNotificationMessage(): Pair<String, String> {
        val messages = listOf(
            // 轻松幽默风格
            Pair("你的相册在喊你！📸", "它说：主人，我好乱啊～"),
            Pair("照片们排队等翻牌中...", "今天轮到谁留下，谁说再见？"),
            Pair("叮！整理时间到 ⏰", "5分钟，给相册做个SPA"),
            
            // 鼓励行动风格
            Pair("今日整理挑战开始！🎯", "目标：比昨天少10张杂图"),
            Pair("相册瘦身计划进行中", "删掉糊图，留下美好"),
            Pair("每天整理一点点", "一个月后，相册焕然一新"),
            
            // 制造好奇心
            Pair("你的相册里藏着什么？", "点开看看，说不定有惊喜"),
            Pair("有些照片在等你做决定", "留下还是删除，你说了算"),
            Pair("解锁今日整理成就？", "来看看能连击多少张"),
            
            // 温馨提醒
            Pair("给回忆做个减法", "留下的每一张都是精选"),
            Pair("好照片值得被看见", "整理一下，让它们重见天日"),
            Pair("相册整理小分队上线！", "一起把杂乱变整洁"),
            
            // 轻松玩梗
            Pair("据说整理照片的人运气都不差", "信不信由你，试试看？"),
            Pair("手机内存告急？", "来，我们聊聊那些糊掉的照片"),
            Pair("今天也是元气满满的一天", "从整理几张照片开始吧")
        )
        
        return messages[Random.nextInt(messages.size)]
    }
    
    private fun showNotification(context: Context) {
        Log.d(TAG, "Showing notification")
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create channel - must be done before showing notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "每日任务提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "提醒每日整理照片"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
        
        // Content Intent (Open App)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get random notification message
        val (title, content) = getRandomNotificationMessage()
        Log.d(TAG, "Notification message - title: $title, content: $content")
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notification posted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification", e)
        }
    }
    
    companion object {
        private const val TAG = "DailyReminderReceiver"
        const val CHANNEL_ID = "daily_reminder_channel"
        const val NOTIFICATION_ID = 1001
    }
}
