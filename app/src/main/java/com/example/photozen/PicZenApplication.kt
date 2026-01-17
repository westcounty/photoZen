package com.example.photozen

import android.app.Application
import android.util.Log
import com.example.photozen.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * PicZen Application class.
 * Entry point for Hilt dependency injection.
 * 
 * NOTE: WorkManager Configuration.Provider temporarily removed to diagnose startup crash.
 * MapLibre initialization also temporarily disabled.
 * These will be restored once startup stability is confirmed.
 */
@HiltAndroidApp
class PicZenApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // CrashLogger already initialized by CrashLoggerInitProvider (ContentProvider)
        // This is the full initialization which clears old logs and starts fresh session
        CrashLogger.init(this)
        CrashLogger.logStartupEvent(this, "Application.onCreate started")
        
        // Log Hilt injection status
        CrashLogger.logStartupEvent(this, "Hilt injection should be complete at this point")
        
        // MapLibre temporarily disabled for debugging
        // TODO: Re-enable after startup crash is resolved
        // try {
        //     CrashLogger.logStartupEvent(this, "MapLibre initializing...")
        //     MapLibre.getInstance(this)
        //     CrashLogger.logStartupEvent(this, "MapLibre initialized successfully")
        // } catch (e: Exception) {
        //     Log.e("PicZenApp", "MapLibre init failed", e)
        //     CrashLogger.logStartupEvent(this, "MapLibre init FAILED: ${e.message}")
        // }
        
        CrashLogger.logStartupEvent(this, "Application.onCreate completed")
        Log.i("PicZenApp", "Application initialization complete")
    }
    
    // WorkManager Configuration.Provider temporarily removed
    // The WorkManager will use default initialization via AndroidManifest
    // which should work without the custom HiltWorkerFactory for now
}
