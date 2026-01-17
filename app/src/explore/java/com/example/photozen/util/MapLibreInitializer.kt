package com.example.photozen.util

import android.content.Context
import android.util.Log
import org.maplibre.android.MapLibre

/**
 * Lazy initializer for MapLibre SDK.
 * 
 * MapLibre initialization is deferred until actually needed (when the map screen is opened)
 * to avoid potential initialization issues during app startup.
 * 
 * Usage:
 * - Call ensureInitialized(context) before using any MapLibre components
 * - The initialization is idempotent (safe to call multiple times)
 */
object MapLibreInitializer {
    
    private const val TAG = "MapLibreInitializer"
    
    @Volatile
    private var isInitialized = false
    
    @Volatile
    private var initializationError: Exception? = null
    
    /**
     * Ensure MapLibre is initialized.
     * This method is idempotent and thread-safe.
     * 
     * @param context Any context (application context will be used)
     * @return true if initialization succeeded, false otherwise
     */
    @Synchronized
    fun ensureInitialized(context: Context): Boolean {
        if (isInitialized) return true
        
        return try {
            CrashLogger.logStartupEvent(context, "MapLibre lazy initialization starting...")
            MapLibre.getInstance(context.applicationContext)
            isInitialized = true
            initializationError = null
            CrashLogger.logStartupEvent(context, "MapLibre lazy initialization SUCCESS")
            Log.i(TAG, "MapLibre initialized successfully")
            true
        } catch (e: Exception) {
            initializationError = e
            CrashLogger.logStartupEvent(context, "MapLibre lazy initialization FAILED: ${e.message}")
            Log.e(TAG, "MapLibre initialization failed", e)
            false
        }
    }
    
    /**
     * Check if MapLibre is currently initialized.
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Get the initialization error if any.
     */
    fun getInitializationError(): Exception? = initializationError
}
