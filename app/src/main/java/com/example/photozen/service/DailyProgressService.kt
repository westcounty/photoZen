package com.example.photozen.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.photozen.MainActivity
import com.example.photozen.R
import com.example.photozen.data.local.dao.DailyStatsDao
import com.example.photozen.data.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

/**
 * Foreground service that displays daily progress in status bar.
 * This keeps the app alive and displays motivational messages.
 */
@AndroidEntryPoint
class DailyProgressService : Service() {
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    @Inject
    lateinit var dailyStatsDao: DailyStatsDao
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 当前显示的激励文案索引，用于定期切换
    private var currentMessageIndex = Random.nextInt(MOTIVATION_MESSAGES.size)
    private var lastMessageChangeTime = 0L
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DailyProgressService onCreate")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DailyProgressService onStartCommand, action: ${intent?.action}")
        
        // Start foreground immediately with initial notification
        startForeground(PROGRESS_NOTIFICATION_ID, buildProgressNotification(0, 100, false))
        Log.d(TAG, "Service started foreground successfully")
        
        // Start observing progress updates
        observeProgressUpdates()
        
        return START_STICKY // Restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DailyProgressService onDestroy")
        serviceScope.cancel()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "DailyProgressService onTaskRemoved - attempting restart")
        
        // 当用户从最近任务中移除app时，尝试重新启动服务
        try {
            val restartIntent = Intent(applicationContext, DailyProgressService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart service on task removed", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Progress notification channel (low priority, silent)
            val progressChannel = NotificationChannel(
                PROGRESS_CHANNEL_ID,
                "每日整理进度",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "时刻督促自己完成每日整理目标"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(progressChannel)
            Log.d(TAG, "Notification channel created")
        }
    }
    
    private fun observeProgressUpdates() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        serviceScope.launch {
            combine(
                preferencesRepository.getDailyTaskTarget(),
                preferencesRepository.getDailyTaskEnabled(),
                dailyStatsDao.getStatsByDate(today)
            ) { target, enabled, stats ->
                Triple(stats?.count ?: 0, target, enabled)
            }.collect { (current, target, enabled) ->
                if (enabled) {
                    val isCompleted = current >= target
                    updateProgressNotification(current, target, isCompleted)
                }
            }
        }
    }
    
    /**
     * 获取激励文案，根据进度状态选择合适的消息
     */
    private fun getMotivationMessage(current: Int, target: Int, isCompleted: Boolean): Pair<String, String> {
        val now = System.currentTimeMillis()
        
        // 每5分钟切换一次文案
        if (now - lastMessageChangeTime > 5 * 60 * 1000) {
            currentMessageIndex = Random.nextInt(MOTIVATION_MESSAGES.size)
            lastMessageChangeTime = now
        }
        
        return when {
            isCompleted -> COMPLETED_MESSAGES.random()
            current == 0 -> START_MESSAGES.random()
            current < target / 2 -> PROGRESS_MESSAGES.random()
            else -> ALMOST_THERE_MESSAGES.random()
        }
    }
    
    private fun updateProgressNotification(current: Int, target: Int, isCompleted: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildProgressNotification(current, target, isCompleted)
        notificationManager.notify(PROGRESS_NOTIFICATION_ID, notification)
    }
    
    private fun buildProgressNotification(current: Int, target: Int, isCompleted: Boolean): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            PROGRESS_REQUEST_CODE,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val (title, subtitle) = getMotivationMessage(current, target, isCompleted)
        val progressText = "$current / $target 张"

        // 通知中心左侧的大图标（使用vector drawable转换为bitmap）
        val largeIcon = try {
            ContextCompat.getDrawable(this, R.drawable.ic_notification_large)?.toBitmap(96, 96)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load large icon", e)
            null
        }

        return NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText("$progressText · $subtitle")
            .setProgress(target, current.coerceAtMost(target), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }
    
    companion object {
        private const val TAG = "DailyProgressService"
        
        const val PROGRESS_CHANNEL_ID = "daily_progress_channel"
        const val PROGRESS_NOTIFICATION_ID = 2001
        const val PROGRESS_REQUEST_CODE = 2001
        
        // 激励文案 - 未开始
        private val START_MESSAGES = listOf(
            Pair("时刻督促自己", "开始今天的整理吧"),
            Pair("新的一天", "整理几张照片如何？"),
            Pair("你的相册在等你", "点击开始整理"),
            Pair("每天整理一点点", "养成好习惯")
        )
        
        // 激励文案 - 进行中
        private val PROGRESS_MESSAGES = listOf(
            Pair("继续加油！", "你做得很好"),
            Pair("保持节奏", "一张一张来"),
            Pair("整理中...", "每一张都算数"),
            Pair("时刻督促自己", "坚持就是胜利")
        )
        
        // 激励文案 - 快完成了
        private val ALMOST_THERE_MESSAGES = listOf(
            Pair("快完成了！", "再坚持一下"),
            Pair("胜利在望", "冲刺吧！"),
            Pair("就差一点点", "你可以的"),
            Pair("即将达成目标", "加把劲！")
        )
        
        // 激励文案 - 已完成
        private val COMPLETED_MESSAGES = listOf(
            Pair("🎉 今日目标已完成！", "太棒了"),
            Pair("✨ 完美达成！", "明天继续"),
            Pair("🏆 你真厉害！", "休息一下吧"),
            Pair("💪 目标达成！", "给自己点个赞")
        )
        
        // 通用激励文案（用于定期切换）
        private val MOTIVATION_MESSAGES = listOf(
            "你的相册在喊你",
            "照片们等翻牌中",
            "5分钟，给相册做个SPA",
            "删掉糊图，留下美好",
            "每天整理一点点",
            "好照片值得被看见",
            "给回忆做个减法"
        )
        
        /**
         * Start the service.
         */
        fun start(context: Context) {
            Log.d(TAG, "Starting DailyProgressService...")
            val intent = Intent(context, DailyProgressService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Service start intent sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
            }
        }
        
        /**
         * Stop the service.
         */
        fun stop(context: Context) {
            Log.d(TAG, "Stopping DailyProgressService...")
            val intent = Intent(context, DailyProgressService::class.java)
            context.stopService(intent)
        }
    }
}
