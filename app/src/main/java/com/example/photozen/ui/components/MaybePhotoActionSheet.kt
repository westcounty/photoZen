package com.example.photozen.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.delay

/**
 * 待定照片操作菜单 (仅用于待定照片列表)
 *
 * 长按照片时显示，提供快捷操作：
 * - 标记为保留
 * - 移至回收站
 * - 彻底删除
 *
 * @param photo 当前操作的照片
 * @param onDismiss 关闭菜单
 * @param onMarkAsKeep 标记为保留
 * @param onMoveToTrash 移至回收站
 * @param onPermanentDelete 彻底删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaybePhotoActionSheet(
    photo: PhotoEntity,
    onDismiss: () -> Unit,
    onMarkAsKeep: () -> Unit,
    onMoveToTrash: () -> Unit,
    onPermanentDelete: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 24.dp)
        ) {
            // 照片预览和信息区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 照片缩略图
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(photo.systemUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // 照片信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = photo.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 状态标签
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaybeAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "待定",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaybeAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        // 尺寸信息
                        if (photo.width > 0 && photo.height > 0) {
                            Text(
                                text = "${photo.width} × ${photo.height}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 操作选项
            MaybeActionItem(
                icon = Icons.Default.Check,
                title = "标记为保留",
                subtitle = "将照片移至「保留」列表",
                iconTint = KeepGreen,
                iconBackgroundColor = KeepGreen.copy(alpha = 0.12f),
                animationDelay = 0,
                onClick = {
                    onMarkAsKeep()
                    onDismiss()
                }
            )

            MaybeActionItem(
                icon = Icons.Default.Delete,
                title = "移至回收站",
                subtitle = "可以在回收站中恢复",
                iconTint = TrashRed,
                iconBackgroundColor = TrashRed.copy(alpha = 0.12f),
                animationDelay = 50,
                onClick = {
                    onMoveToTrash()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            MaybeActionItem(
                icon = Icons.Default.DeleteForever,
                title = "彻底删除",
                subtitle = "从设备中永久删除，无法恢复",
                iconTint = Color(0xFFB71C1C), // 更深的红色
                iconBackgroundColor = Color(0xFFB71C1C).copy(alpha = 0.12f),
                animationDelay = 100,
                isDangerous = true,
                onClick = {
                    onPermanentDelete()
                    onDismiss()
                }
            )
        }
    }
}

/**
 * 操作菜单项
 */
@Composable
private fun MaybeActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color,
    iconBackgroundColor: Color,
    animationDelay: Int = 0,
    isDangerous: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 入场动画
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        isVisible = true
    }

    val entryAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(PicZenMotion.Duration.Fast),
        label = "entryAlpha"
    )
    val entryOffsetX by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 24.dp,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "entryOffsetX"
    )

    // 按压缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "pressScale"
    )

    // 图标按压右移
    val iconOffset by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "iconOffset"
    )

    // 背景色按压变化
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.06f else 0f,
        animationSpec = tween(PicZenMotion.Duration.Quick),
        label = "backgroundAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entryAlpha
                scaleX = scale
                scaleY = scale
                translationX = entryOffsetX.toPx()
            }
            .background(
                if (isDangerous) {
                    TrashRed.copy(alpha = backgroundAlpha)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = backgroundAlpha)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(48.dp)
                .offset(x = iconOffset)
                .clip(CircleShape)
                .background(iconBackgroundColor),
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDangerous) {
                    Color(0xFFB71C1C)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDangerous) {
                        Color(0xFFB71C1C).copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
