package com.example.photozen

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.photozen.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import javax.inject.Inject

/**
 * PicZen Application class.
 * Entry point for Hilt dependency injection.
 */
@HiltAndroidApp
class PicZenApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // 1. 最先初始化 CrashLogger，确保能捕获后续的崩溃
        CrashLogger.init(this)
        CrashLogger.logStartupEvent(this, "Application.onCreate started")
        
        // 2. 初始化 MapLibre（包裹在 try-catch 中）
        try {
            CrashLogger.logStartupEvent(this, "MapLibre initializing...")
            MapLibre.getInstance(this)
            CrashLogger.logStartupEvent(this, "MapLibre initialized successfully")
        } catch (e: Exception) {
            Log.e("PicZenApp", "MapLibre init failed", e)
            CrashLogger.logStartupEvent(this, "MapLibre init FAILED: ${e.message}")
        }
        
        CrashLogger.logStartupEvent(this, "Application.onCreate completed")
    }

    /**
     * WorkManager configuration using lazy initialization.
     * This ensures that workerFactory is already injected by Hilt before being accessed.
     * 
     * IMPORTANT: Using 'by lazy' prevents UninitializedPropertyAccessException
     * that could occur if WorkManager tries to access the config before Hilt injection completes.
     */
    override val workManagerConfiguration: Configuration by lazy {
        CrashLogger.logStartupEvent(this, "WorkManager configuration requested")
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    }
}
