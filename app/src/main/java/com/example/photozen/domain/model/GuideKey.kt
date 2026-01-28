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
    // ==================== FlowSorter 引导 ====================
    /** 滑动筛选全屏引导 - 快速筛选滑动手势 */
    SWIPE_SORT_FULLSCREEN_GUIDE("快速筛选 - 滑动手势全屏引导", priority = 3),

    /** 视图切换引导 - 告知用户可以切换卡片/列表视图 */
    FLOW_SORTER_VIEW_TOGGLE("滑动整理 - 视图切换引导", priority = 25),

    // ==================== 统计页面引导 ====================
    /** 统计页日历热力图引导 */
    STATS_CALENDAR("统计 - 日历热力图引导", priority = 45),

    // ==================== 首页渐进式引导 ====================
    /** 首页提示 - 对比功能：在相册中调用图禅便捷对比图片 */
    HOME_TIP_COMPARE("首页提示 - 对比功能", priority = 51),

    /** 首页提示 - 复制功能：在相册中调用图禅进行照片复制 */
    HOME_TIP_COPY("首页提示 - 复制功能", priority = 52),

    /** 首页提示 - 桌面小部件：添加桌面小部件，每日任务不错过 */
    HOME_TIP_WIDGET("首页提示 - 桌面小部件", priority = 53),

    /** 首页提示 - 时光拾遗：添加时光拾遗小部件，重温尘封记忆 */
    HOME_TIP_MEMORY_LANE("首页提示 - 时光拾遗", priority = 54);

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
