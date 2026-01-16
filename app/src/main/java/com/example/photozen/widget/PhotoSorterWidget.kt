package com.example.photozen.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.photozen.MainActivity
import com.example.photozen.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Widget to sort photos from desktop.
 * Currently simplified to open app.
 */
@AndroidEntryPoint
class PhotoSorterWidget : AppWidgetProvider() {

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
        val views = RemoteViews(context.packageName, R.layout.widget_photo_sorter)
        
        // Open app logic
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_photo, pendingIntent)
        
        // TODO: Implement actions (PendingIntent to BroadcastReceiver)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
