package com.example.photozen.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
import java.io.IOException
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
            val views = RemoteViews(context.packageName, R.layout.widget_photo_sorter)
            
            // Fetch random unsorted photo
            val photo = withContext(Dispatchers.IO) {
                photoRepository.getRandomUnsortedPhoto()
            }
            
            if (photo != null) {
                // Load image bitmap
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val uri = Uri.parse(photo.systemUri)
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            // Decode bounds first to avoid OOM
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            // BitmapFactory.decodeStream(inputStream, null, options) // InputStream cannot be reused
                            // Re-open input stream for actual decode
                            // Simplified approach: Decode with sample size
                            
                            val decodeOptions = BitmapFactory.Options().apply {
                                inSampleSize = 4 // Subsample to reduce memory
                            }
                            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                        }
                    } catch (e: IOException) {
                        null
                    }
                }
                
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widget_photo, bitmap)
                } else {
                    views.setImageViewResource(R.id.widget_photo, R.drawable.ic_launcher_background)
                }
                
                // Action: Trash
                val trashIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = WidgetActionReceiver.ACTION_WIDGET_TRASH
                    putExtra(WidgetActionReceiver.EXTRA_PHOTO_ID, photo.id)
                }
                val trashPendingIntent = PendingIntent.getBroadcast(
                    context,
                    photo.id.hashCode() + 1, // Unique Request Code
                    trashIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_trash, trashPendingIntent)
                
                // Action: Keep
                val keepIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                    action = WidgetActionReceiver.ACTION_WIDGET_KEEP
                    putExtra(WidgetActionReceiver.EXTRA_PHOTO_ID, photo.id)
                }
                val keepPendingIntent = PendingIntent.getBroadcast(
                    context,
                    photo.id.hashCode() + 2,
                    keepIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_keep, keepPendingIntent)
                
                // Click photo to open app (Quick Tag mode for this photo ideally, but general open for now)
                val appIntent = Intent(context, MainActivity::class.java)
                val appPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_photo, appPendingIntent)
                
            } else {
                // No photos left
                views.setImageViewResource(R.id.widget_photo, R.drawable.ic_launcher_foreground) // Placeholder
                // TODO: Show "All Done" text if layout supports it, currently just image view
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
