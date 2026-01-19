package com.example.photozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 整理模式选择底部弹窗
 * 
 * 合并原有的"快速整理"和"一站式整理"两个入口，
 * 通过底部弹窗让用户选择整理模式。
 * 
 * ## 设计目的
 * 
 * 原首页有两个独立的整理入口，用户容易困惑它们的区别。
 * 现在统一为一个"开始整理"按钮，点击后弹出此选择框，
 * 通过清晰的图标和描述文字帮助用户选择合适的模式。
 * 
 * ## 整理模式
 * 
 * 1. **快速滑动**: 适合快速决策，左滑删除/右滑保留
 * 2. **深度整理**: 适合彻底整理，包含筛选→对比→分类→清理完整流程
 * 
 * ## 筛选选择支持
 * 
 * 当 needsFilterSelection 为 true 时，选择模式后不直接进入整理流程，
 * 而是先跳转到照片筛选选择页面（PhotoFilterSelectionScreen），
 * 让用户选择要整理哪些照片。
 * 
 * @param onDismiss 关闭弹窗回调
 * @param onQuickSortSelected 快速滑动模式选中回调
 * @param onWorkflowSelected 深度整理模式选中回调
 * @param unsortedCount 待整理照片数量
 * @param needsFilterSelection 是否需要先进行筛选选择
 * @param onFilterSelectionRequired 需要筛选选择时的回调，参数为模式标识（"flow" 或 "workflow"）
 * 
 * @since Phase 1-A (骨架) → Phase 1-D (启用)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortModeBottomSheet(
    onDismiss: () -> Unit,
    onQuickSortSelected: () -> Unit,
    onWorkflowSelected: () -> Unit,
    unsortedCount: Int,
    needsFilterSelection: Boolean = false,
    onFilterSelectionRequired: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // 标题
            Text(
                text = "选择整理方式",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // 副标题：待整理数量
            Text(
                text = "$unsortedCount 张照片待整理",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 选项 1: 快速滑动
            SortModeOption(
                icon = Icons.Default.SwipeRight,
                title = "快速滑动",
                description = "左滑删除，右滑保留，快速决策",
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = {
                    if (needsFilterSelection) {
                        onFilterSelectionRequired("flow")
                    } else {
                        onQuickSortSelected()
                    }
                    onDismiss()
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 选项 2: 深度整理
            SortModeOption(
                icon = Icons.Default.Rocket,
                title = "深度整理",
                description = "筛选 → 对比 → 分类，彻底整理",
                iconTint = Color(0xFF8B5CF6), // Purple
                onClick = {
                    if (needsFilterSelection) {
                        onFilterSelectionRequired("workflow")
                    } else {
                        onWorkflowSelected()
                    }
                    onDismiss()
                }
            )
            
            // 底部安全区间距
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 整理模式选项
 * 
 * 单个整理模式的展示卡片，包含图标、标题、描述。
 * 
 * @param icon 模式图标
 * @param title 模式标题
 * @param description 模式描述
 * @param iconTint 图标颜色
 * @param onClick 点击回调
 */
@Composable
private fun SortModeOption(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文字内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 箭头
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
