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
 * Memory Lane Widget - Large (4x4) Version
 *
 * Full-featured widget with large photo, date, poetic time, and action buttons.
 */
class MemoryLaneWidgetLarge : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("MemoryLaneWidgetLarge: onUpdate widgetIds=%s", appWidgetIds.contentToString())
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        Timber.d("MemoryLaneWidgetLarge: onReceive action=%s", action)

        when (action) {
            ACTION_KEEP, ACTION_TRASH, ACTION_REFRESH -> {
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
                    ACTION_KEEP -> handleKeepActionInternal(context, intent)
                    ACTION_TRASH -> handleTrashActionInternal(context, intent)
                    ACTION_REFRESH -> handleRefreshActionInternal(context, intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidgetLarge: error handling action %s", action)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.d("MemoryLaneWidgetLarge: onDeleted widgetIds=%s", appWidgetIds.contentToString())

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
                    Timber.d("MemoryLaneWidgetLarge: cleaned up config for widget %d", widgetId)
                }
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidgetLarge: failed to clean up widget config")
            }
        }

        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        Timber.d("MemoryLaneWidgetLarge: onEnabled - first large widget added")
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
                Timber.e(e, "MemoryLaneWidgetLarge: failed to schedule refresh")
            }
        }
    }

    override fun onDisabled(context: Context) {
        Timber.d("MemoryLaneWidgetLarge: onDisabled - last large widget removed")
        super.onDisabled(context)

        // Cancel periodic refresh when last widget is removed
        scope.launch {
            try {
                val hasStandard = MemoryLaneWidget.hasActiveWidgets(context)
                val hasCompact = MemoryLaneWidgetCompact.hasActiveWidgets(context)

                if (!hasStandard && !hasCompact) {
                    MemoryLaneWidgetWorker.cancel(context)
                    Timber.d("MemoryLaneWidgetLarge: canceled refresh - no more widgets")
                }
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidgetLarge: failed to cancel refresh")
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

                // Get OfflineGeocoder for location
                val offlineGeocoder = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    MemoryLaneWidgetEntryPoint::class.java
                ).offlineGeocoder()

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
                        Timber.d("MemoryLaneWidgetLarge: using cached photo id=%s", cachedPhotoId)
                    }
                }

                // If no valid cached photo or refresh interval passed, get a new random one
                if (photo == null) {
                    photo = photoRepository.getRandomMemoryPhoto(preferThisDayInHistory = thisDayPriority, widgetId = appWidgetId)
                    if (photo != null) {
                        preferencesRepository.setWidgetCurrentPhotoId(appWidgetId, photo.id)
                        preferencesRepository.setWidgetLastRefreshTime(appWidgetId, currentTime)
                        Timber.d("MemoryLaneWidgetLarge: refreshed to new photo (interval passed: %b)", shouldForceRefresh)
                    }
                }

                val views = RemoteViews(context.packageName, R.layout.widget_memory_lane_large)

                if (photo != null) {
                    Timber.d("MemoryLaneWidgetLarge: displaying photo id=%s", photo.id)

                    // Show photo content
                    views.setViewVisibility(R.id.memory_photo_container, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.memory_empty_state, android.view.View.GONE)

                    // Get location details (province and city separately)
                    val locationDetails = when {
                        photo.latitude != null && photo.longitude != null -> {
                            offlineGeocoder.getLocationDetails(photo.latitude, photo.longitude)
                        }
                        !photo.gpsScanned -> {
                            offlineGeocoder.getLocationDetailsFromUri(photo.systemUri)
                        }
                        else -> null
                    }

                    // Format time display with location embedded
                    // Date shows with province (e.g., "2019.07.28 福建省")
                    // Poetic time shows with city (e.g., "去年厦门的夏天")
                    val (dateText, poeticText) = MemoryTimeFormatter.formatFullWithLocation(
                        timestamp = photo.dateTaken,
                        usePoetic = usePoeticTime,
                        province = locationDetails?.province,
                        city = locationDetails?.city
                    )
                    views.setTextViewText(R.id.memory_date_text, dateText)
                    views.setTextViewText(R.id.memory_time_text, poeticText)

                    // Hide the separate location text view (location is now embedded)
                    views.setViewVisibility(R.id.memory_location_text, android.view.View.GONE)

                    // Check if it's "this day in history"
                    val isThisDayInHistory = MemoryTimeFormatter.isThisDayInHistory(photo.dateTaken)
                    if (isThisDayInHistory) {
                        views.setViewVisibility(R.id.memory_this_day_badge, android.view.View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.memory_this_day_badge, android.view.View.GONE)
                    }

                    // Load photo using Coil
                    loadPhotoIntoWidget(context, photo, views, appWidgetId, appWidgetManager)

                    // Set up action button intents
                    setupActionButtons(context, views, appWidgetId, photo.id)

                    // Click photo to open app
                    setupPhotoClickIntent(context, views, appWidgetId, photo.id)

                    // Click time text to open settings
                    setupSettingsClickIntent(context, views, appWidgetId)

                } else {
                    Timber.d("MemoryLaneWidgetLarge: no photos available, showing empty state")
                    showEmptyState(context, views, appWidgetId)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidgetLarge: error updating widget")
                showErrorState(context, appWidgetManager, appWidgetId)
            } finally {
                pendingResult.finish()
            }
        }
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
                .size(600, 600) // Larger size for large widget
                .allowHardware(false) // Required for RemoteViews
                .build()

            val result = imageLoader.execute(request)
            val bitmap = result.image?.toBitmap()

            if (bitmap != null) {
                views.setImageViewBitmap(R.id.memory_photo_image, bitmap)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            Timber.e(e, "MemoryLaneWidgetLarge: failed to load photo")
        }
    }

    private fun setupActionButtons(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        photoId: String
    ) {
        // Keep button
        val keepIntent = Intent(context, MemoryLaneWidgetLarge::class.java).apply {
            action = ACTION_KEEP
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_PHOTO_ID, photoId)
        }
        val keepPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 1,
            keepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.memory_btn_keep, keepPendingIntent)

        // Trash button
        val trashIntent = Intent(context, MemoryLaneWidgetLarge::class.java).apply {
            action = ACTION_TRASH
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_PHOTO_ID, photoId)
        }
        val trashPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 2,
            trashIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.memory_btn_trash, trashPendingIntent)

        // Refresh button
        val refreshIntent = Intent(context, MemoryLaneWidgetLarge::class.java).apply {
            action = ACTION_REFRESH
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 3,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.memory_btn_refresh, refreshPendingIntent)
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

    private fun showEmptyState(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setViewVisibility(R.id.memory_photo_container, android.view.View.GONE)
        views.setViewVisibility(R.id.memory_empty_state, android.view.View.VISIBLE)
        views.setTextViewText(R.id.memory_empty_text, "所有记忆都已重温")
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
            val views = RemoteViews(context.packageName, R.layout.widget_memory_lane_large)
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
            Timber.e(e, "MemoryLaneWidgetLarge: failed to show error state")
        }
    }

    private suspend fun handleKeepActionInternal(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val photoId = intent.getStringExtra(EXTRA_PHOTO_ID) ?: return

        Timber.d("MemoryLaneWidgetLarge: handleKeepAction photoId=%s, widgetId=%d", photoId, widgetId)

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MemoryLaneWidgetEntryPoint::class.java
        )
        val photoRepository = entryPoint.photoRepository()

        photoRepository.keepPhoto(photoId)

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(context, "这份记忆已珍藏 💚", Toast.LENGTH_SHORT).show()
        }

        refreshWidgetAndWait(context, widgetId)
    }

    private suspend fun handleTrashActionInternal(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val photoId = intent.getStringExtra(EXTRA_PHOTO_ID) ?: return

        Timber.d("MemoryLaneWidgetLarge: handleTrashAction photoId=%s, widgetId=%d", photoId, widgetId)

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MemoryLaneWidgetEntryPoint::class.java
        )
        val photoRepository = entryPoint.photoRepository()

        photoRepository.trashPhoto(photoId)

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(context, "告别这份记忆 👋", Toast.LENGTH_SHORT).show()
        }

        refreshWidgetAndWait(context, widgetId)
    }

    private suspend fun handleRefreshActionInternal(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        Timber.d("MemoryLaneWidgetLarge: handleRefreshAction widgetId=%d", widgetId)
        refreshWidgetAndWait(context, widgetId)
    }

    private suspend fun refreshWidgetAndWait(context: Context, widgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            updateAppWidgetSuspend(context, appWidgetManager, widgetId)
        } else {
            val componentName = ComponentName(context, MemoryLaneWidgetLarge::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in widgetIds) {
                updateAppWidgetSuspend(context, appWidgetManager, id)
            }
        }
    }

    private suspend fun updateAppWidgetSuspend(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                MemoryLaneWidgetEntryPoint::class.java
            )
            val photoRepository = entryPoint.photoRepository()
            val preferencesRepository = entryPoint.preferencesRepository()
            val offlineGeocoder = entryPoint.offlineGeocoder()

            val thisDayPriority = preferencesRepository.getWidgetThisDayPriority(appWidgetId)
            val usePoeticTime = preferencesRepository.getWidgetPoeticTime(appWidgetId)

            // Always get a new random photo (this method is for forced refresh)
            val photo = photoRepository.getRandomMemoryPhoto(preferThisDayInHistory = thisDayPriority, widgetId = appWidgetId)

            // Save the new photo ID to cache
            preferencesRepository.setWidgetCurrentPhotoId(appWidgetId, photo?.id)

            val views = RemoteViews(context.packageName, R.layout.widget_memory_lane_large)

            if (photo != null) {
                Timber.d("MemoryLaneWidgetLarge: displaying new photo id=%s", photo.id)

                views.setViewVisibility(R.id.memory_photo_container, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.memory_empty_state, android.view.View.GONE)

                // Get location details (province and city separately)
                val locationDetails = when {
                    photo.latitude != null && photo.longitude != null -> {
                        offlineGeocoder.getLocationDetails(photo.latitude, photo.longitude)
                    }
                    !photo.gpsScanned -> {
                        offlineGeocoder.getLocationDetailsFromUri(photo.systemUri)
                    }
                    else -> null
                }

                // Format time display with location embedded
                val (dateText, poeticText) = MemoryTimeFormatter.formatFullWithLocation(
                    timestamp = photo.dateTaken,
                    usePoetic = usePoeticTime,
                    province = locationDetails?.province,
                    city = locationDetails?.city
                )
                views.setTextViewText(R.id.memory_date_text, dateText)
                views.setTextViewText(R.id.memory_time_text, poeticText)

                // Hide the separate location text view (location is now embedded)
                views.setViewVisibility(R.id.memory_location_text, android.view.View.GONE)

                val isThisDayInHistory = MemoryTimeFormatter.isThisDayInHistory(photo.dateTaken)
                if (isThisDayInHistory) {
                    views.setViewVisibility(R.id.memory_this_day_badge, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.memory_this_day_badge, android.view.View.GONE)
                }

                loadPhotoIntoWidget(context, photo, views, appWidgetId, appWidgetManager)
                setupActionButtons(context, views, appWidgetId, photo.id)
                setupPhotoClickIntent(context, views, appWidgetId, photo.id)
                setupSettingsClickIntent(context, views, appWidgetId)

            } else {
                Timber.d("MemoryLaneWidgetLarge: no photos available")
                showEmptyState(context, views, appWidgetId)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)

        } catch (e: Exception) {
            Timber.e(e, "MemoryLaneWidgetLarge: error updating widget in suspend")
            showErrorState(context, appWidgetManager, appWidgetId)
        }
    }

    private fun triggerWidgetUpdate(context: Context, widgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            updateAppWidget(context, appWidgetManager, widgetId)
        } else {
            val componentName = ComponentName(context, MemoryLaneWidgetLarge::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in widgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        // Actions
        const val ACTION_KEEP = "com.example.photozen.widget.memory.large.ACTION_KEEP"
        const val ACTION_TRASH = "com.example.photozen.widget.memory.large.ACTION_TRASH"
        const val ACTION_REFRESH = "com.example.photozen.widget.memory.large.ACTION_REFRESH"

        // Extras
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_PHOTO_ID = "photo_id"

        /**
         * Trigger update for all Memory Lane Large widgets.
         */
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MemoryLaneWidgetLarge::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (widgetIds.isNotEmpty()) {
                Timber.d("MemoryLaneWidgetLarge: triggerUpdate for %d widgets", widgetIds.size)
                val intent = Intent(context, MemoryLaneWidgetLarge::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        /**
         * Check if there are any active Memory Lane Large widgets.
         */
        fun hasActiveWidgets(context: Context): Boolean {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MemoryLaneWidgetLarge::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            return widgetIds.isNotEmpty()
        }
    }
}
