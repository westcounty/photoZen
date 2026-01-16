package com.example.photozen.receiver

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.util.WidgetUpdater
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
        
        Log.d(TAG, "onReceive: action=$action, photoId=$photoId")

        if (photoId != null && action != null) {
            val pendingResult = goAsync()
            
            scope.launch {
                try {
                    when (action) {
                        ACTION_WIDGET_KEEP -> {
                            Log.d(TAG, "Keeping photo: $photoId")
                            photoRepository.keepPhoto(photoId)
                        }
                        ACTION_WIDGET_TRASH -> {
                            Log.d(TAG, "Trashing photo: $photoId")
                            photoRepository.trashPhoto(photoId)
                        }
                        ACTION_WIDGET_MAYBE -> {
                            Log.d(TAG, "Maybe photo: $photoId")
                            photoRepository.maybePhoto(photoId)
                        }
                    }
                    
                    // Trigger widget update to show next photo
                    PhotoSorterWidget.triggerUpdate(context)
                    Log.d(TAG, "Widget update triggered")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing widget action", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "WidgetActionReceiver"
        const val ACTION_WIDGET_KEEP = "com.example.photozen.action.WIDGET_KEEP"
        const val ACTION_WIDGET_TRASH = "com.example.photozen.action.WIDGET_TRASH"
        const val ACTION_WIDGET_MAYBE = "com.example.photozen.action.WIDGET_MAYBE"
        const val EXTRA_PHOTO_ID = "photo_id"
    }
}
