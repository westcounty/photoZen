package com.example.photozen.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 动画性能优化工具
 * 
 * 提供高性能动画实现的工具函数和 Modifier 扩展。
 * 
 * ## 核心原则
 * 
 * 1. **使用 graphicsLayer**：变换操作使用 graphicsLayer 而非直接修改布局属性，
 *    避免触发重新布局（Layout phase）
 * 2. **延迟读取状态**：使用 lambda 延迟读取动画值，避免状态变化时重组整个组件
 * 3. **derivedStateOf**：使用 derivedStateOf 隔离状态变化，减少不必要的重组
 * 
 * ## 使用示例
 * 
 * ```kotlin
 * // 使用优化的变换 Modifier
 * Box(
 *     modifier = Modifier.optimizedTransform(
 *         translationX = { offsetX.value },
 *         rotation = { rotation },
 *         alpha = { if (isFading) 0.5f else 1f }
 *     )
 * )
 * 
 * // 使用延迟状态
 * @Composable
 * fun AnimatedCard(offsetX: Animatable<Float, *>) {
 *     val offsetXProvider = rememberLambdaState(offsetX.value)
 *     
 *     Box(
 *         modifier = Modifier.graphicsLayer {
 *             translationX = offsetXProvider()
 *         }
 *     )
 * }
 * ```
 * 
 * @since Phase 4 - 性能优化
 */
object AnimationOptimizations {
    
    /**
     * 空 lambda（用于默认参数）
     */
    private val ZERO_FLOAT: () -> Float = { 0f }
    private val ONE_FLOAT: () -> Float = { 1f }
}

// ============== Modifier 扩展 ==============

/**
 * 优化的变换 Modifier
 * 
 * 使用 graphicsLayer 进行变换，不触发重新布局。
 * 所有参数使用 lambda 延迟读取，避免不必要的重组。
 * 
 * @param translationX X 轴平移（像素）
 * @param translationY Y 轴平移（像素）
 * @param rotation Z 轴旋转（度）
 * @param scaleX X 轴缩放
 * @param scaleY Y 轴缩放
 * @param alpha 透明度
 */
fun Modifier.optimizedTransform(
    translationX: () -> Float = { 0f },
    translationY: () -> Float = { 0f },
    rotation: () -> Float = { 0f },
    scaleX: () -> Float = { 1f },
    scaleY: () -> Float = { 1f },
    alpha: () -> Float = { 1f }
): Modifier = this.graphicsLayer {
    this.translationX = translationX()
    this.translationY = translationY()
    this.rotationZ = rotation()
    this.scaleX = scaleX()
    this.scaleY = scaleY()
    this.alpha = alpha()
}

/**
 * 简化的平移 + 旋转变换
 * 
 * 专为滑动卡片场景优化。
 * 
 * @param offsetX X 轴偏移
 * @param offsetY Y 轴偏移
 * @param rotation 旋转角度
 */
fun Modifier.swipeTransform(
    offsetX: () -> Float,
    offsetY: () -> Float,
    rotation: () -> Float
): Modifier = this.graphicsLayer {
    translationX = offsetX()
    translationY = offsetY()
    rotationZ = rotation()
}

/**
 * 缩放变换
 * 
 * @param scale 统一缩放比例
 */
fun Modifier.scaleTransform(
    scale: () -> Float
): Modifier = this.graphicsLayer {
    val s = scale()
    scaleX = s
    scaleY = s
}

/**
 * 透明度变换
 * 
 * @param alpha 透明度（0.0 - 1.0）
 */
fun Modifier.alphaTransform(
    alpha: () -> Float
): Modifier = this.graphicsLayer {
    this.alpha = alpha()
}

/**
 * 条件透明度
 * 
 * 根据条件设置透明度，使用 graphicsLayer 优化。
 * 
 * @param condition 条件
 * @param activeAlpha 条件为 true 时的透明度
 * @param inactiveAlpha 条件为 false 时的透明度
 */
fun Modifier.conditionalAlpha(
    condition: () -> Boolean,
    activeAlpha: Float = 1f,
    inactiveAlpha: Float = 0.5f
): Modifier = this.graphicsLayer {
    alpha = if (condition()) activeAlpha else inactiveAlpha
}

// ============== Composable 工具函数 ==============

/**
 * 创建延迟读取的状态 lambda
 * 
 * 用于在 graphicsLayer 中延迟读取状态值，避免状态变化时重组整个组件。
 * 
 * @param value 当前值
 * @return 返回值的 lambda
 */
@Composable
fun <T> rememberLambdaState(value: T): () -> T {
    val state = rememberUpdatedState(value)
    return remember { { state.value } }
}

/**
 * 创建延迟读取的 Float 状态
 * 
 * @param value 当前值
 * @return 返回 Float 的 lambda
 */
@Composable
fun rememberFloatLambda(value: Float): () -> Float {
    val state = rememberUpdatedState(value)
    return remember { { state.value } }
}

/**
 * 创建派生状态（避免不必要的重组）
 * 
 * @param calculation 计算函数
 * @return 派生状态
 */
@Composable
fun <T> rememberDerivedState(calculation: () -> T): State<T> {
    return remember { derivedStateOf(calculation) }
}

/**
 * 创建基于条件的派生布尔状态
 * 
 * 常用于选择状态判断，避免全列表重组。
 * 
 * @param key 依赖的 key（通常是 item ID）
 * @param selectedIds 选中 ID 集合
 * @return 是否选中的状态
 */
@Composable
fun rememberIsSelected(key: String, selectedIds: Set<String>): State<Boolean> {
    return remember(key) {
        derivedStateOf { selectedIds.contains(key) }
    }
}

// ============== 性能监控工具 ==============

/**
 * 组件重组计数器（用于调试）
 * 
 * 在 Composable 中使用：
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *     RecompositionCounter("MyComponent")
 *     // ...
 * }
 * ```
 */
@Composable
fun RecompositionCounter(tag: String) {
    val recomposeCount = remember { mutableIntOf(0) }
    recomposeCount.intValue++
    
    // 在调试版本中打印
    if (BuildConfig.DEBUG) {
        android.util.Log.d("Recomposition", "$tag recomposed: ${recomposeCount.intValue}")
    }
}

/**
 * 可变 Int 引用（用于计数器）
 */
private class MutableIntRef(var intValue: Int)

private fun mutableIntOf(value: Int) = MutableIntRef(value)

// ============== 颜色动画优化 ==============

/**
 * 优化的背景颜色变换
 * 
 * 使用 drawBehind 而非 background Modifier，避免重组。
 * 
 * @param color 颜色 lambda
 */
fun Modifier.animatedBackground(
    color: () -> Color
): Modifier = this.drawBehind {
    drawRect(color())
}

/**
 * 条件背景颜色
 * 
 * @param condition 条件
 * @param activeColor 条件为 true 时的颜色
 * @param inactiveColor 条件为 false 时的颜色
 */
fun Modifier.conditionalBackground(
    condition: () -> Boolean,
    activeColor: Color,
    inactiveColor: Color = Color.Transparent
): Modifier = composed {
    this.drawBehind {
        drawRect(if (condition()) activeColor else inactiveColor)
    }
}

// ============== 边界检测优化 ==============

/**
 * 计算滑动方向和强度
 * 
 * 用于滑动卡片的方向判断，避免在 Composable 中重复计算。
 * 
 * @param offsetX X 偏移
 * @param offsetY Y 偏移
 * @param threshold 触发阈值
 * @return SwipeDirection 和强度
 */
fun calculateSwipeState(
    offsetX: Float,
    offsetY: Float,
    threshold: Float
): SwipeState {
    val absX = kotlin.math.abs(offsetX)
    val absY = kotlin.math.abs(offsetY)
    
    return when {
        absX >= threshold && absX > absY -> {
            if (offsetX > 0) SwipeState.Right(intensityValue = absX / threshold)
            else SwipeState.Left(intensityValue = absX / threshold)
        }
        absY >= threshold && absY > absX -> {
            if (offsetY > 0) SwipeState.Down(intensityValue = absY / threshold)
            else SwipeState.Up(intensityValue = absY / threshold)
        }
        else -> SwipeState.None
    }
}

/**
 * 滑动状态
 */
sealed class SwipeState {
    abstract val intensityValue: Float
    
    data object None : SwipeState() {
        override val intensityValue: Float = 0f
    }
    data class Left(override val intensityValue: Float) : SwipeState()
    data class Right(override val intensityValue: Float) : SwipeState()
    data class Up(override val intensityValue: Float) : SwipeState()
    data class Down(override val intensityValue: Float) : SwipeState()
    
    val isActive: Boolean get() = this !is None
}

// ============== BuildConfig 引用 ==============

private object BuildConfig {
    val DEBUG = true // 在实际构建中会被替换
}
