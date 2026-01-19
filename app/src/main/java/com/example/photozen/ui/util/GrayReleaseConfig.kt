package com.example.photozen.ui.util

import kotlin.math.absoluteValue

/**
 * 灰度发布配置
 * 
 * 控制新功能的渐进式发布，通过灰度比例决定用户是否看到新版本。
 * 
 * ## 使用方式
 * 
 * ```kotlin
 * // 判断是否使用新首页布局
 * val useNewLayout = GrayReleaseConfig.shouldUseNewHomeLayout(userId)
 * 
 * // 设置灰度比例（10% 用户看到新布局）
 * GrayReleaseConfig.newHomeLayoutRatio = 0.1f
 * ```
 * 
 * ## 灰度规则
 * 
 * - 基于用户ID的哈希值确定，确保同一用户始终看到相同的版本
 * - FeatureFlags 优先级高于灰度比例（强制开启时直接使用新布局）
 * 
 * ## 监控指标
 * 
 * | 指标 | 阈值 | 说明 |
 * |:-----|:-----|:-----|
 * | 整理启动率 | 不下降 | 新布局的整理功能入口点击率 |
 * | 每日任务完成率 | 不下降 | 每日任务完成人数/开启人数 |
 * | 首页停留时长 | 观察 | 用户在首页的平均停留时间 |
 * | 崩溃率 | < 0.1% | 新布局相关崩溃 |
 * 
 * @since Phase 1-D
 */
object GrayReleaseConfig {
    
    /**
     * 新首页布局灰度比例 (0.0 - 1.0)
     * 
     * - 0.0 = 全部使用旧布局
     * - 0.5 = 50% 用户使用新布局
     * - 1.0 = 全部使用新布局
     * 
     * 默认值为 0.0f，表示不启用灰度，完全依赖 FeatureFlags
     */
    var newHomeLayoutRatio = 0.0f
    
    /**
     * 判断是否应该使用新首页布局
     * 
     * 判断逻辑：
     * 1. 如果 FeatureFlags.USE_NEW_HOME_LAYOUT = true，直接使用新布局
     * 2. 如果灰度比例 <= 0，不使用新布局
     * 3. 如果灰度比例 >= 1，使用新布局
     * 4. 否则，基于用户ID哈希值判断
     * 
     * @param userId 用户标识符，用于计算哈希确保一致性
     * @return true 使用新首页布局，false 使用旧布局
     */
    fun shouldUseNewHomeLayout(userId: String): Boolean {
        // FeatureFlags 强制开启时，直接使用新布局
        if (FeatureFlags.USE_NEW_HOME_LAYOUT) {
            return true
        }
        
        // 边界检查
        if (newHomeLayoutRatio <= 0f) return false
        if (newHomeLayoutRatio >= 1f) return true
        
        // 基于用户ID哈希判断
        val hash = userId.hashCode().absoluteValue
        return (hash % 100) < (newHomeLayoutRatio * 100)
    }
    
    /**
     * 重置灰度配置
     * 
     * 主要用于测试环境
     */
    fun reset() {
        newHomeLayoutRatio = 0.0f
    }
    
    /**
     * 获取当前灰度配置描述
     * 
     * @return 格式化的配置字符串，用于调试日志
     */
    fun getConfigDescription(): String {
        return buildString {
            appendLine("GrayReleaseConfig Status:")
            appendLine("  newHomeLayoutRatio = ${(newHomeLayoutRatio * 100).toInt()}%")
            appendLine("  FeatureFlags.USE_NEW_HOME_LAYOUT = ${FeatureFlags.USE_NEW_HOME_LAYOUT}")
        }
    }
}
