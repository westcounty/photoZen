package com.example.photozen

import android.app.Application
import com.example.photozen.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * PicZen Application class.
 * Entry point for Hilt dependency injection.
 */
@HiltAndroidApp
class PicZenApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize crash logger for debugging
        CrashLogger.init(this)
    }
}
