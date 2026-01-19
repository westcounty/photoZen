package com.example.photozen.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 筛选预设
 * 
 * 用户保存的筛选条件组合，可快速应用。
 * 
 * @property id 唯一标识符
 * @property name 预设名称（用户可见）
 * @property config 筛选配置
 * @property createdAt 创建时间戳
 */
@Serializable
data class FilterPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val config: FilterConfig,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取预设描述
     */
    fun getDescription(): String {
        val parts = mutableListOf<String>()
        
        config.albumIds?.let { ids ->
            if (ids.isNotEmpty()) {
                parts.add("${ids.size} 个相册")
            }
        }
        
        if (config.startDate != null || config.endDate != null) {
            parts.add("指定日期")
        }
        
        return parts.joinToString(", ").ifEmpty { "无条件" }
    }
    
    companion object {
        /**
         * 预设名称最大长度
         */
        const val MAX_NAME_LENGTH = 10
        
        /**
         * 最大预设数量
         */
        const val MAX_PRESETS = 3
        
        /**
         * 创建预设
         */
        fun create(name: String, config: FilterConfig): FilterPreset {
            val trimmedName = name.take(MAX_NAME_LENGTH).trim()
            require(trimmedName.isNotEmpty()) { "预设名称不能为空" }
            require(!config.isEmpty) { "不能保存空的筛选条件" }
            
            return FilterPreset(
                name = trimmedName,
                config = config
            )
        }
    }
}
