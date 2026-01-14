package com.example.photozen

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * PicZen Application class.
 * Entry point for Hilt dependency injection.
 * 
 * Implements Configuration.Provider for WorkManager + Hilt integration.
 */
@HiltAndroidApp
class PicZenApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        // Manually initialize WorkManager with Hilt after injection is complete
        try {
            WorkManager.initialize(this, workManagerConfiguration)
            Log.d("PicZenApplication", "WorkManager initialized with HiltWorkerFactory")
        } catch (e: IllegalStateException) {
            // Already initialized - this is fine
            Log.d("PicZenApplication", "WorkManager already initialized")
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
}
