package com.example.photozen.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.photozen.MainActivity
import com.example.photozen.R
import com.example.photozen.data.local.dao.DailyStatsDao
import com.example.photozen.data.repository.PreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Entry point for accessing dependencies in Widget.
 * Using EntryPoint instead of @AndroidEntryPoint for more reliable injection in widgets.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyProgressWidgetEntryPoint {
    fun preferencesRepository(): PreferencesRepository
    fun dailyStatsDao(): DailyStatsDao
}

/**
 * Widget to display daily sorting progress.
 * Uses EntryPointAccessors for reliable dependency access in widget lifecycle.
 */
class DailyProgressWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive: ${intent.action}")
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "updateAppWidget for id: $appWidgetId")
        
        val pendingResult = goAsync()
        
        scope.launch {
            try {
                // Use EntryPointAccessors for reliable dependency injection
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    DailyProgressWidgetEntryPoint::class.java
                )
                val preferencesRepository = entryPoint.preferencesRepository()
                val dailyStatsDao = entryPoint.dailyStatsDao()
                
                // Read values directly to ensure we get the latest data
                val isEnabled = preferencesRepository.getDailyTaskEnabled().first()
                val target = preferencesRepository.getDailyTaskTarget().first()
                
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val stats = dailyStatsDao.getStatsByDateOneShot(today)
                val current = stats?.count ?: 0
                
                Log.d(TAG, "Direct read - enabled=$isEnabled, target=$target, current=$current, today=$today")
                
                val views = RemoteViews(context.packageName, R.layout.widget_daily_progress)
                
                // If daily task is not enabled, show disabled state
                if (!isEnabled) {
                    Log.d(TAG, "Daily task is disabled")
                    views.setTextViewText(R.id.widget_emoji, "😴")
                    views.setTextViewText(R.id.widget_status_text, "每日任务未开启")
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_progress_text, android.view.View.GONE)
                } else {
                    // Determine state based on actual progress
                    // States: NOT_STARTED (0), IN_PROGRESS (1 to target-1), COMPLETED (>= target)
                    val isCompleted = target > 0 && current >= target
                    val isInProgress = current > 0 && !isCompleted
                    val notStarted = current == 0
                    val progressPercent = if (target > 0) (current * 100 / target) else 0
                    
                    // Set emoji based on state - enhanced visual design
                    val emoji = when {
                        isCompleted -> "🏆"      // Trophy - task completed!
                        isInProgress -> "🔥"    // Flame - keep going!
                        else -> "🌅"             // Sunrise - new day, new start
                    }
                    
                    // Set status text based on state - more engaging messages
                    val statusText = when {
                        isCompleted -> "太棒了！今日完成"
                        isInProgress && progressPercent >= 50 -> "已过半！继续加油"
                        isInProgress -> "继续加油！"
                        else -> "新的一天，开始整理吧！"
                    }
                    
                    Log.d(TAG, "Setting emoji: $emoji, statusText: $statusText, isCompleted=$isCompleted, isInProgress=$isInProgress (current=$current, target=$target, progress=$progressPercent%)")
                    
                    views.setTextViewText(R.id.widget_emoji, emoji)
                    views.setTextViewText(R.id.widget_status_text, statusText)
                    
                    // Show/hide progress bar based on state
                    if (isCompleted) {
                        // Completed - hide progress bar, show celebration
                        views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                        views.setViewVisibility(R.id.widget_progress_text, android.view.View.GONE)
                    } else {
                        // Not completed (either in progress or not started) - show progress bar
                        views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.widget_progress_text, android.view.View.VISIBLE)
                        views.setProgressBar(R.id.widget_progress_bar, target, current, false)
                        views.setTextViewText(R.id.widget_progress_text, "$current / $target")
                    }
                }
                
                // Click to open app
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("open_daily_task", true)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "Widget updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget", e)
                // Show error state so user can at least click to open app
                try {
                    val views = RemoteViews(context.packageName, R.layout.widget_daily_progress)
                    views.setTextViewText(R.id.widget_emoji, "⚠️")
                    views.setTextViewText(R.id.widget_status_text, "点击打开")
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_progress_text, android.view.View.GONE)
                    
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to show error state", e2)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    companion object {
        private const val TAG = "DailyProgressWidget"
        
        /**
         * Trigger update for all daily progress widgets.
         */
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DailyProgressWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (widgetIds.isNotEmpty()) {
                val intent = Intent(context, DailyProgressWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
