package com.example.photozen.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.service.DailyProgressService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver to start services after device boot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val pendingResult = goAsync()

            scope.launch {
                try {
                    // Start foreground progress service if enabled
                    val progressEnabled = preferencesRepository.getProgressNotificationEnabled().first()
                    if (progressEnabled) {
                        DailyProgressService.start(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during boot initialization", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
    
    companion object {
        private const val TAG = "BootReceiver"
    }
}
