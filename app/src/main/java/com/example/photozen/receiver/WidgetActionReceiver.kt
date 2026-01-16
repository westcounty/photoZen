package com.example.photozen.receiver

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.widget.PhotoSorterWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver to handle actions from the PhotoSorterWidget (Keep/Trash/Maybe).
 */
@AndroidEntryPoint
class WidgetActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var photoRepository: PhotoRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val photoId = intent.getStringExtra(EXTRA_PHOTO_ID)

        if (photoId != null && action != null) {
            scope.launch {
                when (action) {
                    ACTION_WIDGET_KEEP -> {
                        photoRepository.keepPhoto(photoId)
                    }
                    ACTION_WIDGET_TRASH -> {
                        photoRepository.trashPhoto(photoId)
                    }
                    ACTION_WIDGET_MAYBE -> {
                        photoRepository.maybePhoto(photoId)
                    }
                }
                
                // Trigger widget update to show next photo
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetComponent = ComponentName(context, PhotoSorterWidget::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
                
                val updateIntent = Intent(context, PhotoSorterWidget::class.java).apply {
                    this.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(updateIntent)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_KEEP = "com.example.photozen.action.WIDGET_KEEP"
        const val ACTION_WIDGET_TRASH = "com.example.photozen.action.WIDGET_TRASH"
        const val ACTION_WIDGET_MAYBE = "com.example.photozen.action.WIDGET_MAYBE"
        const val EXTRA_PHOTO_ID = "photo_id"
    }
}
