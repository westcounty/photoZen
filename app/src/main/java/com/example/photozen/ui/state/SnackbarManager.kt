package com.example.photozen.ui.state

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局 Snackbar 管理器 (Phase 3-8)
 * 
 * 统一管理应用中的 Snackbar 显示，支持：
 * - 普通消息
 * - 成功消息（绿色）
 * - 错误消息（红色）
 * - 带操作按钮的消息（如撤销）
 * 
 * ## 使用方式
 * 
 * ```kotlin
 * // 在 ViewModel 中注入
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val snackbarManager: SnackbarManager
 * ) : ViewModel() {
 *     fun doSomething() {
 *         snackbarManager.showSuccess("操作成功")
 *     }
 * }
 * 
 * // 在 Composable 中观察
 * val snackbarEvent by snackbarManager.events.collectAsState(initial = null)
 * LaunchedEffect(snackbarEvent) { ... }
 * ```
 * 
 * ## 设计说明
 * 
 * - 使用 SharedFlow 实现事件流，支持多个订阅者
 * - extraBufferCapacity = 1 确保至少缓存一个事件
 * - DROP_OLDEST 策略避免消息堆积
 * - 支持队列显示，新消息等待当前消息消失后显示
 * 
 * @since Phase 3-8
 */
@Singleton
class SnackbarManager @Inject constructor() {
    
    private val _events = MutableSharedFlow<SnackbarEvent>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    /**
     * Snackbar 事件流
     * 
     * 订阅此流以接收 Snackbar 事件并显示
     */
    val events: SharedFlow<SnackbarEvent> = _events.asSharedFlow()
    
    /**
     * 显示普通消息
     * 
     * @param message 消息内容
     * @param duration 显示时长
     */
    fun showMessage(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _events.tryEmit(SnackbarEvent.Message(message, duration))
    }
    
    /**
     * 显示成功消息
     * 
     * @param message 消息内容
     * @param duration 显示时长
     */
    fun showSuccess(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _events.tryEmit(SnackbarEvent.Success(message, duration))
    }
    
    /**
     * 显示错误消息
     * 
     * @param message 消息内容
     * @param duration 显示时长
     */
    fun showError(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        _events.tryEmit(SnackbarEvent.Error(message, duration))
    }
    
    /**
     * 显示警告消息
     * 
     * @param message 消息内容
     * @param duration 显示时长
     */
    fun showWarning(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _events.tryEmit(SnackbarEvent.Warning(message, duration))
    }
    
    /**
     * 显示带操作按钮的消息
     * 
     * @param message 消息内容
     * @param actionLabel 操作按钮文字
     * @param duration 显示时长
     * @param onAction 操作按钮点击回调
     */
    fun showWithAction(
        message: String,
        actionLabel: String,
        duration: SnackbarDuration = SnackbarDuration.Long,
        onAction: () -> Unit
    ) {
        _events.tryEmit(SnackbarEvent.WithAction(message, actionLabel, duration, onAction))
    }
    
    /**
     * 显示带撤销按钮的消息
     * 
     * 便捷方法，用于常见的"操作 + 撤销"场景
     * 
     * @param message 消息内容
     * @param onUndo 撤销操作回调
     */
    fun showWithUndo(
        message: String,
        onUndo: () -> Unit
    ) {
        _events.tryEmit(
            SnackbarEvent.WithAction(
                message = message,
                actionLabel = "撤销",
                duration = SnackbarDuration.Long,
                onAction = onUndo
            )
        )
    }
    
    /**
     * 显示成功消息带撤销
     * 
     * @param message 消息内容
     * @param onUndo 撤销操作回调
     */
    fun showSuccessWithUndo(
        message: String,
        onUndo: () -> Unit
    ) {
        _events.tryEmit(
            SnackbarEvent.SuccessWithAction(
                message = message,
                actionLabel = "撤销",
                duration = SnackbarDuration.Long,
                onAction = onUndo
            )
        )
    }
}

/**
 * Snackbar 事件类型
 */
sealed class SnackbarEvent {
    abstract val message: String
    abstract val duration: SnackbarDuration
    
    /**
     * 普通消息
     */
    data class Message(
        override val message: String,
        override val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarEvent()
    
    /**
     * 成功消息 - 绿色主题
     */
    data class Success(
        override val message: String,
        override val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarEvent()
    
    /**
     * 错误消息 - 红色主题
     */
    data class Error(
        override val message: String,
        override val duration: SnackbarDuration = SnackbarDuration.Long
    ) : SnackbarEvent()
    
    /**
     * 警告消息 - 黄色主题
     */
    data class Warning(
        override val message: String,
        override val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarEvent()
    
    /**
     * 带操作按钮的消息
     */
    data class WithAction(
        override val message: String,
        val actionLabel: String,
        override val duration: SnackbarDuration = SnackbarDuration.Long,
        val onAction: () -> Unit
    ) : SnackbarEvent()
    
    /**
     * 成功消息带操作按钮 - 绿色主题
     */
    data class SuccessWithAction(
        override val message: String,
        val actionLabel: String,
        override val duration: SnackbarDuration = SnackbarDuration.Long,
        val onAction: () -> Unit
    ) : SnackbarEvent()
}

/**
 * Snackbar 类型枚举 - 用于样式区分
 */
enum class SnackbarType {
    DEFAULT,
    SUCCESS,
    ERROR,
    WARNING
}

/**
 * 从 SnackbarEvent 获取类型
 */
fun SnackbarEvent.getType(): SnackbarType = when (this) {
    is SnackbarEvent.Success, is SnackbarEvent.SuccessWithAction -> SnackbarType.SUCCESS
    is SnackbarEvent.Error -> SnackbarType.ERROR
    is SnackbarEvent.Warning -> SnackbarType.WARNING
    else -> SnackbarType.DEFAULT
}
