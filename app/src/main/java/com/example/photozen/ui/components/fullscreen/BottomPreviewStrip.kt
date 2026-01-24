package com.example.photozen.ui.components.fullscreen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.photozen.data.local.entity.PhotoEntity
import kotlin.math.abs

/**
 * PhotoZen 底部照片预览条 (REQ-014)
 * =================================
 *
 * 特性:
 * - 固定高宽比 2:1 (高度:宽度)
 * - 当前照片居中并高亮显示
 * - 丝滑滑动，有惯性
 * - 滑动过程中实时将预览照片替换为预览条中央的照片
 * - 点击预览条中的照片切换到该照片
 *
 * @param photos 照片列表
 * @param currentIndex 当前照片索引
 * @param onIndexChange 索引变更回调
 * @param modifier Modifier
 * @param itemWidth 普通项宽度
 * @param currentItemWidth 当前项宽度 (高亮显示)
 */
@Composable
fun BottomPreviewStrip(
    photos: List<PhotoEntity>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 36.dp,
    currentItemWidth: Dp = 48.dp
) {
    val listState = rememberLazyListState()

    // 当前索引变化时，滚动到居中位置
    LaunchedEffect(currentIndex) {
        // 计算滚动偏移使当前项居中
        val viewportWidth = listState.layoutInfo.viewportSize.width
        if (viewportWidth > 0 && photos.isNotEmpty()) {
            // 简化处理：滚动到当前项
            listState.animateScrollToItem(
                index = maxOf(0, currentIndex),
                scrollOffset = 0
            )
        }
    }

    // 检测滑动时的中央项
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                currentIndex
            } else {
                val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.width / 2
                layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
                    abs(itemInfo.offset + itemInfo.size / 2 - viewportCenter)
                }?.index ?: currentIndex
            }
        }
    }

    // 滑动结束时更新当前索引
    LaunchedEffect(listState.isScrollInProgress, centerIndex) {
        if (!listState.isScrollInProgress && centerIndex != currentIndex) {
            onIndexChange(centerIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(
            items = photos,
            key = { _, photo -> "preview_${photo.id}" }
        ) { index, photo ->
            val isCurrent = index == currentIndex
            val width = if (isCurrent) currentItemWidth else itemWidth
            val height = width * 2  // 高宽比 2:1

            PreviewStripItem(
                photo = photo,
                isCurrent = isCurrent,
                width = width,
                height = height,
                onClick = { onIndexChange(index) }
            )
        }
    }
}

/**
 * 预览条单个项目
 */
@Composable
private fun PreviewStripItem(
    photo: PhotoEntity,
    isCurrent: Boolean,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .then(
                if (isCurrent) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(photo.systemUri))
                .crossfade(100)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 非当前项添加半透明遮罩
        if (!isCurrent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
    }
}
