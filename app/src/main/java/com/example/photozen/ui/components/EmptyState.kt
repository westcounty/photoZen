package com.example.photozen.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.PicZenTokens
import com.example.photozen.ui.theme.TrashRed

/**
 * 统一的空状态组件
 * 
 * 提供一致的空状态视觉设计，包括图标、标题、描述和可选操作按钮。
 * 用于各种列表为空、搜索无结果、加载失败等场景。
 * 
 * @param icon 显示图标
 * @param title 标题文字
 * @param description 描述文字
 * @param modifier Modifier
 * @param iconTint 图标颜色
 * @param iconSize 图标大小
 * @param action 可选的操作按钮
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    iconSize: Dp = 56.dp,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标容器（带背景圆圈）
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

/**
 * 带浮动动画的空状态组件 (DES-028)
 *
 * 增强版空状态，图标区域有微妙的浮动动画效果，
 * 增加视觉趣味性和空间感。
 *
 * @param icon 显示图标
 * @param title 标题文字
 * @param description 描述文字
 * @param modifier Modifier
 * @param iconTint 图标颜色
 * @param iconSize 图标大小
 * @param action 可选的操作按钮
 */
@Composable
fun AnimatedEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    iconSize: Dp = PicZenTokens.IconSize.XXXL,
    action: (@Composable () -> Unit)? = null
) {
    // 浮动动画
    val infiniteTransition = rememberInfiniteTransition(label = "emptyStateFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    // 脉冲光晕动画
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PicZenTokens.Spacing.XXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 带浮动效果的图标容器
        Box(
            contentAlignment = Alignment.Center
        ) {
            // 外层光晕
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .offset(y = floatOffset.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                iconTint.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 主图标容器
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(y = floatOffset.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        Spacer(modifier = Modifier.height(PicZenTokens.Spacing.XL))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(PicZenTokens.Spacing.S))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(PicZenTokens.Spacing.XL))
            action()
        }
    }
}

/**
 * 卡片式空状态组件 (DES-028)
 *
 * 带卡片背景的空状态，适用于需要更强视觉边界的场景。
 */
@Composable
fun CardEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    action: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(PicZenTokens.Radius.L)
    ) {
        AnimatedEmptyState(
            icon = icon,
            title = title,
            description = description,
            iconTint = iconTint,
            action = action
        )
    }
}

/**
 * 紧凑型空状态组件
 *
 * 用于空间有限的场景，如列表项内嵌空状态。
 */
@Composable
fun CompactEmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 预定义的空状态组件集合
 * 
 * 提供常用场景的空状态实现，确保全应用视觉一致性。
 */
object EmptyStates {
    
    /**
     * 所有照片已整理完成
     */
    @Composable
    fun AllSorted(
        onRefresh: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.CheckCircle,
            title = "太棒了！",
            description = "所有照片都已整理完成",
            iconTint = KeepGreen,
            modifier = modifier
        ) {
            FilledTonalButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("刷新照片")
            }
        }
    }
    
    /**
     * 没有待定照片
     */
    @Composable
    fun NoMaybe(
        onGoSort: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.QuestionMark,
            title = "没有待定照片",
            description = "整理照片时向上滑动可标记为待定",
            iconTint = MaybeAmber,
            modifier = modifier
        ) {
            onGoSort?.let {
                FilledTonalButton(onClick = it) {
                    Text("去整理照片")
                }
            }
        }
    }
    
    /**
     * 回收站为空
     */
    @Composable
    fun EmptyTrash(
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.Delete,
            title = "回收站是空的",
            description = "整理照片时向左滑动可移入回收站",
            iconTint = TrashRed,
            modifier = modifier
        )
    }
    
    /**
     * 没有保留照片
     */
    @Composable
    fun NoKeep(
        onGoSort: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.Favorite,
            title = "还没有保留的照片",
            description = "整理照片时向右滑动可标记为保留",
            iconTint = KeepGreen,
            modifier = modifier
        ) {
            onGoSort?.let {
                FilledTonalButton(onClick = it) {
                    Text("开始整理")
                }
            }
        }
    }
    
    /**
     * 搜索无结果
     */
    @Composable
    fun NoSearchResults(
        query: String,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.SearchOff,
            title = "未找到结果",
            description = "没有找到与 \"$query\" 相关的内容",
            modifier = modifier
        )
    }
    
    /**
     * 相册为空
     */
    @Composable
    fun EmptyAlbum(
        albumName: String,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.PhotoAlbum,
            title = "相册是空的",
            description = "\"$albumName\" 中还没有照片",
            modifier = modifier
        )
    }
    
    /**
     * 时间线为空
     */
    @Composable
    fun EmptyTimeline(
        onGoSort: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.Timeline,
            title = "时间线是空的",
            description = "整理一些照片后这里会显示时间线",
            modifier = modifier
        ) {
            onGoSort?.let {
                FilledTonalButton(onClick = it) {
                    Text("开始整理")
                }
            }
        }
    }
    
    /**
     * 没有未整理的照片
     */
    @Composable
    fun NoUnsorted(
        onRefresh: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.DoneAll,
            title = "没有待整理照片",
            description = "恭喜！当前筛选范围内的照片都已整理",
            iconTint = KeepGreen,
            modifier = modifier
        ) {
            onRefresh?.let {
                FilledTonalButton(onClick = it) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("刷新")
                }
            }
        }
    }
    
    /**
     * 加载失败
     */
    @Composable
    fun LoadError(
        message: String = "加载失败",
        onRetry: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.ErrorOutline,
            title = "出错了",
            description = message,
            iconTint = MaterialTheme.colorScheme.error,
            modifier = modifier
        ) {
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("重试")
            }
        }
    }
    
    /**
     * 网络错误
     */
    @Composable
    fun NetworkError(
        onRetry: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.WifiOff,
            title = "网络连接失败",
            description = "请检查网络连接后重试",
            iconTint = MaterialTheme.colorScheme.error,
            modifier = modifier
        ) {
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("重试")
            }
        }
    }
    
    /**
     * 无权限
     */
    @Composable
    fun NoPermission(
        onRequestPermission: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.Lock,
            title = "需要权限",
            description = "请授予照片访问权限以继续使用",
            iconTint = MaybeAmber,
            modifier = modifier
        ) {
            Button(onClick = onRequestPermission) {
                Text("授予权限")
            }
        }
    }
    
    /**
     * 功能开发中
     */
    @Composable
    fun ComingSoon(
        featureName: String = "此功能",
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.Construction,
            title = "敬请期待",
            description = "$featureName 正在开发中",
            iconTint = MaybeAmber,
            modifier = modifier
        )
    }
    
    /**
     * 筛选无结果
     */
    @Composable
    fun NoFilterResults(
        onClearFilter: (() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        EmptyState(
            icon = Icons.Default.FilterAlt,
            title = "没有匹配的照片",
            description = "尝试调整筛选条件",
            modifier = modifier
        ) {
            onClearFilter?.let {
                FilledTonalButton(onClick = it) {
                    Text("清除筛选")
                }
            }
        }
    }
}
