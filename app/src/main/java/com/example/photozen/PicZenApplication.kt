package com.example.photozen

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.service.DailyProgressService
import com.example.photozen.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
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
        
        // Start foreground progress service if enabled
        initProgressService()
        
        CrashLogger.logStartupEvent(this, "Application.onCreate completed")
        Log.i("PicZenApp", "Application initialization complete")
    }
    
    /**
     * Initialize foreground progress service on app startup.
     * This service displays daily progress in status bar and keeps the app alive.
     */
    private fun initProgressService() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = preferencesRepository.getProgressNotificationEnabled().first()
                if (enabled) {
                    // 使用主线程启动服务，确保稳定性
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        DailyProgressService.start(this@PicZenApplication)
                    }
                }
            } catch (e: Exception) {
                Log.e("PicZenApp", "Failed to start progress service", e)
                CrashLogger.logStartupEvent(this@PicZenApplication, "Progress service start failed: ${e.message}")
            }
        }
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
