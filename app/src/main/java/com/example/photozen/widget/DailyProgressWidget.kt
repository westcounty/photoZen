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
import com.example.photozen.domain.usecase.GetDailyTaskStatusUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Widget to display daily sorting progress.
 */
@AndroidEntryPoint
class DailyProgressWidget : AppWidgetProvider() {

    @Inject
    lateinit var getDailyTaskStatusUseCase: GetDailyTaskStatusUseCase

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
                val status = getDailyTaskStatusUseCase().first()
                Log.d(TAG, "Daily status: current=${status.current}, target=${status.target}, completed=${status.isCompleted}")
                
                val views = RemoteViews(context.packageName, R.layout.widget_daily_progress)
                
                // Determine emoji and status text based on actual completion status
                // Check if task is truly completed (current >= target)
                val isActuallyCompleted = status.current >= status.target
                
                val emoji = when {
                    isActuallyCompleted -> "🥳"
                    status.current > 0 -> "🙂"
                    else -> "😢"
                }
                
                val statusText = when {
                    isActuallyCompleted -> "任务完成！"
                    status.current > 0 -> "继续加油"
                    else -> "开始整理吧"
                }
                
                Log.d(TAG, "Setting emoji: $emoji, statusText: $statusText")
                
                views.setTextViewText(R.id.widget_emoji, emoji)
                views.setTextViewText(R.id.widget_status_text, statusText)
                
                if (isActuallyCompleted) {
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_progress_text, android.view.View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_progress_text, android.view.View.VISIBLE)
                    views.setProgressBar(R.id.widget_progress_bar, status.target, status.current, false)
                    views.setTextViewText(R.id.widget_progress_text, "${status.current} / ${status.target}")
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
