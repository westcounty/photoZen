package com.example.photozen.di

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.photozen.data.worker.LocationScannerWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for WorkManager related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    
    /**
     * Provides the WorkManager instance.
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    /**
     * Provides a helper class for scheduling location scan work.
     */
    @Provides
    @Singleton
    fun provideLocationScanScheduler(
        workManager: WorkManager
    ): LocationScanScheduler {
        return LocationScanScheduler(workManager)
    }
}

/**
 * Helper class for scheduling LocationScannerWorker.
 */
class LocationScanScheduler(
    private val workManager: WorkManager
) {
    companion object {
        private const val PERIODIC_WORK_NAME = "periodic_location_scan"
    }
    
    /**
     * Schedule a one-time GPS location scan.
     * Used when new photos are synced or user triggers scan.
     */
    fun scheduleOneTimeScan() {
        // No constraints - run immediately when user requests
        val workRequest = OneTimeWorkRequestBuilder<LocationScannerWorker>()
            .build()
        
        workManager.enqueueUniqueWork(
            LocationScannerWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace previous work to start fresh
            workRequest
        )
    }
    
    /**
     * Schedule a periodic GPS location scan.
     * Runs every 6 hours when device is idle and battery is not low.
     */
    fun schedulePeriodicScan() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<LocationScannerWorker>(
            repeatInterval = 6,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Cancel any pending location scans.
     */
    fun cancelScans() {
        workManager.cancelUniqueWork(LocationScannerWorker.WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }
    
    /**
     * Get work info for observing progress.
     */
    fun getWorkInfoFlow() = workManager.getWorkInfosForUniqueWorkFlow(LocationScannerWorker.WORK_NAME)
}
