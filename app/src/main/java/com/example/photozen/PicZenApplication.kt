package com.example.photozen

import android.app.Application
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
        
        // Initialize MapLibre
        MapLibre.getInstance(this)
        
        // Initialize crash logger for debugging
        CrashLogger.init(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
