package com.example.photozen.widget.memory

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.photozen.data.repository.PreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * 时光拾遗小部件自动刷新 Worker
 *
 * 使用 WorkManager 定期刷新所有 Memory Lane 小部件。
 * 刷新间隔可在配置页面中设置（30分钟 - 每天）。
 */
@HiltWorker
class MemoryLaneWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val preferencesRepository: PreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("MemoryLaneWidgetWorker: doWork started")

        return try {
            // Trigger update for all Memory Lane widget sizes
            MemoryLaneWidget.triggerUpdate(context)
            MemoryLaneWidgetCompact.triggerUpdate(context)
            MemoryLaneWidgetLarge.triggerUpdate(context)

            Timber.d("MemoryLaneWidgetWorker: widgets updated successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "MemoryLaneWidgetWorker: failed to update widgets")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "memory_lane_widget_refresh"

        /**
         * Schedule periodic refresh for Memory Lane widgets.
         *
         * @param context Application context
         * @param intervalMinutes Refresh interval in minutes (min 30)
         */
        fun schedule(context: Context, intervalMinutes: Int) {
            val interval = intervalMinutes.coerceAtLeast(30).toLong()

            Timber.d("MemoryLaneWidgetWorker: scheduling refresh every $interval minutes")

            val request = PeriodicWorkRequestBuilder<MemoryLaneWidgetWorker>(
                interval, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /**
         * Cancel periodic refresh.
         *
         * @param context Application context
         */
        fun cancel(context: Context) {
            Timber.d("MemoryLaneWidgetWorker: canceling scheduled refresh")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Check if refresh is currently scheduled.
         *
         * @param context Application context
         * @return True if refresh work is enqueued
         */
        suspend fun isScheduled(context: Context): Boolean {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            return workInfos.any { !it.state.isFinished }
        }
    }
}
