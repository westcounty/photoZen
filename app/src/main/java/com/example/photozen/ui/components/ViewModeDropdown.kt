package com.example.photozen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 视图模式下拉按钮组件 - 用于所有照片列表界面
 *
 * 提供两种视图模式切换和列数调整功能：
 * - 网格模式 (SQUARE): 等宽正方形，支持 2-5 列
 * - 瀑布流模式 (WATERFALL): 原比例，支持 1-5 列
 *
 * @param currentMode 当前视图模式
 * @param currentColumns 当前列数
 * @param onModeChanged 视图模式改变回调
 * @param onColumnsChanged 列数改变回调
 * @param modifier Modifier
 */
@Composable
fun ViewModeDropdownButton(
    currentMode: PhotoGridMode,
    currentColumns: Int,
    onModeChanged: (PhotoGridMode) -> Unit,
    onColumnsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // 根据当前模式确定列数范围
    val minColumns = if (currentMode == PhotoGridMode.WATERFALL) 1 else 2
    val maxColumns = 5

    IconButton(
        onClick = { expanded = true },
        modifier = modifier
    ) {
        Icon(
            imageVector = when (currentMode) {
                PhotoGridMode.SQUARE -> Icons.Default.GridView
                PhotoGridMode.WATERFALL -> Icons.Default.Dashboard
            },
            contentDescription = "视图模式"
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        // 视图模式区块标题
        Text(
            text = "视图模式",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 网格模式选项
        DropdownMenuItem(
            text = {
                Text(
                    text = "网格模式",
                    color = if (currentMode == PhotoGridMode.SQUARE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            },
            onClick = {
                if (currentMode != PhotoGridMode.SQUARE) {
                    onModeChanged(PhotoGridMode.SQUARE)
                    // 网格模式最小 2 列
                    if (currentColumns < 2) {
                        onColumnsChanged(2)
                    }
                }
                expanded = false
            },
            leadingIcon = {
                if (currentMode == PhotoGridMode.SQUARE) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = if (currentMode == PhotoGridMode.SQUARE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        )

        // 瀑布流模式选项
        DropdownMenuItem(
            text = {
                Text(
                    text = "瀑布流模式",
                    color = if (currentMode == PhotoGridMode.WATERFALL) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            },
            onClick = {
                if (currentMode != PhotoGridMode.WATERFALL) {
                    onModeChanged(PhotoGridMode.WATERFALL)
                }
                expanded = false
            },
            leadingIcon = {
                if (currentMode == PhotoGridMode.WATERFALL) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = if (currentMode == PhotoGridMode.WATERFALL) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 列数区块标题
        Text(
            text = "列数",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 列数选项
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (col in minColumns..maxColumns) {
                val isSelected = col == currentColumns
                ColumnNumberChip(
                    number = col,
                    isSelected = isSelected,
                    onClick = {
                        onColumnsChanged(col)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 列数选择芯片
 */
@Composable
private fun ColumnNumberChip(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.material3.FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text("$number") },
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

/**
 * 紧凑版视图模式切换按钮 - 仅切换模式，不显示列数
 */
@Composable
fun ViewModeToggleButton(
    currentMode: PhotoGridMode,
    onModeChanged: (PhotoGridMode) -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = {
            val newMode = when (currentMode) {
                PhotoGridMode.SQUARE -> PhotoGridMode.WATERFALL
                PhotoGridMode.WATERFALL -> PhotoGridMode.SQUARE
            }
            onModeChanged(newMode)
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = when (currentMode) {
                PhotoGridMode.SQUARE -> Icons.Default.GridView
                PhotoGridMode.WATERFALL -> Icons.Default.Dashboard
            },
            contentDescription = when (currentMode) {
                PhotoGridMode.SQUARE -> "切换到瀑布流"
                PhotoGridMode.WATERFALL -> "切换到网格"
            }
        )
    }
}
