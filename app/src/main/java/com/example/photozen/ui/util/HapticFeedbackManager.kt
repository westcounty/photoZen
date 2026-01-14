package com.example.photozen.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.photozen.ui.screens.flowsorter.ComboLevel

/**
 * Manager for enhanced haptic feedback with variable intensity.
 * 
 * Provides combo-aware haptic feedback where higher combos result in
 * stronger/longer vibrations for a more satisfying experience.
 */
class HapticFeedbackManager(
    private val context: Context,
    private val defaultHaptic: HapticFeedback
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    /**
     * Perform haptic feedback for a swipe action.
     * Intensity scales with combo level.
     * 
     * @param comboCount The current combo count
     * @param level The current combo level
     */
    fun performSwipeFeedback(comboCount: Int, level: ComboLevel) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use VibrationEffect for more control
                val (duration, amplitude) = getVibrationParams(comboCount, level)
                
                val effect = VibrationEffect.createOneShot(duration, amplitude)
                vibrator?.vibrate(effect)
            } else {
                // Fallback for older devices
                defaultHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (e: Exception) {
            // Fallback if vibration fails (e.g., missing permission)
            try {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {
                // Ignore if all haptic feedback fails
            }
        }
    }
    
    /**
     * Perform standard haptic feedback for UI interactions.
     */
    fun performClick() {
        try {
            defaultHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }
    
    /**
     * Perform light haptic feedback.
     */
    fun performLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                vibrator?.vibrate(effect)
            } else {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {}
    }
    
    /**
     * Perform heavy haptic feedback for important actions.
     */
    fun performHeavy() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                vibrator?.vibrate(effect)
            } else {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {}
    }
    
    /**
     * Perform victory celebration haptic pattern.
     */
    fun performVictory() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create a celebratory pattern
                val timings = longArrayOf(0, 50, 100, 50, 100, 100)
                val amplitudes = intArrayOf(0, 100, 0, 150, 0, 255)
                
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                // Fallback pattern
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 50, 100, 50, 100, 100), -1)
            }
        } catch (_: Exception) {}
    }
    
    /**
     * Calculate vibration parameters based on combo level.
     * 
     * @return Pair of (duration in ms, amplitude 1-255)
     */
    private fun getVibrationParams(comboCount: Int, level: ComboLevel): Pair<Long, Int> {
        return when (level) {
            ComboLevel.NONE -> 20L to 50
            ComboLevel.NORMAL -> 25L to 80
            ComboLevel.WARM -> 30L to 120
            ComboLevel.HOT -> 40L to 180
            ComboLevel.FIRE -> 50L to 255
        }
    }
    
    companion object {
        /**
         * Check if device supports haptic feedback.
         */
        fun isHapticSupported(context: Context): Boolean {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            
            return vibrator?.hasVibrator() == true
        }
    }
}

/**
 * Remember a HapticFeedbackManager instance.
 */
@Composable
fun rememberHapticFeedbackManager(): HapticFeedbackManager {
    val context = LocalContext.current
    val defaultHaptic = LocalHapticFeedback.current
    
    return remember(context, defaultHaptic) {
        HapticFeedbackManager(context, defaultHaptic)
    }
}
