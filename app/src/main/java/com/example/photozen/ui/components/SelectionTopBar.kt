package com.example.photozen.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "退出选择模式"
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
                Text(
                    text = "$selectedCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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
 * 全选/取消全选按钮
 */
@Composable
private fun SelectAllButton(
    isAllSelected: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    TextButton(
        onClick = if (isAllSelected) onDeselectAll else onSelectAll,
        contentPadding = PaddingValues(horizontal = 12.dp)
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
