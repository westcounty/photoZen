package com.example.photozen.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.util.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver to reschedule alarms after device boot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var alarmScheduler: AlarmScheduler
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            
            scope.launch {
                try {
                    // Check if daily reminder is enabled
                    val reminderEnabled = preferencesRepository.getDailyReminderEnabled().first()
                    
                    if (reminderEnabled) {
                        // Get the reminder time
                        val reminderTime = preferencesRepository.getDailyReminderTime().first()
                        
                        // Reschedule the alarm
                        alarmScheduler.scheduleDailyReminder(reminderTime.first, reminderTime.second)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
