package com.example.photozen.ui.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 通用异步加载状态
 * 
 * 用于替代多个 isLoading/error 字段组合，提供更清晰的状态表达。
 * 
 * ## 使用示例
 * 
 * ```kotlin
 * // 在 ViewModel 中
 * private val _syncState = MutableStateFlow<AsyncState<Unit>>(AsyncState.Idle)
 * val syncState: StateFlow<AsyncState<Unit>> = _syncState.asStateFlow()
 * 
 * fun sync() {
 *     viewModelScope.launch {
 *         _syncState.value = AsyncState.Loading
 *         try {
 *             val result = repository.sync()
 *             _syncState.value = AsyncState.Success(result)
 *         } catch (e: Exception) {
 *             _syncState.value = AsyncState.Error(e.message ?: "同步失败")
 *         }
 *     }
 * }
 * 
 * // 在 Composable 中
 * syncState.Render(
 *     onLoading = { LoadingIndicator() },
 *     onError = { msg -> ErrorView(msg) },
 *     onSuccess = { data -> SuccessView(data) }
 * )
 * ```
 * 
 * @since Phase 4 - 状态管理重构
 */
sealed interface AsyncState<out T> {
    
    /**
     * 初始状态，尚未开始加载
     */
    data object Idle : AsyncState<Nothing>
    
    /**
     * 加载中
     */
    data object Loading : AsyncState<Nothing>
    
    /**
     * 加载成功
     * 
     * @property data 加载的数据
     */
    data class Success<T>(val data: T) : AsyncState<T>
    
    /**
     * 加载失败
     * 
     * @property message 错误信息
     * @property cause 原始异常（可选）
     */
    data class Error(val message: String, val cause: Throwable? = null) : AsyncState<Nothing>
    
    /**
     * 是否正在加载
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * 是否加载成功
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * 是否加载失败
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * 是否处于初始状态
     */
    val isIdle: Boolean
        get() = this is Idle
    
    /**
     * 获取数据（如果成功）
     * 
     * @return 成功时返回数据，否则返回 null
     */
    fun getOrNull(): T? = (this as? Success)?.data
    
    /**
     * 获取数据或抛出异常
     * 
     * @throws IllegalStateException 如果状态不是 Success
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw IllegalStateException(message, cause)
        else -> throw IllegalStateException("State is not Success: $this")
    }
    
    /**
     * 获取数据或返回默认值
     * 
     * @param default 默认值
     * @return 成功时返回数据，否则返回默认值
     */
    fun getOrDefault(default: @UnsafeVariance T): T = (this as? Success)?.data ?: default
    
    /**
     * 获取错误信息（如果失败）
     * 
     * @return 失败时返回错误信息，否则返回 null
     */
    fun errorOrNull(): String? = (this as? Error)?.message
    
    /**
     * 获取原始异常（如果有）
     */
    fun causeOrNull(): Throwable? = (this as? Error)?.cause
}

// ============== 扩展函数 ==============

/**
 * 转换成功数据
 * 
 * @param transform 转换函数
 * @return 转换后的 AsyncState
 */
inline fun <T, R> AsyncState<T>.map(transform: (T) -> R): AsyncState<R> {
    return when (this) {
        is AsyncState.Idle -> AsyncState.Idle
        is AsyncState.Loading -> AsyncState.Loading
        is AsyncState.Success -> AsyncState.Success(transform(data))
        is AsyncState.Error -> this
    }
}

/**
 * 平铺映射
 * 
 * @param transform 转换函数，返回新的 AsyncState
 * @return 转换后的 AsyncState
 */
inline fun <T, R> AsyncState<T>.flatMap(transform: (T) -> AsyncState<R>): AsyncState<R> {
    return when (this) {
        is AsyncState.Idle -> AsyncState.Idle
        is AsyncState.Loading -> AsyncState.Loading
        is AsyncState.Success -> transform(data)
        is AsyncState.Error -> this
    }
}

/**
 * 模式匹配处理
 * 
 * @param onIdle 初始状态处理
 * @param onLoading 加载中处理
 * @param onSuccess 成功处理
 * @param onError 错误处理
 * @return 处理结果
 */
inline fun <T, R> AsyncState<T>.fold(
    onIdle: () -> R,
    onLoading: () -> R,
    onSuccess: (T) -> R,
    onError: (String, Throwable?) -> R
): R = when (this) {
    is AsyncState.Idle -> onIdle()
    is AsyncState.Loading -> onLoading()
    is AsyncState.Success -> onSuccess(data)
    is AsyncState.Error -> onError(message, cause)
}

/**
 * 仅处理成功状态
 * 
 * @param action 成功时执行的操作
 */
inline fun <T> AsyncState<T>.onSuccess(action: (T) -> Unit): AsyncState<T> {
    if (this is AsyncState.Success) {
        action(data)
    }
    return this
}

/**
 * 仅处理错误状态
 * 
 * @param action 错误时执行的操作
 */
inline fun <T> AsyncState<T>.onError(action: (String, Throwable?) -> Unit): AsyncState<T> {
    if (this is AsyncState.Error) {
        action(message, cause)
    }
    return this
}

/**
 * 仅处理加载状态
 * 
 * @param action 加载中时执行的操作
 */
inline fun <T> AsyncState<T>.onLoading(action: () -> Unit): AsyncState<T> {
    if (this is AsyncState.Loading) {
        action()
    }
    return this
}

// ============== Composable 扩展 ==============

/**
 * 在 Composable 中便捷渲染 AsyncState
 * 
 * @param modifier Modifier
 * @param onIdle 初始状态 UI（默认不显示）
 * @param onLoading 加载中 UI（默认显示居中 CircularProgressIndicator）
 * @param onError 错误 UI（默认显示错误文本）
 * @param onSuccess 成功 UI
 */
@Composable
fun <T> AsyncState<T>.Render(
    modifier: Modifier = Modifier,
    onIdle: @Composable () -> Unit = { /* 不显示 */ },
    onLoading: @Composable () -> Unit = {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    },
    onError: @Composable (String) -> Unit = { msg ->
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "错误: $msg")
        }
    },
    onSuccess: @Composable (T) -> Unit
) {
    when (this) {
        is AsyncState.Idle -> onIdle()
        is AsyncState.Loading -> onLoading()
        is AsyncState.Success -> onSuccess(data)
        is AsyncState.Error -> onError(message)
    }
}

/**
 * 简化版渲染：仅关注成功和加载状态
 * 
 * @param onLoading 加载中 UI
 * @param onSuccess 成功 UI
 */
@Composable
fun <T> AsyncState<T>.RenderSimple(
    onLoading: @Composable () -> Unit = { CircularProgressIndicator() },
    onSuccess: @Composable (T) -> Unit
) {
    when (this) {
        is AsyncState.Idle -> { /* 不显示 */ }
        is AsyncState.Loading -> onLoading()
        is AsyncState.Success -> onSuccess(data)
        is AsyncState.Error -> { /* 不显示错误 */ }
    }
}

// ============== 工厂方法 ==============

/**
 * 从 Result 创建 AsyncState
 */
fun <T> Result<T>.toAsyncState(): AsyncState<T> = fold(
    onSuccess = { AsyncState.Success(it) },
    onFailure = { AsyncState.Error(it.message ?: "未知错误", it) }
)

/**
 * 从可空值创建 AsyncState
 * 
 * @param errorMessage 值为 null 时的错误信息
 */
fun <T> T?.toAsyncState(errorMessage: String = "数据为空"): AsyncState<T> =
    this?.let { AsyncState.Success(it) } ?: AsyncState.Error(errorMessage)
