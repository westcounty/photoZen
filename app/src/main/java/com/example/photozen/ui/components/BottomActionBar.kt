package com.example.photozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * Standard action item for bottom bar with vertical icon + text layout.
 * Designed for consistent UI across all photo list screens.
 */
@Composable
fun BottomBarActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) color.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Data class representing a bottom bar action button.
 */
data class BottomBarAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

/**
 * Standard selection bottom bar container with horizontal scrolling support.
 * Uses LazyRow when there are more than 5 buttons to enable scrolling.
 *
 * REQ-025: 底部操作栏滑动提示
 * - 按钮展示不全时支持左右滑动
 * - 右侧有渐变遮罩提示用户有更多按钮
 */
@Composable
fun SelectionBottomBar(
    actions: List<BottomBarAction>,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    // 检查是否可以继续向右滚动 (REQ-025)
    val showEndIndicator by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            if (totalItemsCount == 0) return@derivedStateOf false

            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index < totalItemsCount - 1
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            LazyRow(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = if (actions.size <= 5) Arrangement.SpaceEvenly else Arrangement.spacedBy(4.dp),
                contentPadding = if (actions.size <= 5) PaddingValues(horizontal = 4.dp) else PaddingValues(horizontal = 8.dp, vertical = 0.dp).also {
                    // 为右侧渐变指示器预留空间
                    if (showEndIndicator) PaddingValues(start = 8.dp, end = 40.dp)
                }
            ) {
                items(actions) { action ->
                    BottomBarActionItem(
                        icon = action.icon,
                        label = action.label,
                        color = action.color,
                        onClick = action.onClick,
                        enabled = action.enabled
                    )
                }
            }

            // 右侧渐变遮罩提示 (REQ-025)
            if (showEndIndicator) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(40.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "更多操作",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============== Predefined Action Builders ==============

/**
 * Create a "Keep" (保留) action.
 */
fun keepAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.Favorite,
    label = "保留",
    color = KeepGreen,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Maybe" (待定) action.
 */
fun maybeAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.QuestionMark,
    label = "待定",
    color = MaybeAmber,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Delete" (删除) action - move to trash.
 */
fun deleteAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.Delete,
    label = "删除",
    color = TrashRed,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Permanent Delete" (彻删) action.
 */
fun permanentDeleteAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.DeleteForever,
    label = "彻删",
    color = TrashRed,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Reset" (重置) action - restore to unsorted.
 */
@Composable
fun resetAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.AutoMirrored.Filled.Undo,
    label = "重置",
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create an "Album" (相册) action - add to album.
 */
@Composable
fun albumAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.PhotoAlbum,
    label = "相册",
    color = MaterialTheme.colorScheme.primary,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Move" (移动) action - move to another album.
 */
@Composable
fun moveAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.Folder,
    label = "移动",
    color = MaterialTheme.colorScheme.primary,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Copy" (复制) action - copy to another album.
 */
@Composable
fun copyAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.ContentCopy,
    label = "复制",
    color = MaterialTheme.colorScheme.secondary,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create an "Edit" (编辑) action.
 */
@Composable
fun editAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.Edit,
    label = "编辑",
    color = MaterialTheme.colorScheme.tertiary,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Share" (分享) action.
 */
fun shareAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.Share,
    label = "分享",
    color = Color(0xFF1E88E5),
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Filter" (筛选) action - start filtering from this photo.
 */
@Composable
fun filterAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.FilterList,
    label = "筛选",
    color = MaterialTheme.colorScheme.secondary,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Start From Here" (从此开始) action - start sorting from this photo.
 * Used in album photo list for single selection.
 */
@Composable
fun startFromHereAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.FilterList,
    label = "从此开始",
    color = MaterialTheme.colorScheme.primary,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Discard" (丢弃) action for light table comparison.
 */
fun discardAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.Delete,
    label = "丢弃",
    color = TrashRed,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Clear" (清除) action - clear all selections (REQ-031).
 */
@Composable
fun clearAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.Clear,
    label = "清除",
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Compare" (对比) action - enter compare mode (REQ-031).
 */
@Composable
fun compareAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.AutoMirrored.Filled.CompareArrows,
    label = "对比",
    color = MaterialTheme.colorScheme.primary,
    onClick = onClick,
    enabled = enabled
)

/**
 * Create a "Change Status" (改状态) action - batch change status (REQ-048).
 */
@Composable
fun changeStatusAction(onClick: () -> Unit, enabled: Boolean = true) = BottomBarAction(
    icon = Icons.Default.SwapVert,
    label = "改状态",
    color = MaterialTheme.colorScheme.secondary,
    onClick = onClick,
    enabled = enabled
)
