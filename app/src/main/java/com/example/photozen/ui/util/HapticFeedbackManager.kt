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
 * 
 * Phase 3-7 增强：
 * - 临界点反馈 (performThresholdReached)
 * - 方向感知的反馈 (performDirectionalFeedback)
 * - 可配置的震动开关
 */
class HapticFeedbackManager(
    private val context: Context,
    private val defaultHaptic: HapticFeedback
) {
    /** 震动反馈是否启用 */
    var isEnabled: Boolean = true
    
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
        if (!isEnabled) return
        
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
     * 临界点反馈 - 当滑动达到触发阈值时调用
     * 
     * Phase 3-7: 提供明确的"已达到阈值"触觉反馈，
     * 让用户知道释放手指将触发操作
     * 
     * @param direction 滑动方向 (用于可能的方向感知反馈)
     */
    fun performThresholdReached(direction: SwipeHapticDirection = SwipeHapticDirection.NEUTRAL) {
        if (!isEnabled) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 使用 EFFECT_CLICK 提供清晰但不过强的反馈
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 短促的确认震动
                val effect = VibrationEffect.createOneShot(15, 120)
                vibrator?.vibrate(effect)
            } else {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {
            try {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }
    }
    
    /**
     * 拖动开始反馈 - 轻微的触觉反馈表示开始交互
     */
    fun performDragStart() {
        if (!isEnabled) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                vibrator?.vibrate(effect)
            } else {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
            try {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }
    
    /**
     * 操作完成反馈 - 滑动操作完成时的确认反馈
     * 
     * @param direction 操作方向
     */
    fun performActionComplete(direction: SwipeHapticDirection) {
        if (!isEnabled) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 根据操作类型选择不同的反馈
                val effectType = when (direction) {
                    SwipeHapticDirection.KEEP -> VibrationEffect.EFFECT_HEAVY_CLICK
                    SwipeHapticDirection.TRASH -> VibrationEffect.EFFECT_DOUBLE_CLICK
                    SwipeHapticDirection.MAYBE -> VibrationEffect.EFFECT_CLICK
                    SwipeHapticDirection.NEUTRAL -> VibrationEffect.EFFECT_CLICK
                }
                val effect = VibrationEffect.createPredefined(effectType)
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val (duration, amplitude) = when (direction) {
                    SwipeHapticDirection.KEEP -> 30L to 150
                    SwipeHapticDirection.TRASH -> 25L to 180
                    SwipeHapticDirection.MAYBE -> 20L to 100
                    SwipeHapticDirection.NEUTRAL -> 20L to 100
                }
                val effect = VibrationEffect.createOneShot(duration, amplitude)
                vibrator?.vibrate(effect)
            } else {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {
            try {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }
    }
    
    /**
     * Perform standard haptic feedback for UI interactions.
     */
    fun performClick() {
        if (!isEnabled) return
        
        try {
            defaultHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }
    
    /**
     * Perform light haptic feedback.
     */
    fun performLight() {
        if (!isEnabled) return
        
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
        if (!isEnabled) return
        
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
        if (!isEnabled) return
        
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
     * 错误/警告反馈 - 用于操作失败或警告
     */
    fun performError() {
        if (!isEnabled) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 双震模式表示警告
                val timings = longArrayOf(0, 30, 50, 30)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 30, 50, 30), -1)
            }
        } catch (_: Exception) {}
    }
    
    /**
     * 选择反馈 - 用于多选模式下的项目选择/取消
     */
    fun performSelection(selected: Boolean) {
        if (!isEnabled) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effectType = if (selected) {
                    VibrationEffect.EFFECT_TICK
                } else {
                    VibrationEffect.EFFECT_TICK
                }
                val effect = VibrationEffect.createPredefined(effectType)
                vibrator?.vibrate(effect)
            } else {
                defaultHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
 * 滑动方向枚举 - 用于触觉反馈
 */
enum class SwipeHapticDirection {
    /** 保留操作 (左滑/右滑) */
    KEEP,
    /** 删除操作 (上滑) */
    TRASH,
    /** 待定操作 (下滑) */
    MAYBE,
    /** 中性/未知方向 */
    NEUTRAL
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

/**
 * Remember a HapticFeedbackManager instance with enabled state from settings.
 * 
 * @param enabled Whether haptic feedback is enabled
 */
@Composable
fun rememberHapticFeedbackManager(enabled: Boolean): HapticFeedbackManager {
    val context = LocalContext.current
    val defaultHaptic = LocalHapticFeedback.current
    
    return remember(context, defaultHaptic, enabled) {
        HapticFeedbackManager(context, defaultHaptic).apply {
            isEnabled = enabled
        }
    }
}
