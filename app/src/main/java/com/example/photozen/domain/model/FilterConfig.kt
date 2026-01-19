package com.example.photozen.domain.model

import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import kotlinx.serialization.Serializable

/**
 * 筛选配置
 * 
 * 定义照片筛选的各种条件，所有字段均为可选。
 * null 表示不限制该条件。
 * 
 * ## 字段说明
 * - albumIds: 要包含的相册 ID 列表（null=全部相册）
 * - startDate: 开始日期时间戳（null=不限开始）
 * - endDate: 结束日期时间戳（null=不限结束）
 * 
 * ## 使用示例
 * ```kotlin
 * // 筛选相机相册中 2024 年 1 月的照片
 * val config = FilterConfig(
 *     albumIds = listOf("camera_bucket_id"),
 *     startDate = 1704067200000, // 2024-01-01
 *     endDate = 1706745599000    // 2024-01-31 23:59:59
 * )
 * ```
 */
@Serializable
data class FilterConfig(
    val albumIds: List<String>? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
) {
    /**
     * 筛选配置是否为空（无任何条件）
     */
    val isEmpty: Boolean
        get() = albumIds.isNullOrEmpty() && 
                startDate == null && 
                endDate == null
    
    /**
     * 当前激活的筛选条件数量
     */
    val activeFilterCount: Int
        get() {
            var count = 0
            if (!albumIds.isNullOrEmpty()) count++
            if (startDate != null || endDate != null) count++
            return count
        }
    
    /**
     * 是否有相册筛选
     */
    val hasAlbumFilter: Boolean
        get() = !albumIds.isNullOrEmpty()
    
    /**
     * 是否有日期筛选
     */
    val hasDateFilter: Boolean
        get() = startDate != null || endDate != null
    
    /**
     * 清除相册筛选
     */
    fun clearAlbumFilter(): FilterConfig = copy(albumIds = null)
    
    /**
     * 清除日期筛选
     */
    fun clearDateFilter(): FilterConfig = copy(startDate = null, endDate = null)
    
    /**
     * 检查照片是否匹配筛选条件
     */
    fun matches(photo: PhotoEntity): Boolean {
        // 相册检查
        if (!albumIds.isNullOrEmpty() && photo.bucketId !in albumIds) {
            return false
        }
        
        // 日期检查
        if (startDate != null && photo.dateTaken < startDate) {
            return false
        }
        if (endDate != null && photo.dateTaken > endDate) {
            return false
        }
        
        return true
    }
    
    companion object {
        /**
         * 空筛选配置（无任何条件）
         */
        val EMPTY = FilterConfig()
    }
}

/**
 * 筛选类型
 */
enum class FilterType {
    ALBUM,   // 相册筛选
    DATE     // 日期筛选
}
