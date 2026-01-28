package com.example.photozen.widget.memory

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import android.widget.Toast
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.example.photozen.MainActivity
import com.example.photozen.R
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.util.OfflineGeocoder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Entry point for accessing dependencies in MemoryLaneWidget.
 * Using EntryPoint instead of @AndroidEntryPoint for reliable injection in widgets.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MemoryLaneWidgetEntryPoint {
    fun photoRepository(): PhotoRepository
    fun preferencesRepository(): PreferencesRepository
    fun offlineGeocoder(): OfflineGeocoder
}

/**
 * Memory Lane Widget - "时光拾遗" 小部件
 *
 * 在桌面展示手机相册中被遗忘的照片，让尘封的记忆自然涌现。
 *
 * 功能:
 * - 随机展示"尘封"的照片（更久远的照片权重更高）
 * - 诗意的时间描述（如"三年前的夏天"）
 * - 快捷分类操作：保留(KEEP)/删除(TRASH)/刷新
 */
class MemoryLaneWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("MemoryLaneWidget: onUpdate widgetIds=%s", appWidgetIds.contentToString())
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        Timber.d("MemoryLaneWidget: onReceive action=%s", action)

        when (action) {
            ACTION_KEEP, ACTION_TRASH, ACTION_REFRESH -> {
                // Use goAsync to prevent the broadcast receiver from being killed
                // before the coroutine completes
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
                Timber.e(e, "MemoryLaneWidget: error handling action %s", action)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.d("MemoryLaneWidget: onDeleted widgetIds=%s", appWidgetIds.contentToString())

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
                    Timber.d("MemoryLaneWidget: cleaned up config for widget %d", widgetId)
                }
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidget: failed to clean up widget config")
            }
        }

        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        Timber.d("MemoryLaneWidget: onEnabled - first widget added")
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
                Timber.e(e, "MemoryLaneWidget: failed to schedule refresh")
            }
        }
    }

    override fun onDisabled(context: Context) {
        Timber.d("MemoryLaneWidget: onDisabled - last widget removed")
        super.onDisabled(context)

        // Cancel periodic refresh when last widget is removed
        // Only cancel if no other Memory Lane widgets exist
        scope.launch {
            try {
                val hasCompact = MemoryLaneWidgetCompact.hasActiveWidgets(context)
                val hasLarge = MemoryLaneWidgetLarge.hasActiveWidgets(context)

                if (!hasCompact && !hasLarge) {
                    MemoryLaneWidgetWorker.cancel(context)
                    Timber.d("MemoryLaneWidget: canceled refresh - no more widgets")
                }
            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidget: failed to cancel refresh")
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
                val offlineGeocoder = entryPoint.offlineGeocoder()

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
                    // Try to load cached photo
                    val cachedPhoto = photoRepository.getPhotoById(cachedPhotoId)
                    // Use cached photo if it exists and is still UNSORTED
                    if (cachedPhoto != null && cachedPhoto.status == com.example.photozen.data.model.PhotoStatus.UNSORTED) {
                        photo = cachedPhoto
                        Timber.d("MemoryLaneWidget: using cached photo id=%s", cachedPhotoId)
                    }
                }

                // If no valid cached photo or refresh interval passed, get a new random one
                if (photo == null) {
                    photo = photoRepository.getRandomMemoryPhoto(preferThisDayInHistory = thisDayPriority, widgetId = appWidgetId)
                    // Save the new photo ID and refresh time to cache
                    if (photo != null) {
                        preferencesRepository.setWidgetCurrentPhotoId(appWidgetId, photo.id)
                        preferencesRepository.setWidgetLastRefreshTime(appWidgetId, currentTime)
                        Timber.d("MemoryLaneWidget: refreshed to new photo id=%s (interval passed: %b)", photo.id, shouldForceRefresh)
                    }
                }

                val views = RemoteViews(context.packageName, R.layout.widget_memory_lane_standard)

                if (photo != null) {
                    Timber.d("MemoryLaneWidget: displaying photo id=%s", photo.id)

                    // Show photo content
                    views.setViewVisibility(R.id.memory_photo_container, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.memory_empty_state, android.view.View.GONE)

                    // Format time display
                    val timeText = MemoryTimeFormatter.format(photo.dateTaken, usePoeticTime)
                    views.setTextViewText(R.id.memory_time_text, timeText)

                    // Display location if available
                    val locationText = when {
                        photo.latitude != null && photo.longitude != null -> {
                            offlineGeocoder.getLocationText(photo.latitude, photo.longitude)
                        }
                        !photo.gpsScanned -> {
                            offlineGeocoder.getLocationTextFromUri(photo.systemUri)
                        }
                        else -> null
                    }
                    if (locationText != null) {
                        views.setTextViewText(R.id.memory_location_text, locationText)
                        views.setViewVisibility(R.id.memory_location_text, android.view.View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.memory_location_text, android.view.View.GONE)
                    }

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
                    Timber.d("MemoryLaneWidget: no photos available, showing empty state")
                    showEmptyState(context, views, appWidgetId)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                Timber.e(e, "MemoryLaneWidget: error updating widget")
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
                .size(400, 300) // Limit size for widget performance
                .allowHardware(false) // Required for RemoteViews
                .build()

            val result = imageLoader.execute(request)
            val bitmap = result.image?.toBitmap()

            if (bitmap != null) {
                views.setImageViewBitmap(R.id.memory_photo_image, bitmap)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            Timber.e(e, "MemoryLaneWidget: failed to load photo")
        }
    }

    private fun setupActionButtons(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        photoId: String
    ) {
        // Keep button
        val keepIntent = Intent(context, MemoryLaneWidget::class.java).apply {
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
        val trashIntent = Intent(context, MemoryLaneWidget::class.java).apply {
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
        val refreshIntent = Intent(context, MemoryLaneWidget::class.java).apply {
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
            putExtra(EXTRA_OPEN_PHOTO_ID, photoId)
            putExtra(EXTRA_FROM_MEMORY_WIDGET, true)
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
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
     * Allows users to edit widget settings after initial setup.
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
            appWidgetId * 100 + 4, // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.memory_time_text, pendingIntent)
    }

    private fun showEmptyState(context: Context, views: RemoteViews, appWidgetId: Int) {
        views.setViewVisibility(R.id.memory_photo_container, android.view.View.GONE)
        views.setViewVisibility(R.id.memory_empty_state, android.view.View.VISIBLE)
        views.setTextViewText(R.id.memory_empty_text, "所有记忆都已重温")
        views.setTextViewText(R.id.memory_empty_emoji, "🎉")

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
            val views = RemoteViews(context.packageName, R.layout.widget_memory_lane_standard)
            views.setViewVisibility(R.id.memory_photo_container, android.view.View.GONE)
            views.setViewVisibility(R.id.memory_empty_state, android.view.View.VISIBLE)
            views.setTextViewText(R.id.memory_empty_text, "点击打开")
            views.setTextViewText(R.id.memory_empty_emoji, "⚠️")

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
            Timber.e(e, "MemoryLaneWidget: failed to show error state")
        }
    }

    private suspend fun handleKeepActionInternal(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val photoId = intent.getStringExtra(EXTRA_PHOTO_ID) ?: return

        Timber.d("MemoryLaneWidget: handleKeepAction photoId=%s, widgetId=%d", photoId, widgetId)

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MemoryLaneWidgetEntryPoint::class.java
        )
        val photoRepository = entryPoint.photoRepository()

        // Update photo status to KEEP (also increments daily stats internally)
        photoRepository.keepPhoto(photoId)

        // Show feedback toast
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(context, "这份记忆已珍藏 💚", Toast.LENGTH_SHORT).show()
        }

        // Refresh widget with new photo - wait for completion
        refreshWidgetAndWait(context, widgetId)
    }

    private suspend fun handleTrashActionInternal(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val photoId = intent.getStringExtra(EXTRA_PHOTO_ID) ?: return

        Timber.d("MemoryLaneWidget: handleTrashAction photoId=%s, widgetId=%d", photoId, widgetId)

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MemoryLaneWidgetEntryPoint::class.java
        )
        val photoRepository = entryPoint.photoRepository()

        // Update photo status to TRASH (also increments daily stats internally)
        photoRepository.trashPhoto(photoId)

        // Show feedback toast
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(context, "告别这份记忆 👋", Toast.LENGTH_SHORT).show()
        }

        // Refresh widget with new photo - wait for completion
        refreshWidgetAndWait(context, widgetId)
    }

    private suspend fun handleRefreshActionInternal(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        Timber.d("MemoryLaneWidget: handleRefreshAction widgetId=%d", widgetId)

        refreshWidgetAndWait(context, widgetId)
    }

    /**
     * Refresh widget and wait for completion.
     * This is a suspend function that directly updates the widget without launching another coroutine.
     */
    private suspend fun refreshWidgetAndWait(context: Context, widgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Update specific widget
            updateAppWidgetSuspend(context, appWidgetManager, widgetId)
        } else {
            // Update all widgets
            val componentName = ComponentName(context, MemoryLaneWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in widgetIds) {
                updateAppWidgetSuspend(context, appWidgetManager, id)
            }
        }
    }

    /**
     * Suspend version of updateAppWidget that doesn't use goAsync (already handled by caller).
     */
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

            // Read per-widget settings (falls back to global if not set)
            val thisDayPriority = preferencesRepository.getWidgetThisDayPriority(appWidgetId)
            val usePoeticTime = preferencesRepository.getWidgetPoeticTime(appWidgetId)

            // Always get a new random photo (this method is for forced refresh)
            val photo = photoRepository.getRandomMemoryPhoto(preferThisDayInHistory = thisDayPriority, widgetId = appWidgetId)

            // Save the new photo ID to cache
            preferencesRepository.setWidgetCurrentPhotoId(appWidgetId, photo?.id)

            val views = RemoteViews(context.packageName, R.layout.widget_memory_lane_standard)

            if (photo != null) {
                Timber.d("MemoryLaneWidget: displaying new photo id=%s", photo.id)

                // Show photo content
                views.setViewVisibility(R.id.memory_photo_container, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.memory_empty_state, android.view.View.GONE)

                // Format time display
                val timeText = MemoryTimeFormatter.format(photo.dateTaken, usePoeticTime)
                views.setTextViewText(R.id.memory_time_text, timeText)

                // Display location if available
                val locationText = when {
                    photo.latitude != null && photo.longitude != null -> {
                        offlineGeocoder.getLocationText(photo.latitude, photo.longitude)
                    }
                    !photo.gpsScanned -> {
                        offlineGeocoder.getLocationTextFromUri(photo.systemUri)
                    }
                    else -> null
                }
                if (locationText != null) {
                    views.setTextViewText(R.id.memory_location_text, locationText)
                    views.setViewVisibility(R.id.memory_location_text, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.memory_location_text, android.view.View.GONE)
                }

                // Check if it's "this day in history"
                val isThisDayInHistory = MemoryTimeFormatter.isThisDayInHistory(photo.dateTaken)
                if (isThisDayInHistory) {
                    views.setViewVisibility(R.id.memory_this_day_badge, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.memory_this_day_badge, android.view.View.GONE)
                }

                // Load photo using Coil
                loadPhotoIntoWidget(context, photo, views, appWidgetId, appWidgetManager)

                // Set up action button intents with NEW photo ID
                setupActionButtons(context, views, appWidgetId, photo.id)

                // Click photo to open app
                setupPhotoClickIntent(context, views, appWidgetId, photo.id)

                // Click time text to open settings
                setupSettingsClickIntent(context, views, appWidgetId)

            } else {
                Timber.d("MemoryLaneWidget: no photos available, showing empty state")
                showEmptyState(context, views, appWidgetId)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)

        } catch (e: Exception) {
            Timber.e(e, "MemoryLaneWidget: error updating widget in suspend")
            showErrorState(context, appWidgetManager, appWidgetId)
        }
    }

    private fun triggerWidgetUpdate(context: Context, widgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // Update specific widget
            updateAppWidget(context, appWidgetManager, widgetId)
        } else {
            // Update all widgets
            val componentName = ComponentName(context, MemoryLaneWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in widgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        private const val TAG = "MemoryLaneWidget"

        // Actions
        const val ACTION_KEEP = "com.example.photozen.widget.memory.ACTION_KEEP"
        const val ACTION_TRASH = "com.example.photozen.widget.memory.ACTION_TRASH"
        const val ACTION_REFRESH = "com.example.photozen.widget.memory.ACTION_REFRESH"

        // Extras
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_PHOTO_ID = "photo_id"
        const val EXTRA_OPEN_PHOTO_ID = "open_photo_id"
        const val EXTRA_FROM_MEMORY_WIDGET = "from_memory_widget"

        /**
         * Trigger update for all Memory Lane widgets.
         */
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MemoryLaneWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (widgetIds.isNotEmpty()) {
                Timber.d("MemoryLaneWidget: triggerUpdate for %d widgets", widgetIds.size)
                val intent = Intent(context, MemoryLaneWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }
        }

        /**
         * Check if there are any active Memory Lane (standard) widgets.
         */
        fun hasActiveWidgets(context: Context): Boolean {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MemoryLaneWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            return widgetIds.isNotEmpty()
        }
    }
}
