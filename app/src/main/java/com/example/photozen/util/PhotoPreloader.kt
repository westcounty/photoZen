package com.example.photozen.util

import android.content.Context
import android.net.Uri
import android.util.Log
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.example.photozen.data.local.entity.PhotoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PhotoPreloader"

/**
 * 照片预加载器
 * 
 * 智能预加载即将显示的照片，提升用户体验。
 * 
 * ## 功能特点
 * 
 * 1. **滑动场景预加载**：预加载下一张/下几张照片
 * 2. **网格场景预加载**：预加载可见区域周围的照片
 * 3. **去重处理**：避免重复预加载同一张照片
 * 4. **取消支持**：支持取消正在进行的预加载
 * 
 * ## 使用示例
 * 
 * ```kotlin
 * // 在 ViewModel 中
 * @HiltViewModel
 * class FlowSorterViewModel @Inject constructor(
 *     private val preloader: PhotoPreloader
 * ) : ViewModel() {
 *     
 *     fun onPhotoChanged(currentIndex: Int) {
 *         preloader.preloadForSwipe(photos, currentIndex)
 *     }
 * }
 * 
 * // 在 Composable 中监听滚动
 * LaunchedEffect(gridState.firstVisibleItemIndex) {
 *     preloader.preloadForGrid(photos, visibleRange, columns)
 * }
 * ```
 * 
 * ## 注意事项
 * 
 * - 预加载是后台操作，不会阻塞主线程
 * - 预加载失败会静默处理，不影响主流程
 * - 页面离开时应调用 `cancelAll()` 释放资源
 * 
 * @since Phase 4 - 性能优化
 */
@Singleton
class PhotoPreloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 正在预加载的任务
    private val preloadJobs = mutableMapOf<String, Job>()
    
    // 已预加载的照片 URI（避免重复预加载）
    private val preloadedUris = mutableSetOf<String>()
    
    // 最大并发预加载数量
    private val maxConcurrentPreloads = 5
    
    /**
     * 预加载单张照片
     * 
     * @param photoUri 照片 URI
     * @param sizeContext 缩略图尺寸场景
     */
    fun preload(
        photoUri: String,
        sizeContext: ThumbnailSizePolicy.Context = ThumbnailSizePolicy.Context.CARD_PREVIEW
    ) {
        // 检查是否已预加载或正在预加载
        if (preloadedUris.contains(photoUri) || preloadJobs.containsKey(photoUri)) {
            return
        }
        
        // 限制并发数量
        if (preloadJobs.size >= maxConcurrentPreloads) {
            // 取消最早的预加载任务
            preloadJobs.entries.firstOrNull()?.let { (uri, job) ->
                job.cancel()
                preloadJobs.remove(uri)
            }
        }
        
        val job = scope.launch {
            try {
                val request = ImageRequest.Builder(context)
                    .data(Uri.parse(photoUri))
                    .withThumbnailPolicy(sizeContext)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                
                imageLoader.enqueue(request)
                preloadedUris.add(photoUri)
                Log.d(TAG, "Preloaded: $photoUri")
            } catch (e: Exception) {
                // 预加载失败不影响主流程
                Log.w(TAG, "Preload failed: $photoUri", e)
            } finally {
                preloadJobs.remove(photoUri)
            }
        }
        
        preloadJobs[photoUri] = job
    }
    
    /**
     * 为滑动场景预加载
     * 
     * 预加载当前照片和后续 N 张照片。
     * 
     * @param photos 照片列表
     * @param currentIndex 当前索引
     * @param preloadCount 预加载数量（默认 3）
     */
    fun preloadForSwipe(
        photos: List<PhotoEntity>,
        currentIndex: Int,
        preloadCount: Int = 3
    ) {
        if (photos.isEmpty() || currentIndex < 0) return
        
        val endIndex = minOf(currentIndex + preloadCount, photos.size)
        for (i in currentIndex until endIndex) {
            photos.getOrNull(i)?.let { photo ->
                preload(photo.systemUri, ThumbnailSizePolicy.Context.CARD_PREVIEW)
            }
        }
    }
    
    /**
     * 为网格场景预加载
     * 
     * 预加载可见区域及周围的照片。
     * 
     * @param photos 照片列表
     * @param visibleRange 可见索引范围
     * @param columns 网格列数
     * @param extraRows 额外预加载的行数（默认 2）
     */
    fun preloadForGrid(
        photos: List<PhotoEntity>,
        visibleRange: IntRange,
        columns: Int,
        extraRows: Int = 2
    ) {
        if (photos.isEmpty()) return
        
        val sizeContext = ThumbnailSizePolicy.contextForColumns(columns)
        val extraCount = columns * extraRows
        
        val startIndex = maxOf(0, visibleRange.first - extraCount)
        val endIndex = minOf(photos.size, visibleRange.last + extraCount + 1)
        
        for (i in startIndex until endIndex) {
            photos.getOrNull(i)?.let { photo ->
                preload(photo.systemUri, sizeContext)
            }
        }
    }
    
    /**
     * 预加载指定照片列表
     * 
     * @param photos 照片列表
     * @param sizeContext 缩略图尺寸场景
     * @param maxCount 最大预加载数量（默认 10）
     */
    fun preloadPhotos(
        photos: List<PhotoEntity>,
        sizeContext: ThumbnailSizePolicy.Context = ThumbnailSizePolicy.Context.GRID_2_COLUMN,
        maxCount: Int = 10
    ) {
        photos.take(maxCount).forEach { photo ->
            preload(photo.systemUri, sizeContext)
        }
    }
    
    /**
     * 取消指定照片的预加载
     * 
     * @param photoUri 照片 URI
     */
    fun cancel(photoUri: String) {
        preloadJobs[photoUri]?.cancel()
        preloadJobs.remove(photoUri)
    }
    
    /**
     * 取消所有预加载
     * 
     * 应在页面离开时调用。
     */
    fun cancelAll() {
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
    }
    
    /**
     * 清除预加载记录
     * 
     * 清除已预加载的记录，允许重新预加载。
     * 通常在数据刷新后调用。
     */
    fun clearCache() {
        preloadedUris.clear()
    }
    
    /**
     * 重置预加载器
     * 
     * 取消所有预加载并清除记录。
     */
    fun reset() {
        cancelAll()
        clearCache()
    }
    
    /**
     * 获取预加载统计信息（用于调试）
     */
    fun getStats(): PreloadStats = PreloadStats(
        activeJobs = preloadJobs.size,
        completedCount = preloadedUris.size
    )
    
    /**
     * 预加载统计数据
     */
    data class PreloadStats(
        val activeJobs: Int,
        val completedCount: Int
    )
}

// ============== 便捷扩展函数 ==============

/**
 * 从照片列表中预加载首屏照片
 * 
 * @param columns 网格列数
 * @param rows 可见行数
 */
fun PhotoPreloader.preloadFirstScreen(
    photos: List<PhotoEntity>,
    columns: Int = 2,
    rows: Int = 4
) {
    val count = columns * rows
    preloadPhotos(
        photos = photos.take(count),
        sizeContext = ThumbnailSizePolicy.contextForColumns(columns),
        maxCount = count
    )
}

/**
 * 预加载下一张照片（滑动整理场景）
 */
fun PhotoPreloader.preloadNext(photos: List<PhotoEntity>, currentIndex: Int) {
    photos.getOrNull(currentIndex + 1)?.let { photo ->
        preload(photo.systemUri, ThumbnailSizePolicy.Context.CARD_PREVIEW)
    }
}

/**
 * 预加载 Light Table 比较照片
 */
fun PhotoPreloader.preloadForLightTable(photos: List<PhotoEntity>) {
    photos.forEach { photo ->
        preload(photo.systemUri, ThumbnailSizePolicy.Context.THUMBNAIL_MEDIUM)
    }
}
