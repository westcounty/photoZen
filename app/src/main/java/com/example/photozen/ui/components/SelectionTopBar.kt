package com.example.photozen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.PicZenMotion

/**
 * 选择模式顶栏
 * 
 * 在选择模式下替代普通顶栏，显示选择数量和全选按钮。
 * 
 * ## 布局
 * ```
 * [X 关闭]  已选择 X 张  [全选/取消全选]
 * ```
 * 
 * ## 设计规范
 * - 背景：primaryContainer 30% 透明度
 * - 选择数量：titleMedium 字体
 * - 全选按钮：图标 + 文字
 * 
 * @param selectedCount 已选择数量
 * @param totalCount 总数量（用于判断是否已全选）
 * @param onClose 关闭选择模式回调
 * @param onSelectAll 全选回调
 * @param onDeselectAll 取消全选回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllSelected = selectedCount == totalCount && totalCount > 0

    // 关闭按钮交互状态
    val closeInteractionSource = remember { MutableInteractionSource() }
    val isClosePressed by closeInteractionSource.collectIsPressedAsState()

    // 关闭按钮旋转动画 (按压时旋转90°)
    val closeRotation by animateFloatAsState(
        targetValue = if (isClosePressed) 90f else 0f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "closeRotation"
    )

    // 关闭按钮缩放动画 (按压时缩小到0.85f)
    val closeScale by animateFloatAsState(
        targetValue = if (isClosePressed) 0.85f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "closeScale"
    )

    // 选中数量脉冲动画
    var previousCount by remember { mutableIntStateOf(selectedCount) }
    var triggerPulse by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedCount) {
        if (selectedCount != previousCount) {
            triggerPulse++
            previousCount = selectedCount
        }
    }

    val countScale by animateFloatAsState(
        targetValue = if (triggerPulse > 0) 1f else 1f,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "countScale"
    )

    // 实际的脉冲效果 - 数量变化时放大再恢复
    var isPulsing by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedCount) {
        if (previousCount != selectedCount || isPulsing == 0) {
            isPulsing++
        }
    }

    val actualCountScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "actualCountScale"
    )

    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = onClose,
                interactionSource = closeInteractionSource
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "退出选择模式",
                    modifier = Modifier.graphicsLayer {
                        rotationZ = closeRotation
                        scaleX = closeScale
                        scaleY = closeScale
                    }
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已选择 ",
                    style = MaterialTheme.typography.titleMedium
                )
                // 带脉冲动画的数量文字
                AnimatedCountText(
                    count = selectedCount
                )
                Text(
                    text = " 张",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        actions = {
            SelectAllButton(
                isAllSelected = isAllSelected,
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    )
}

/**
 * 带脉冲动画的数量文字
 * 数量变化时会有缩放脉冲效果
 */
@Composable
private fun AnimatedCountText(
    count: Int
) {
    var previousCount by remember { mutableIntStateOf(count) }
    var triggerBounce by remember { mutableIntStateOf(0) }

    LaunchedEffect(count) {
        if (count != previousCount) {
            triggerBounce++
            previousCount = count
        }
    }

    // 每次 triggerBounce 变化时从 1.15f 开始动画回到 1f
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "countPulseScale"
    )

    // 使用 key 来触发重组实现脉冲
    val pulseScale by animateFloatAsState(
        targetValue = if (triggerBounce % 2 == 1) 1.15f else 1f,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "pulseScale",
        finishedListener = {
            if (triggerBounce % 2 == 1) {
                triggerBounce++
            }
        }
    )

    Text(
        text = "$count",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.graphicsLayer {
            scaleX = pulseScale
            scaleY = pulseScale
        }
    )
}

/**
 * 全选/取消全选按钮
 * 带按压缩放动画
 */
@Composable
private fun SelectAllButton(
    isAllSelected: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按压缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "selectAllScale"
    )

    TextButton(
        onClick = if (isAllSelected) onDeselectAll else onSelectAll,
        contentPadding = PaddingValues(horizontal = 12.dp),
        interactionSource = interactionSource,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Icon(
            imageVector = if (isAllSelected)
                Icons.Default.Deselect
            else
                Icons.Default.SelectAll,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isAllSelected) "取消全选" else "全选",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * 选择模式顶栏状态
 * 
 * 用于简化顶栏参数传递
 */
data class SelectionTopBarState(
    val selectedCount: Int,
    val totalCount: Int,
    val onClose: () -> Unit,
    val onSelectAll: () -> Unit,
    val onDeselectAll: () -> Unit
) {
    val isAllSelected: Boolean get() = selectedCount == totalCount && totalCount > 0
    val hasSelection: Boolean get() = selectedCount > 0
}

/**
 * 便捷扩展：从 SelectionTopBarState 创建 SelectionTopBar
 */
@Composable
fun SelectionTopBar(
    state: SelectionTopBarState,
    modifier: Modifier = Modifier
) {
    SelectionTopBar(
        selectedCount = state.selectedCount,
        totalCount = state.totalCount,
        onClose = state.onClose,
        onSelectAll = state.onSelectAll,
        onDeselectAll = state.onDeselectAll,
        modifier = modifier
    )
}
