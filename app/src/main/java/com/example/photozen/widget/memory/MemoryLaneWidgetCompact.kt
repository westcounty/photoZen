package com.example.photozen.widget.memory

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.example.photozen.MainActivity
import com.example.photozen.R
import com.example.photozen.data.local.entity.PhotoEntity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Memory Lane Widget - Compact (2x2) Version
 *
 * A minimal widget showing photo with time text and delete/refresh buttons.
 */
class MemoryLaneWidgetCompact : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action ?: return

        when (action) {
            ACTION_TRASH, ACTION_REFRESH -> {
                val pendingResult = goAsync()
                handleActionAsync(context, intent, action, pendingResult)
            }
        }
    }

    private fun handleActionAsync(
        context: Context,
        intent: Intent,
        action: String,
        pendingResult: PendingResult
    ) {
        scope.launch {
            try {
                when (action) {
                    ACTION_TRASH -> handleTrashActionInternal(context, intent)
                    ACTION_REFRESH -> handleRefreshActionInternal(context, intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidgetCompact: error handling action %s", action)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("MemoryLaneWidgetCompact: onUpdate widgetIds=%s", appWidgetIds.contentToString())
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.d("MemoryLaneWidgetCompact: onDeleted widgetIds=%s", appWidgetIds.contentToString())

        // Clean up per-widget preferences
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    MemoryLaneWidgetEntryPoint::class.java
                )
                val preferencesRepository = entryPoint.preferencesRepository()

                for (widgetId in appWidgetIds) {
                    preferencesRepository.deleteWidgetConfig(widgetId)
                    Timber.d("MemoryLaneWidgetCompact: cleaned up config for widget %d", widgetId)
                }
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidgetCompact: failed to clean up widget config")
            }
        }

        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        Timber.d("MemoryLaneWidgetCompact: onEnabled - first compact widget added")
        super.onEnabled(context)

        // Schedule periodic refresh when first widget is added
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    MemoryLaneWidgetEntryPoint::class.java
                )
                val preferencesRepository = entryPoint.preferencesRepository()
                val intervalMinutes = preferencesRepository.getMemoryLaneRefreshIntervalSync()

                MemoryLaneWidgetWorker.schedule(context, intervalMinutes)
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidgetCompact: failed to schedule refresh")
            }
        }
    }

    override fun onDisabled(context: Context) {
        Timber.d("MemoryLaneWidgetCompact: onDisabled - last compact widget removed")
        super.onDisabled(context)

        // Cancel periodic refresh when last widget is removed
        scope.launch {
            try {
                val hasStandard = MemoryLaneWidget.hasActiveWidgets(context)
                val hasLarge = MemoryLaneWidgetLarge.hasActiveWidgets(context)

                if (!hasStandard && !hasLarge) {
                    MemoryLaneWidgetWorker.cancel(context)
                    Timber.d("MemoryLaneWidgetCompact: canceled refresh - no more widgets")
                }
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidgetCompact: failed to cancel refresh")
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val pendingResult = goAsync()

        scope.launch {
            try {
                updateAppWidgetSuspend(context, appWidgetManager, appWidgetId)
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidgetCompact: error updating widget")
                showErrorState(context, appWidgetManager, appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateAppWidgetSuspend(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MemoryLaneWidgetEntryPoint::class.java
        )
        val photoRepository = entryPoint.photoRepository()
        val preferencesRepository = entryPoint.preferencesRepository()

        // Read per-widget settings (falls back to global if not set)
        val thisDayPriority = preferencesRepository.getWidgetThisDayPriority(appWidgetId)
        val usePoeticTime = preferencesRepository.getWidgetPoeticTime(appWidgetId)
        val refreshIntervalMinutes = preferencesRepository.getWidgetRefreshInterval(appWidgetId)

        // Check if scheduled refresh should get a new photo
        val lastRefreshTime = preferencesRepository.getWidgetLastRefreshTime(appWidgetId)
        val currentTime = System.currentTimeMillis()
        val intervalMillis = refreshIntervalMinutes * 60 * 1000L
        val shouldForceRefresh = (currentTime - lastRefreshTime) >= intervalMillis

        // Try to use cached photo first (to prevent auto-refresh on visibility change)
        val cachedPhotoId = preferencesRepository.getWidgetCurrentPhotoId(appWidgetId)
        var photo: PhotoEntity? = null

        if (!shouldForceRefresh && cachedPhotoId != null) {
            val cachedPhoto = photoRepository.getPhotoById(cachedPhotoId)
            if (cachedPhoto != null && cachedPhoto.status == com.example.photozen.data.model.PhotoStatus.UNSORTED) {
                photo = cachedPhoto
                Timber.d("MemoryLaneWidgetCompact: using cached photo id=%s", cachedPhotoId)
            }
        }

        // If no valid cached photo or refresh interval passed, get a new random one
        if (photo == null) {
            photo = photoRepository.getRandomMemoryPhoto(preferThisDayInHistory = thisDayPriority, widgetId = appWidgetId)
            if (photo != null) {
                preferencesRepository.setWidgetCurrentPhotoId(appWidgetId, photo.id)
                preferencesRepository.setWidgetLastRefreshTime(appWidgetId, currentTime)
                Timber.d("MemoryLaneWidgetCompact: refreshed to new photo (interval passed: %b)", shouldForceRefresh)
            }
        }

        val views = RemoteViews(context.packageName, R.layout.widget_memory_lane_compact)

        if (photo != null) {
            Timber.d("MemoryLaneWidgetCompact: displaying photo id=%s", photo.id)

            // Show photo content
            views.setViewVisibility(R.id.memory_photo_container, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.memory_empty_state, android.view.View.GONE)

            // Format time display (single line for compact)
            val timeText = MemoryTimeFormatter.format(photo.dateTaken, usePoeticTime).replace("\n", " ")
            views.setTextViewText(R.id.memory_time_text, timeText)

            // Check if it's "this day in history"
            val isThisDayInHistory = MemoryTimeFormatter.isThisDayInHistory(photo.dateTaken)
            if (isThisDayInHistory) {
                views.setViewVisibility(R.id.memory_this_day_badge, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.memory_this_day_badge, android.view.View.GONE)
            }

            // Load photo using Coil
            loadPhotoIntoWidget(context, photo, views, appWidgetId, appWidgetManager)

            // Click photo to open app
            setupPhotoClickIntent(context, views, appWidgetId, photo.id)

            // Click time text to open settings
            setupSettingsClickIntent(context, views, appWidgetId)

            // Setup delete button (move to trash)
            setupTrashButtonIntent(context, views, appWidgetId, photo.id)

            // Setup refresh button
            setupRefreshButtonIntent(context, views, appWidgetId)

        } else {
            Timber.d("MemoryLaneWidgetCompact: no photos available, showing empty state")
            showEmptyState(context, views, appWidgetId)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private suspend fun loadPhotoIntoWidget(
        context: Context,
        photo: PhotoEntity,
        views: RemoteViews,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager
    ) {
        try {
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(photo.systemUri)
                .size(200, 200) // Smaller size for compact widget
                .allowHardware(false) // Required for RemoteViews
                .build()

            val result = imageLoader.execute(request)
            val bitmap = result.image?.toBitmap()

            if (bitmap != null) {
                views.setImageViewBitmap(R.id.memory_photo_image, bitmap)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            Timber.e(e, "MemoryLaneWidgetCompact: failed to load photo")
        }
    }

    private fun setupPhotoClickIntent(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        photoId: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MemoryLaneWidget.EXTRA_OPEN_PHOTO_ID, photoId)
            putExtra(MemoryLaneWidget.EXTRA_FROM_MEMORY_WIDGET, true)
            putExtra(MemoryLaneWidget.EXTRA_WIDGET_ID, appWidgetId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Only set click on the photo image, not the whole container (to allow button clicks)
        views.setOnClickPendingIntent(R.id.memory_photo_image, pendingIntent)
    }

    /**
     * Set up time text click intent to open widget configuration.
     */
    private fun setupSettingsClickIntent(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        val intent = Intent(context, MemoryLaneWidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId * 100 + 4,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.memory_time_text, pendingIntent)
    }

    /**
     * Set up delete button (move to trash) intent.
     */
    private fun setupTrashButtonIntent(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        photoId: String
    ) {
        val intent = Intent(context, MemoryLaneWidgetCompact::class.java).apply {
            action = ACTION_TRASH
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_PHOTO_ID, photoId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.memory_btn_delete, pendingIntent)
    }

    /**
     * Set up refresh button intent.
     */
    private fun setupRefreshButtonIntent(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        val intent = Intent(context, MemoryLaneWidgetCompact::class.java).apply {
            action = ACTION_REFRESH
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.memory_btn_refresh, pendingIntent)
    }

    /**
     * Handle trash action - move photo to trash and refresh widget.
     */
    private suspend fun handleTrashActionInternal(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val photoId = intent.getStringExtra(EXTRA_PHOTO_ID) ?: return

        Timber.d("MemoryLaneWidgetCompact: handleTrashAction photoId=%s, widgetId=%d", photoId, widgetId)

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MemoryLaneWidgetEntryPoint::class.java
        )
        val photoRepository = entryPoint.photoRepository()

        // Update photo status to TRASH
        photoRepository.trashPhoto(photoId)

        // Show feedback toast
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            Toast.makeText(context, "告别这份记忆 👋", Toast.LENGTH_SHORT).show()
        }

        // Refresh widget with new photo
        refreshWidgetAndWait(context, widgetId)
    }

    /**
     * Handle refresh action - get new photo.
     */
    private suspend fun handleRefreshActionInternal(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        Timber.d("MemoryLaneWidgetCompact: handleRefreshAction widgetId=%d", widgetId)

        // Force refresh by clearing cached photo
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MemoryLaneWidgetEntryPoint::class.java
        )
        val preferencesRepository = entryPoint.preferencesRepository()
        preferencesRepository.setWidgetCurrentPhotoId(widgetId, null)
        preferencesRepository.setWidgetLastRefreshTime(widgetId, 0)

        refreshWidgetAndWait(context, widgetId)
    }

    /**
     * Refresh widget and wait for completion.
     */
    private suspend fun refreshWidgetAndWait(context: Context, widgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Update specific widget
            updateAppWidgetSuspend(context, appWidgetManager, widgetId)
        } else {
            // Update all widgets
            val componentName = ComponentName(context, MemoryLaneWidgetCompact::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in widgetIds) {
                updateAppWidgetSuspend(context, appWidgetManager, id)
            }
        }
    }

    private fun showEmptyState(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setViewVisibility(R.id.memory_photo_container, android.view.View.GONE)
        views.setViewVisibility(R.id.memory_empty_state, android.view.View.VISIBLE)
        views.setTextViewText(R.id.memory_empty_text, "无照片")
        views.setTextViewText(R.id.memory_empty_emoji, "\uD83C\uDF89")

        // Click to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.memory_empty_state, pendingIntent)
    }

    private fun showErrorState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_memory_lane_compact)
            views.setViewVisibility(R.id.memory_photo_container, android.view.View.GONE)
            views.setViewVisibility(R.id.memory_empty_state, android.view.View.VISIBLE)
            views.setTextViewText(R.id.memory_empty_text, "点击打开")
            views.setTextViewText(R.id.memory_empty_emoji, "\u26A0\uFE0F")

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.memory_empty_state, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            Timber.e(e, "MemoryLaneWidgetCompact: failed to show error state")
        }
    }

    companion object {
        // Action constants
        const val ACTION_TRASH = "com.example.photozen.widget.memory.compact.ACTION_TRASH"
        const val ACTION_REFRESH = "com.example.photozen.widget.memory.compact.ACTION_REFRESH"

        // Extra constants
        const val EXTRA_WIDGET_ID = "extra_widget_id"
        const val EXTRA_PHOTO_ID = "extra_photo_id"

        /**
         * Trigger update for all Memory Lane Compact widgets.
         */
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MemoryLaneWidgetCompact::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (widgetIds.isNotEmpty()) {
                Timber.d("MemoryLaneWidgetCompact: triggerUpdate for %d widgets", widgetIds.size)
                val intent = Intent(context, MemoryLaneWidgetCompact::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        /**
         * Check if there are any active Memory Lane Compact widgets.
         */
        fun hasActiveWidgets(context: Context): Boolean {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MemoryLaneWidgetCompact::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            return widgetIds.isNotEmpty()
        }
    }
}
