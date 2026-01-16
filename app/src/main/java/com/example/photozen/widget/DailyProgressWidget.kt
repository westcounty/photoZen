package com.example.photozen.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            val status = getDailyTaskStatusUseCase().first()
            
            val views = RemoteViews(context.packageName, R.layout.widget_daily_progress)
            views.setTextViewText(R.id.widget_title, "每日任务")
            views.setTextViewText(R.id.widget_progress_text, "${status.current} / ${status.target}")
            views.setProgressBar(R.id.widget_progress_bar, status.target, status.current, false)
            
            // Click to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_progress_text, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_progress_bar, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
