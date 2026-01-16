package com.example.photozen.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.example.photozen.MainActivity
import com.example.photozen.R
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.receiver.WidgetActionReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Widget to sort photos from desktop.
 * Displays a random unsorted photo and allows actions.
 */
@AndroidEntryPoint
class PhotoSorterWidget : AppWidgetProvider() {

    @Inject
    lateinit var photoRepository: PhotoRepository

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
        
        // Use goAsync for longer operations
        val pendingResult = goAsync()
        
        scope.launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_photo_sorter)
                
                // Fetch random unsorted photo
                val photo = withContext(Dispatchers.IO) {
                    try {
                        photoRepository.getRandomUnsortedPhoto()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get random photo", e)
                        null
                    }
                }
                
                Log.d(TAG, "Photo fetched: ${photo?.id}, uri: ${photo?.systemUri}")
                
                if (photo != null) {
                    views.setViewVisibility(R.id.widget_empty_text, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_photo, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_actions, android.view.View.VISIBLE)
                    
                    // Load image bitmap
                    val bitmap = withContext(Dispatchers.IO) {
                        loadBitmap(context, photo.systemUri)
                    }
                    
                    if (bitmap != null) {
                        Log.d(TAG, "Bitmap loaded: ${bitmap.width}x${bitmap.height}")
                        views.setImageViewBitmap(R.id.widget_photo, bitmap)
                    } else {
                        Log.w(TAG, "Bitmap is null, using placeholder")
                        views.setImageViewResource(R.id.widget_photo, R.drawable.ic_launcher_background)
                    }
                    
                    // Action: Trash
                    val trashIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                        action = WidgetActionReceiver.ACTION_WIDGET_TRASH
                        putExtra(WidgetActionReceiver.EXTRA_PHOTO_ID, photo.id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val trashPendingIntent = PendingIntent.getBroadcast(
                        context,
                        photo.id.hashCode() + appWidgetId * 1000 + 1,
                        trashIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_trash, trashPendingIntent)
                    
                    // Action: Keep
                    val keepIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                        action = WidgetActionReceiver.ACTION_WIDGET_KEEP
                        putExtra(WidgetActionReceiver.EXTRA_PHOTO_ID, photo.id)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val keepPendingIntent = PendingIntent.getBroadcast(
                        context,
                        photo.id.hashCode() + appWidgetId * 1000 + 2,
                        keepIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_keep, keepPendingIntent)
                    
                    // Click photo to open app
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("photo_id", photo.id)
                    }
                    val appPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        appIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_photo, appPendingIntent)
                    
                } else {
                    Log.d(TAG, "No photo available, showing empty state")
                    // No photos left
                    views.setViewVisibility(R.id.widget_empty_text, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_photo, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_actions, android.view.View.GONE)
                    
                    // Click empty state to open app
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val appPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        appIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_empty_text, appPendingIntent)
                }
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "Widget updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private fun loadBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            Log.d(TAG, "Loading bitmap from: $uri")
            
            // First, get image dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            
            // Calculate sample size for widget (max 500px)
            val maxSize = 500
            var sampleSize = 1
            if (options.outWidth > maxSize || options.outHeight > maxSize) {
                val halfWidth = options.outWidth / 2
                val halfHeight = options.outHeight / 2
                while ((halfWidth / sampleSize) >= maxSize && (halfHeight / sampleSize) >= maxSize) {
                    sampleSize *= 2
                }
            }
            
            Log.d(TAG, "Image size: ${options.outWidth}x${options.outHeight}, sampleSize: $sampleSize")
            
            // Now decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            }
            
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap", e)
            null
        }
    }
    
    companion object {
        private const val TAG = "PhotoSorterWidget"
        
        /**
         * Trigger update for all photo sorter widgets.
         */
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PhotoSorterWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (widgetIds.isNotEmpty()) {
                val intent = Intent(context, PhotoSorterWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
