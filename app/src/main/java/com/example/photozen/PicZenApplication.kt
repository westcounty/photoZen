package com.example.photozen

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.photozen.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * PicZen Application class.
 * Entry point for Hilt dependency injection.
 * 
 * Implements Configuration.Provider for WorkManager with HiltWorkerFactory.
 * Uses lazy initialization to ensure Hilt injection is complete before accessing workerFactory.
 */
@HiltAndroidApp
class PicZenApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // CrashLogger already initialized by CrashLoggerInitProvider (ContentProvider)
        // This is the full initialization which clears old logs and starts fresh session
        CrashLogger.init(this)
        CrashLogger.logStartupEvent(this, "Application.onCreate started")
        
        // Log Hilt injection status (workerFactory should be initialized at this point)
        val isWorkerFactoryInitialized = ::workerFactory.isInitialized
        CrashLogger.logStartupEvent(this, "Hilt injection complete, workerFactory initialized: $isWorkerFactoryInitialized")
        
        // Note: MapLibre initialization is now done lazily in MapLibreInitializer
        // to avoid potential initialization issues during app startup
        
        CrashLogger.logStartupEvent(this, "Application.onCreate completed")
        Log.i("PicZenApp", "Application initialization complete")
    }
    
    /**
     * WorkManager configuration using HiltWorkerFactory.
     * Uses lazy initialization to ensure Hilt has completed injection before accessing workerFactory.
     */
    override val workManagerConfiguration: Configuration by lazy {
        CrashLogger.logStartupEvent(this, "WorkManager config requested, workerFactory initialized: ${::workerFactory.isInitialized}")
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    }
}
