package com.example.photozen.ui.util

/**
 * Feature Flags for gradual rollout of new features.
 * 
 * PhotoZen 功能开关框架，用于控制新功能的渐进式发布。
 * 
 * ## 使用方式
 * 
 * ```kotlin
 * if (FeatureFlags.USE_BOTTOM_NAV) {
 *     // 显示底部导航
 *     MainScaffold(navController) { ... }
 * } else {
 *     // 使用旧布局
 *     PicZenNavHost(navController)
 * }
 * ```
 * 
 * ## Phase 1 功能开关
 * 
 * | Flag | Phase | 说明 |
 * |:-----|:------|:-----|
 * | USE_UNIFIED_GESTURE | 1-B | 手势统一 - 所有列表页面使用统一手势 |
 * | USE_BOTTOM_NAV | 1-C | 底部导航 - 启用 4-Tab 底部导航栏 |
 * | USE_NEW_HOME_LAYOUT | 1-D | 新首页布局 - 使用重构后的首页 |
 * 
 * ## 开发指南
 * 
 * 1. 新增 Feature Flag 时，默认值应为 `false`
 * 2. 在测试环境中可以通过 `FeatureFlags.xxx = true` 启用
 * 3. 功能稳定后，移除 Feature Flag 判断，直接使用新代码
 * 4. 使用 `resetAll()` 在测试中重置所有 Flag
 * 
 * @since Phase 1-A
 */
object FeatureFlags {
    
    // ==================== Phase 1-B: 手势统一 ====================
    
    /**
     * 启用统一手势交互
     * 
     * 启用后：
     * - AlbumPhotoListScreen 使用 DragSelectPhotoGrid
     * - TimelineScreen 在事件组内使用统一手势
     * - 所有列表页面的点击/长按/拖动行为一致
     * 
     * @since Phase 1-B
     */
    var USE_UNIFIED_GESTURE = false
    
    // ==================== Phase 1-C: 底部导航 ====================
    
    /**
     * 启用底部导航栏
     * 
     * 启用后：
     * - MainActivity 使用 MainScaffold 包装 NavHost
     * - 显示 4-Tab 底部导航（首页/时间线/相册/设置）
     * - 全屏页面（整理流程/编辑器等）自动隐藏底部导航
     * 
     * @since Phase 1-C
     */
    var USE_BOTTOM_NAV = false
    
    // ==================== Phase 1-D: 新首页布局 ====================
    
    /**
     * 启用新首页布局
     * 
     * 启用后：
     * - 使用 HomeMainAction 替代原有的多卡片布局
     * - 合并"快速整理"和"一站式整理"入口
     * - 每日任务卡片可折叠
     * - 移除时间线/相册入口（由底部导航提供）
     * 
     * 依赖：USE_BOTTOM_NAV = true
     * 
     * @since Phase 1-D
     */
    var USE_NEW_HOME_LAYOUT = false
    
    // ==================== 调试方法 ====================
    
    /**
     * 重置所有 Feature Flag 为默认值 (false)
     * 
     * 主要用于：
     * - 单元测试的 @Before 方法
     * - 调试时快速重置状态
     */
    fun resetAll() {
        USE_UNIFIED_GESTURE = false
        USE_BOTTOM_NAV = false
        USE_NEW_HOME_LAYOUT = false
    }
    
    /**
     * 启用所有 Phase 1 功能
     * 
     * 仅用于测试环境，一次性启用所有 Phase 1 新功能
     */
    fun enableAllPhase1() {
        USE_UNIFIED_GESTURE = true
        USE_BOTTOM_NAV = true
        USE_NEW_HOME_LAYOUT = true
    }
    
    /**
     * 获取当前所有 Flag 状态的描述
     * 
     * @return 格式化的状态字符串，用于调试日志
     */
    fun getStatusDescription(): String {
        return buildString {
            appendLine("FeatureFlags Status:")
            appendLine("  USE_UNIFIED_GESTURE = $USE_UNIFIED_GESTURE")
            appendLine("  USE_BOTTOM_NAV = $USE_BOTTOM_NAV")
            appendLine("  USE_NEW_HOME_LAYOUT = $USE_NEW_HOME_LAYOUT")
        }
    }
}
