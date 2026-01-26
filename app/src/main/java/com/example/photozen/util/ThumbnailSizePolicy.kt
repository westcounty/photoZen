package com.example.photozen.util

import coil3.request.ImageRequest
import coil3.size.Size

/**
 * 缩略图尺寸策略
 * 
 * 根据使用场景返回合适的缩略图尺寸，优化图片加载性能。
 * 
 * ## 设计原则
 * 
 * 1. **按需加载**：不同场景使用不同尺寸，避免加载过大图片
 * 2. **内存优化**：小尺寸缩略图减少内存占用
 * 3. **显示质量**：确保每个场景的图片清晰度足够
 * 
 * ## 使用示例
 * 
 * ```kotlin
 * // 在 Composable 中
 * AsyncImage(
 *     model = ImageRequest.Builder(context)
 *         .data(photo.systemUri)
 *         .withThumbnailPolicy(ThumbnailSizePolicy.Context.GRID_3_COLUMN)
 *         .crossfade(true)
 *         .build(),
 *     contentDescription = null
 * )
 * 
 * // 根据列数动态选择
 * val sizeContext = ThumbnailSizePolicy.contextForColumns(gridColumns)
 * ```
 * 
 * @since Phase 4 - 性能优化
 */
object ThumbnailSizePolicy {
    
    /**
     * 使用场景
     */
    enum class Context {
        /** 4 列网格（最小缩略图） */
        GRID_4_COLUMN,
        
        /** 3 列网格 */
        GRID_3_COLUMN,
        
        /** 2 列网格 */
        GRID_2_COLUMN,
        
        /** 单列网格（较大缩略图） */
        GRID_1_COLUMN,
        
        /** 卡片预览（滑动整理） */
        CARD_PREVIEW,
        
        /** 全屏查看（使用原图） */
        FULLSCREEN,
        
        /** 小缩略图（确认对话框、列表项等） */
        THUMBNAIL_SMALL,
        
        /** 中等缩略图（Light Table 比较） */
        THUMBNAIL_MEDIUM,
        
        /** 时间线缩略图 */
        TIMELINE
    }
    
    /**
     * 获取指定场景的缩略图尺寸
     *
     * @param context 使用场景
     * @return 推荐的尺寸，FULLSCREEN 返回 Size.ORIGINAL
     */
    fun getSizeForContext(context: Context): Size = when (context) {
        Context.GRID_4_COLUMN -> Size(200, 200)
        Context.GRID_3_COLUMN -> Size(300, 300)
        Context.GRID_2_COLUMN -> Size(450, 450)
        Context.GRID_1_COLUMN -> Size(800, 800)
        Context.CARD_PREVIEW -> Size(1200, 1200)  // 增大卡片预览尺寸，适配高分屏
        Context.FULLSCREEN -> Size.ORIGINAL
        Context.THUMBNAIL_SMALL -> Size(120, 120)
        Context.THUMBNAIL_MEDIUM -> Size(600, 600)
        Context.TIMELINE -> Size(300, 300)
    }
    
    /**
     * 获取像素尺寸（用于不支持 Size 类型的场景）
     *
     * @param context 使用场景
     * @return Pair(width, height)，FULLSCREEN 返回 null 表示原图
     */
    fun getPixelSizeForContext(context: Context): Pair<Int, Int>? = when (context) {
        Context.GRID_4_COLUMN -> 200 to 200
        Context.GRID_3_COLUMN -> 300 to 300
        Context.GRID_2_COLUMN -> 450 to 450
        Context.GRID_1_COLUMN -> 800 to 800
        Context.CARD_PREVIEW -> 1200 to 1200
        Context.FULLSCREEN -> null
        Context.THUMBNAIL_SMALL -> 120 to 120
        Context.THUMBNAIL_MEDIUM -> 600 to 600
        Context.TIMELINE -> 300 to 300
    }
    
    /**
     * 根据网格列数获取合适的场景
     * 
     * @param columns 网格列数
     * @return 对应的场景
     */
    fun contextForColumns(columns: Int): Context = when (columns) {
        1 -> Context.GRID_1_COLUMN
        2 -> Context.GRID_2_COLUMN
        3 -> Context.GRID_3_COLUMN
        else -> Context.GRID_4_COLUMN
    }
    
    /**
     * 根据屏幕宽度和列数计算合适的尺寸
     * 
     * @param screenWidthPx 屏幕宽度（像素）
     * @param columns 列数
     * @param paddingPx 总内边距（像素）
     * @return 每个 item 的建议尺寸
     */
    fun calculateItemSize(screenWidthPx: Int, columns: Int, paddingPx: Int = 0): Int {
        val availableWidth = screenWidthPx - paddingPx
        val itemWidth = availableWidth / columns
        // 返回略大于显示尺寸的值，确保缩放时仍清晰
        return (itemWidth * 1.5f).toInt().coerceIn(80, 800)
    }
    
    /**
     * 获取内存缓存大小建议
     * 
     * 基于场景返回建议的内存缓存大小（字节）
     * 
     * @param context 使用场景
     * @return 建议的单张图片内存大小
     */
    fun estimateMemorySize(context: Context): Long {
        val (width, height) = getPixelSizeForContext(context) ?: return 0L
        // ARGB_8888: 4 bytes per pixel
        return width.toLong() * height.toLong() * 4
    }
    
    /**
     * 场景名称（用于日志和调试）
     */
    fun Context.displayName(): String = when (this) {
        Context.GRID_4_COLUMN -> "4列网格"
        Context.GRID_3_COLUMN -> "3列网格"
        Context.GRID_2_COLUMN -> "2列网格"
        Context.GRID_1_COLUMN -> "单列网格"
        Context.CARD_PREVIEW -> "卡片预览"
        Context.FULLSCREEN -> "全屏"
        Context.THUMBNAIL_SMALL -> "小缩略图"
        Context.THUMBNAIL_MEDIUM -> "中等缩略图"
        Context.TIMELINE -> "时间线"
    }
}

// ============== Coil ImageRequest 扩展函数 ==============

/**
 * 应用缩略图策略到 ImageRequest
 * 
 * @param context 使用场景
 * @return 配置了尺寸的 Builder
 */
fun ImageRequest.Builder.withThumbnailPolicy(
    context: ThumbnailSizePolicy.Context
): ImageRequest.Builder {
    val size = ThumbnailSizePolicy.getSizeForContext(context)
    return if (size == Size.ORIGINAL) {
        this
    } else {
        this.size(size)
    }
}

/**
 * 根据列数应用缩略图策略
 * 
 * @param columns 网格列数
 * @return 配置了尺寸的 Builder
 */
fun ImageRequest.Builder.withThumbnailPolicyForColumns(
    columns: Int
): ImageRequest.Builder {
    val context = ThumbnailSizePolicy.contextForColumns(columns)
    return withThumbnailPolicy(context)
}

/**
 * 应用小缩略图策略（适用于对话框、列表项等）
 */
fun ImageRequest.Builder.withSmallThumbnail(): ImageRequest.Builder =
    withThumbnailPolicy(ThumbnailSizePolicy.Context.THUMBNAIL_SMALL)

/**
 * 应用卡片预览策略（适用于滑动整理）
 */
fun ImageRequest.Builder.withCardPreview(): ImageRequest.Builder =
    withThumbnailPolicy(ThumbnailSizePolicy.Context.CARD_PREVIEW)

/**
 * 应用时间线策略
 */
fun ImageRequest.Builder.withTimelineThumbnail(): ImageRequest.Builder =
    withThumbnailPolicy(ThumbnailSizePolicy.Context.TIMELINE)
