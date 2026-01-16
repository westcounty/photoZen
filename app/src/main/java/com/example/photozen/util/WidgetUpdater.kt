package com.example.photozen.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.photozen.widget.DailyProgressWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to update all app widgets.
 * Should be called whenever data that widgets display is changed.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Update all Daily Progress widgets.
     */
    fun updateDailyProgressWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DailyProgressWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        
        if (widgetIds.isNotEmpty()) {
            // Send broadcast to update widgets
            val intent = Intent(context, DailyProgressWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            }
            context.sendBroadcast(intent)
        }
    }
    
    /**
     * Update all widgets.
     */
    fun updateAllWidgets() {
        updateDailyProgressWidgets()
    }
}
