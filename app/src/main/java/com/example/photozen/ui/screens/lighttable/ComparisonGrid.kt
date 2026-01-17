package com.example.photozen.ui.screens.lighttable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.PhotoEntity
import kotlin.math.min

/**
 * 布局配置
 */
private data class LayoutConfig(
    val rows: Int,
    val cols: Int,
    val photoWidth: Float,
    val photoHeight: Float,
    val totalArea: Float
) {
    // 每行的照片分布（用于处理不均匀分布，如3张照片时可以是2+1或1+2）
    var rowDistribution: List<Int> = emptyList()
}

/**
 * Comparison grid layout for Light Table.
 * Arranges 2-6 photos using intelligent layout algorithm.
 * 
 * The algorithm calculates the optimal layout by:
 * 1. Trying all possible row/column configurations
 * 2. Considering screen aspect ratio and photo aspect ratio
 * 3. Maximizing the display area while keeping all photos the same size
 * 
 * @param photos List of photos to compare (max 6)
 * @param transformState Shared transformation state for sync zoom
 * @param selectedPhotoIds Set of selected photo IDs
 * @param onSelectPhoto Callback when a photo is tapped
 * @param onFullscreenClick Callback when fullscreen button is clicked
 * @param modifier Modifier for the grid
 */
@Composable
fun ComparisonGrid(
    photos: List<PhotoEntity>,
    transformState: TransformState,
    selectedPhotoIds: Set<String>,
    onSelectPhoto: (String) -> Unit,
    onFullscreenClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val photoCount = photos.size.coerceAtMost(6)
    if (photoCount == 0) return
    
    val density = LocalDensity.current
    val spacing = 4.dp  // Reduced from 8.dp for more photo display area
    val spacingPx = with(density) { spacing.toPx() }
    
    // 计算照片的平均长宽比
    val avgAspectRatio = remember(photos) {
        if (photos.isEmpty()) 1f
        else {
            photos.map { photo ->
                if (photo.height > 0) photo.width.toFloat() / photo.height else 1f
            }.average().toFloat()
        }
    }
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing)  // Reduced padding for more display area
    ) {
        val containerWidth = constraints.maxWidth.toFloat() - spacingPx
        val containerHeight = constraints.maxHeight.toFloat() - spacingPx
        
        // 计算最优布局
        val optimalLayout = remember(photoCount, containerWidth, containerHeight, avgAspectRatio) {
            calculateOptimalLayout(
                photoCount = photoCount,
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                photoAspectRatio = avgAspectRatio,
                spacing = spacingPx
            )
        }
        
        // 渲染布局
        RenderLayout(
            layout = optimalLayout,
            photos = photos.take(photoCount),
            transformState = transformState,
            selectedPhotoIds = selectedPhotoIds,
            onSelectPhoto = onSelectPhoto,
            onFullscreenClick = onFullscreenClick,
            spacing = spacing
        )
    }
}

/**
 * 计算最优布局
 */
private fun calculateOptimalLayout(
    photoCount: Int,
    containerWidth: Float,
    containerHeight: Float,
    photoAspectRatio: Float,
    spacing: Float
): LayoutConfig {
    val possibleLayouts = mutableListOf<LayoutConfig>()
    
    // 生成所有可能的布局配置
    when (photoCount) {
        1 -> {
            possibleLayouts.add(createLayout(1, 1, containerWidth, containerHeight, photoAspectRatio, spacing))
        }
        2 -> {
            // 2张：1x2（左右）或 2x1（上下）
            possibleLayouts.add(createLayout(1, 2, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayout(2, 1, containerWidth, containerHeight, photoAspectRatio, spacing))
        }
        3 -> {
            // 3张：1x3, 3x1, 2行(2+1), 2行(1+2), 2列(2+1), 2列(1+2)
            possibleLayouts.add(createLayout(1, 3, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayout(3, 1, containerWidth, containerHeight, photoAspectRatio, spacing))
            // 2行，上2下1
            possibleLayouts.add(createLayoutWithDistribution(2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2, 1)))
            // 2行，上1下2
            possibleLayouts.add(createLayoutWithDistribution(2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(1, 2)))
        }
        4 -> {
            // 4张：2x2, 1x4, 4x1, 2行(3+1), 2行(1+3)
            possibleLayouts.add(createLayout(2, 2, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayout(1, 4, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayout(4, 1, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayoutWithDistribution(2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(3, 1)))
            possibleLayouts.add(createLayoutWithDistribution(2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(1, 3)))
        }
        5 -> {
            // 5张：多种组合
            possibleLayouts.add(createLayout(1, 5, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayout(5, 1, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayoutWithDistribution(2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(3, 2)))
            possibleLayouts.add(createLayoutWithDistribution(2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2, 3)))
            possibleLayouts.add(createLayoutWithDistribution(3, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2, 2, 1)))
            possibleLayouts.add(createLayoutWithDistribution(3, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(1, 2, 2)))
        }
        6 -> {
            // 6张：2x3, 3x2, 多种组合
            possibleLayouts.add(createLayout(2, 3, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayout(3, 2, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayout(1, 6, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayout(6, 1, containerWidth, containerHeight, photoAspectRatio, spacing))
            possibleLayouts.add(createLayoutWithDistribution(2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(4, 2)))
            possibleLayouts.add(createLayoutWithDistribution(2, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2, 4)))
            possibleLayouts.add(createLayoutWithDistribution(3, containerWidth, containerHeight, photoAspectRatio, spacing, listOf(2, 2, 2)))
        }
    }
    
    // 选择总面积最大的布局
    return possibleLayouts.maxByOrNull { it.totalArea } ?: possibleLayouts.first()
}

/**
 * 创建均匀分布的布局
 */
private fun createLayout(
    rows: Int,
    cols: Int,
    containerWidth: Float,
    containerHeight: Float,
    photoAspectRatio: Float,
    spacing: Float
): LayoutConfig {
    // 计算每个格子的可用空间
    val cellWidth = (containerWidth - spacing * (cols - 1)) / cols
    val cellHeight = (containerHeight - spacing * (rows - 1)) / rows
    
    // 根据照片长宽比计算实际显示尺寸
    val (photoWidth, photoHeight) = fitPhotoInCell(cellWidth, cellHeight, photoAspectRatio)
    
    return LayoutConfig(
        rows = rows,
        cols = cols,
        photoWidth = photoWidth,
        photoHeight = photoHeight,
        totalArea = photoWidth * photoHeight * rows * cols
    ).apply {
        rowDistribution = List(rows) { cols }
    }
}

/**
 * 创建不均匀分布的布局（如2+1, 3+2等）
 */
private fun createLayoutWithDistribution(
    rows: Int,
    containerWidth: Float,
    containerHeight: Float,
    photoAspectRatio: Float,
    spacing: Float,
    distribution: List<Int>
): LayoutConfig {
    val maxCols = distribution.maxOrNull() ?: 1
    val rowHeight = (containerHeight - spacing * (rows - 1)) / rows
    
    // 对于不均匀分布，所有照片应该大小一致
    // 使用最小的格子尺寸来保证一致性
    var minPhotoWidth = Float.MAX_VALUE
    var minPhotoHeight = Float.MAX_VALUE
    
    distribution.forEach { colsInRow ->
        val cellWidth = (containerWidth - spacing * (colsInRow - 1)) / colsInRow
        val (pw, ph) = fitPhotoInCell(cellWidth, rowHeight, photoAspectRatio)
        if (pw * ph < minPhotoWidth * minPhotoHeight) {
            minPhotoWidth = pw
            minPhotoHeight = ph
        }
    }
    
    val totalPhotos = distribution.sum()
    
    return LayoutConfig(
        rows = rows,
        cols = maxCols,
        photoWidth = minPhotoWidth,
        photoHeight = minPhotoHeight,
        totalArea = minPhotoWidth * minPhotoHeight * totalPhotos
    ).apply {
        rowDistribution = distribution
    }
}

/**
 * 在给定格子内适配照片，保持长宽比
 */
private fun fitPhotoInCell(
    cellWidth: Float,
    cellHeight: Float,
    photoAspectRatio: Float
): Pair<Float, Float> {
    val cellAspectRatio = cellWidth / cellHeight
    
    return if (photoAspectRatio > cellAspectRatio) {
        // 照片更宽，以宽度为准
        val width = cellWidth
        val height = width / photoAspectRatio
        width to height
    } else {
        // 照片更高，以高度为准
        val height = cellHeight
        val width = height * photoAspectRatio
        width to height
    }
}

/**
 * 渲染布局
 */
@Composable
private fun RenderLayout(
    layout: LayoutConfig,
    photos: List<PhotoEntity>,
    transformState: TransformState,
    selectedPhotoIds: Set<String>,
    onSelectPhoto: (String) -> Unit,
    onFullscreenClick: ((Int) -> Unit)?,
    spacing: Dp
) {
    val density = LocalDensity.current
    val photoWidthDp = with(density) { layout.photoWidth.toDp() }
    val photoHeightDp = with(density) { layout.photoHeight.toDp() }
    
    var photoIndex = 0
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        layout.rowDistribution.forEachIndexed { rowIndex, colsInRow ->
            val isFirstRow = rowIndex == 0
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(colsInRow) {
                    if (photoIndex < photos.size) {
                        val photo = photos[photoIndex]
                        val currentIndex = photoIndex
                        
                        SyncZoomImage(
                            photo = photo,
                            transformState = transformState,
                            isSelected = photo.id in selectedPhotoIds,
                            onSelect = { onSelectPhoto(photo.id) },
                            onFullscreenClick = onFullscreenClick?.let { { it(currentIndex) } },
                            // First row: place fullscreen button at bottom-right to avoid status bar
                            fullscreenButtonPosition = if (isFirstRow) Alignment.BottomEnd else Alignment.TopEnd,
                            modifier = Modifier
                                .width(photoWidthDp)
                                .height(photoHeightDp)
                        )
                        
                        photoIndex++
                    }
                }
            }
        }
    }
}
