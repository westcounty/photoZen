package com.example.photozen.domain.model

/**
 * 引导点位枚举
 * 
 * 定义应用中所有需要引导的功能点。
 * 每个枚举值对应一个独立的引导状态。
 * 
 * ## 命名规范
 * - 页面名_功能名 格式
 * - 使用大写下划线命名
 * 
 * ## 扩展说明
 * 新增引导点位只需添加枚举值，无需修改存储逻辑。
 */
enum class GuideKey(
    val description: String,
    val priority: Int = 0  // 优先级，数字越小优先级越高
) {
    // ==================== FlowSorter 滑动引导 ====================
    /** 右滑保留引导 - 滑动整理第 1 步 */
    SWIPE_RIGHT("滑动整理 - 右滑保留引导", priority = 0),
    
    /** 左滑删除引导 - 滑动整理第 2 步 */
    SWIPE_LEFT("滑动整理 - 左滑删除引导", priority = 1),
    
    /** 上滑待定引导 - 滑动整理第 3 步 */
    SWIPE_UP("滑动整理 - 上滑待定引导", priority = 2),
    
    // ==================== 照片列表引导 ====================
    /** 长按多选引导 */
    PHOTO_LIST_LONG_PRESS("照片列表 - 长按多选引导", priority = 10),
    
    // ==================== 首页引导 ====================
    /** 开始整理按钮引导 */
    HOME_START_BUTTON("首页 - 开始整理按钮引导", priority = 20),
    
    // ==================== 选择模式引导 ====================
    /** 全选按钮引导 */
    SELECTION_SELECT_ALL("选择模式 - 全选按钮引导", priority = 30),
    
    // ==================== 筛选面板引导 (Phase 2-C) ====================
    /** 筛选面板引导 */
    FILTER_PANEL("筛选面板引导", priority = 40);
    
    companion object {
        /**
         * 获取 FlowSorter 滑动引导序列
         * 按优先级排序
         */
        val flowSorterSequence: List<GuideKey> = listOf(
            SWIPE_RIGHT,
            SWIPE_LEFT,
            SWIPE_UP
        )
        
        /**
         * 安全地从字符串解析 GuideKey
         */
        fun fromString(name: String): GuideKey? = try {
            valueOf(name)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
