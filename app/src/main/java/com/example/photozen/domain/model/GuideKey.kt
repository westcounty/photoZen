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
    // ==================== 照片列表引导 ====================
    /** 长按多选引导 */
    PHOTO_LIST_LONG_PRESS("照片列表 - 长按多选引导", priority = 10),

    // ==================== 首页引导 ====================
    /** 开始整理按钮引导 */
    HOME_START_BUTTON("首页 - 开始整理按钮引导", priority = 20),

    // ==================== FlowSorter 视图切换引导 ====================
    /** 视图切换引导 - 告知用户可以切换卡片/列表视图 */
    FLOW_SORTER_VIEW_TOGGLE("滑动整理 - 视图切换引导", priority = 25),

    // ==================== 时间线引导 ====================
    /** 时间线分组模式引导 */
    TIMELINE_GROUPING("时间线 - 分组模式引导", priority = 35),

    // ==================== 筛选面板引导 ====================
    /** 筛选面板引导 */
    FILTER_PANEL("筛选面板引导", priority = 40),

    // ==================== 统计页面引导 ====================
    /** 统计页日历热力图引导 */
    STATS_CALENDAR("统计 - 日历热力图引导", priority = 45),

    // ==================== 分享功能提示 ====================
    /** 分享功能提示 - 首页卡片 (已废弃，改用渐进式提示) */
    @Deprecated("Use HOME_TIP_COMPARE, HOME_TIP_COPY, HOME_TIP_WIDGET instead")
    SHARE_FEATURE_TIP("首页 - 分享功能提示", priority = 50),

    // ==================== 首页渐进式引导 ====================
    /** 首页提示 - 对比功能：在相册中调用图禅便捷对比图片 */
    HOME_TIP_COMPARE("首页提示 - 对比功能", priority = 51),

    /** 首页提示 - 复制功能：在相册中调用图禅进行照片复制 */
    HOME_TIP_COPY("首页提示 - 复制功能", priority = 52),

    /** 首页提示 - 桌面小部件：添加桌面小部件，每日任务不错过 */
    HOME_TIP_WIDGET("首页提示 - 桌面小部件", priority = 53),

    // ==================== 新手引导 (REQ-067) ====================
    /** 双指缩放引导 - 照片网格列数切换 */
    PINCH_ZOOM_GUIDE("照片网格 - 双指缩放切换列数引导", priority = 5),

    /** 滑动筛选全屏引导 - 快速筛选滑动手势 */
    SWIPE_SORT_FULLSCREEN_GUIDE("快速筛选 - 滑动手势全屏引导", priority = 3);
    
    companion object {
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
