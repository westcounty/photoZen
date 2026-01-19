package com.example.photozen.ui.state

import androidx.compose.material3.SnackbarDuration

/**
 * UI 事件
 * 
 * 用于 ViewModel 向 Screen 发送一次性事件。
 * 通过 SharedFlow 实现事件机制，避免配置变更时重复处理。
 * 
 * ## 使用示例
 * ```kotlin
 * // ViewModel
 * private val _uiEvent = MutableSharedFlow<UiEvent>()
 * val uiEvent = _uiEvent.asSharedFlow()
 * 
 * suspend fun doSomething() {
 *     _uiEvent.emit(UiEvent.ShowSnackbar("操作成功", "撤销") { undo() })
 * }
 * 
 * // Screen
 * LaunchedEffect(Unit) {
 *     viewModel.uiEvent.collect { event ->
 *         when (event) {
 *             is UiEvent.ShowSnackbar -> { ... }
 *         }
 *     }
 * }
 * ```
 */
sealed class UiEvent {
    /**
     * 显示 Snackbar 事件
     * 
     * @property message 显示的消息
     * @property actionLabel 操作按钮文字（null 表示无按钮）
     * @property duration Snackbar 显示时长
     * @property onAction 操作按钮点击回调
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val onAction: (suspend () -> Unit)? = null
    ) : UiEvent()
    
    /**
     * 导航事件
     */
    data class Navigate(val route: String) : UiEvent()
    
    /**
     * 返回上一页事件
     */
    data object NavigateBack : UiEvent()
}

/**
 * Snackbar 消息数据类
 * 
 * 用于在 UiState 中保存待显示的 Snackbar 消息
 */
data class SnackbarMessage(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val actionLabel: String? = null,
    val onAction: (suspend () -> Unit)? = null
) {
    companion object {
        /**
         * 创建简单消息（无操作按钮）
         */
        fun simple(message: String) = SnackbarMessage(message = message)
        
        /**
         * 创建可撤销消息
         */
        fun withUndo(message: String, onUndo: suspend () -> Unit) = SnackbarMessage(
            message = message,
            actionLabel = "撤销",
            onAction = onUndo
        )
    }
}
