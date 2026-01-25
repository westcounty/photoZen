package com.example.photozen.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.domain.model.FilterConfig
import com.example.photozen.domain.model.FilterPreset
import com.example.photozen.ui.theme.PicZenMotion

/**
 * 筛选面板 - 底部抽屉形式
 * 
 * 完整的筛选条件设置面板，包含：
 * - 预设快捷选择
 * - 相册多选
 * - 日期范围选择
 * - 保存预设
 * 
 * @param currentConfig 当前筛选配置
 * @param presets 已保存的预设列表
 * @param albums 可选相册列表
 * @param onConfigChange 配置变更回调
 * @param onSavePreset 保存预设回调
 * @param onApplyPreset 应用预设回调
 * @param onDeletePreset 删除预设回调
 * @param onDismiss 关闭面板回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentConfig: FilterConfig,
    presets: List<FilterPreset>,
    albums: List<AlbumBubbleEntity>,
    onConfigChange: (FilterConfig) -> Unit,
    onSavePreset: (String) -> Unit,
    onApplyPreset: (FilterPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 本地编辑状态
    var localConfig by remember(currentConfig) { mutableStateOf(currentConfig) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .verticalScroll(scrollState)
        ) {
            // 标题栏
            FilterSheetHeader(
                onReset = { localConfig = FilterConfig.EMPTY }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 预设快捷选择
            if (presets.isNotEmpty()) {
                FilterPresetsSection(
                    presets = presets,
                    onApplyPreset = { preset ->
                        localConfig = preset.config
                        onApplyPreset(preset)
                    },
                    onDeletePreset = onDeletePreset
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // 相册选择
            FilterSection(
                title = "相册",
                icon = Icons.Default.Collections,
                trailing = {
                    val selectedAlbumIds = localConfig.albumIds?.toSet() ?: emptySet()
                    Row {
                        TextButton(
                            onClick = {
                                localConfig = localConfig.copy(
                                    albumIds = albums.map { it.bucketId }
                                )
                            },
                            enabled = selectedAlbumIds.size < albums.size
                        ) {
                            Text("全选")
                        }
                        TextButton(
                            onClick = {
                                localConfig = localConfig.copy(albumIds = null)
                            },
                            enabled = selectedAlbumIds.isNotEmpty()
                        ) {
                            Text("清除")
                        }
                    }
                }
            ) {
                AlbumMultiSelector(
                    albums = albums,
                    selectedIds = localConfig.albumIds?.toSet() ?: emptySet(),
                    onSelectionChange = { ids ->
                        localConfig = localConfig.copy(
                            albumIds = ids.takeIf { it.isNotEmpty() }?.toList()
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 日期范围
            FilterSection(
                title = "日期范围",
                icon = Icons.Default.DateRange
            ) {
                DateRangePicker(
                    startDate = localConfig.startDate,
                    endDate = localConfig.endDate,
                    onRangeChange = { start, end ->
                        localConfig = localConfig.copy(
                            startDate = start,
                            endDate = end
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 操作按钮
            FilterActionButtons(
                canSavePreset = !localConfig.isEmpty && presets.size < FilterPreset.MAX_PRESETS,
                onSavePreset = { showSaveDialog = true },
                onApply = {
                    onConfigChange(localConfig)
                    onDismiss()
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // 保存预设对话框
    if (showSaveDialog) {
        SavePresetDialog(
            onConfirm = { name ->
                onSavePreset(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }
}

/**
 * 筛选面板标题栏
 */
@Composable
private fun FilterSheetHeader(
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "筛选条件",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onReset) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("重置")
        }
    }
}

/**
 * 预设快捷选择区域
 */
@Composable
private fun FilterPresetsSection(
    presets: List<FilterPreset>,
    onApplyPreset: (FilterPreset) -> Unit,
    onDeletePreset: (String) -> Unit
) {
    Column {
        Text(
            text = "常用预设",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets, key = { it.id }) { preset ->
                AssistChip(
                    onClick = { onApplyPreset(preset) },
                    label = { Text(preset.name) },
                    trailingIcon = {
                        IconButton(
                            onClick = { onDeletePreset(preset.id) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "删除预设",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * 筛选区域容器
 *
 * @param title 区域标题
 * @param icon 标题图标
 * @param trailing 可选的尾部内容（如按钮），显示在标题行右侧
 * @param content 区域内容
 */
@Composable
private fun FilterSection(
    title: String,
    icon: ImageVector,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

/**
 * 底部操作按钮
 *
 * 增强动画:
 * - 按钮按压缩放 (0.97f)
 */
@Composable
private fun FilterActionButtons(
    canSavePreset: Boolean,
    onSavePreset: () -> Unit,
    onApply: () -> Unit
) {
    // 保存按钮交互
    val saveInteractionSource = remember { MutableInteractionSource() }
    val isSavePressed by saveInteractionSource.collectIsPressedAsState()
    val saveScale by animateFloatAsState(
        targetValue = if (isSavePressed) 0.97f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "saveScale"
    )

    // 应用按钮交互
    val applyInteractionSource = remember { MutableInteractionSource() }
    val isApplyPressed by applyInteractionSource.collectIsPressedAsState()
    val applyScale by animateFloatAsState(
        targetValue = if (isApplyPressed) 0.97f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "applyScale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 保存为预设
        OutlinedButton(
            onClick = onSavePreset,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = saveScale
                    scaleY = saveScale
                },
            enabled = canSavePreset,
            interactionSource = saveInteractionSource
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("保存预设")
        }

        // 应用
        Button(
            onClick = onApply,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = applyScale
                    scaleY = applyScale
                },
            interactionSource = applyInteractionSource
        ) {
            Text("应用")
        }
    }
}
