package com.example.photozen.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.photozen.ui.theme.PicZenMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 列表入场动画组件集 (DES-027)
 *
 * 提供列表项渐进入场动画，增强视觉体验。
 *
 * ## 使用方式
 *
 * ```kotlin
 * AnimatedLazyColumn(items = photos) { index, photo ->
 *     PhotoListItem(photo = photo)
 * }
 * ```
 *
 * ## 动画规范
 *
 * - 错开延迟: 30ms per item
 * - 动画时长: 200ms (PicZenMotion.Duration.Normal)
 * - 缓动曲线: EmphasizedDecelerate
 * - 效果: 淡入 + 向上位移
 */

/**
 * 带入场动画的 LazyColumn
 *
 * @param items 列表数据
 * @param modifier Modifier
 * @param state LazyListState
 * @param content 列表项内容
 */
@Composable
fun <T> AnimatedLazyColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    key: ((item: T) -> Any)? = null,
    content: @Composable (index: Int, item: T) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = state
    ) {
        itemsIndexed(
            items = items,
            key = if (key != null) { index, item -> key(item) } else null
        ) { index, item ->
            AnimatedListItem(
                index = index,
                maxDelay = items.size.coerceAtMost(15) // 最多15个item的延迟
            ) {
                content(index, item)
            }
        }
    }
}

/**
 * 带入场动画的 LazyRow
 *
 * @param items 列表数据
 * @param modifier Modifier
 * @param state LazyListState
 * @param content 列表项内容
 */
@Composable
fun <T> AnimatedLazyRow(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    key: ((item: T) -> Any)? = null,
    content: @Composable (index: Int, item: T) -> Unit
) {
    LazyRow(
        modifier = modifier,
        state = state
    ) {
        itemsIndexed(
            items = items,
            key = if (key != null) { index, item -> key(item) } else null
        ) { index, item ->
            AnimatedListItem(
                index = index,
                maxDelay = items.size.coerceAtMost(10), // 横向列表最多10个item延迟
                translateX = true // 横向位移而非纵向
            ) {
                content(index, item)
            }
        }
    }
}

/**
 * 单个列表项的入场动画包装器
 *
 * @param index 列表索引，用于计算延迟
 * @param maxDelay 最大延迟项数
 * @param translateX 是否使用水平位移（用于横向列表）
 * @param content 内容
 */
@Composable
fun AnimatedListItem(
    index: Int,
    maxDelay: Int = 15,
    translateX: Boolean = false,
    content: @Composable () -> Unit
) {
    val animatedAlpha = remember { Animatable(0f) }
    val animatedOffset = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // 错开入场，但限制最大延迟
        val actualIndex = index.coerceAtMost(maxDelay)
        delay(actualIndex * PicZenMotion.Delay.StaggerItem)

        // 并行执行淡入和位移动画
        launch {
            animatedAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = PicZenMotion.Duration.Normal,
                    easing = PicZenMotion.Easing.EmphasizedDecelerate
                )
            )
        }
        animatedOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = PicZenMotion.Duration.Normal,
                easing = PicZenMotion.Easing.EmphasizedDecelerate
            )
        )
    }

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = animatedAlpha.value
            if (translateX) {
                translationX = animatedOffset.value
            } else {
                translationY = animatedOffset.value
            }
        }
    ) {
        content()
    }
}

/**
 * 扩展函数：为 LazyListScope 添加带动画的 items
 *
 * 使用方式：
 * ```kotlin
 * LazyColumn {
 *     animatedItems(photos) { photo ->
 *         PhotoListItem(photo)
 *     }
 * }
 * ```
 */
inline fun <T> LazyListScope.animatedItems(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    crossinline itemContent: @Composable (item: T) -> Unit
) {
    itemsIndexed(
        items = items,
        key = if (key != null) { _, item -> key(item) } else null
    ) { index, item ->
        AnimatedListItem(index = index) {
            itemContent(item)
        }
    }
}

/**
 * 扩展函数：为 LazyListScope 添加带动画和索引的 items
 */
inline fun <T> LazyListScope.animatedItemsIndexed(
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline itemContent: @Composable (index: Int, item: T) -> Unit
) {
    itemsIndexed(
        items = items,
        key = key
    ) { index, item ->
        AnimatedListItem(index = index) {
            itemContent(index, item)
        }
    }
}
