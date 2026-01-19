package com.example.photozen.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航栏的目的地定义
 * 
 * PhotoZen 主导航系统，定义了底部导航栏的 4 个 Tab 页面。
 * 
 * ## 设计原则
 * 
 * 1. **图标切换**: 未选中使用 Outlined 图标，选中使用 Filled 图标
 * 2. **路由规范**: 主 Tab 路由以 `main_` 前缀区分
 * 3. **状态保持**: 支持 Tab 切换时保持页面状态
 * 
 * ## 使用方式
 * 
 * ```kotlin
 * // 遍历所有目的地
 * MainDestination.entries.forEach { destination ->
 *     NavigationBarItem(
 *         selected = current == destination,
 *         icon = { Icon(if (selected) destination.selectedIcon else destination.icon) },
 *         label = { Text(destination.label) }
 *     )
 * }
 * 
 * // 判断是否显示底部导航
 * val showBottomNav = MainDestination.shouldShowBottomNav(currentRoute)
 * ```
 * 
 * @property route 导航路由
 * @property icon 未选中状态图标 (Outlined)
 * @property selectedIcon 选中状态图标 (Filled)
 * @property label 显示标签
 * 
 * @since Phase 1-A (骨架) → Phase 1-C (启用)
 */
sealed class MainDestination(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
) {
    /**
     * 首页 Tab
     * 
     * 显示待整理照片数量、快捷入口、每日任务等
     */
    data object Home : MainDestination(
        route = "main_home",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        label = "首页"
    )
    
    /**
     * 时间线 Tab
     * 
     * 按时间分组显示所有照片，支持事件分组浏览
     */
    data object Timeline : MainDestination(
        route = "main_timeline",
        icon = Icons.Outlined.Timeline,
        selectedIcon = Icons.Filled.Timeline,
        label = "时间线"
    )
    
    /**
     * 相册 Tab
     * 
     * 显示相册气泡图，支持进入相册详情
     */
    data object Albums : MainDestination(
        route = "main_albums",
        icon = Icons.Outlined.Collections,
        selectedIcon = Icons.Filled.Collections,
        label = "相册"
    )
    
    /**
     * 设置 Tab
     * 
     * 应用设置、偏好配置、关于信息等
     */
    data object Settings : MainDestination(
        route = "main_settings",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        label = "设置"
    )
    
    companion object {
        /**
         * 所有导航目的地列表
         * 
         * 按照底部导航栏从左到右的顺序排列
         */
        val entries = listOf(Home, Timeline, Albums, Settings)
        
        /**
         * 需要隐藏底部导航的路由前缀
         * 
         * 这些页面需要全屏沉浸式体验，不显示底部导航栏：
         * - 整理流程页面 (FlowSorter, Workflow)
         * - 编辑器页面 (PhotoEditor)
         * - 对比页面 (LightTable)
         * - 外部分享页面 (ShareCopy, ShareCompare)
         * - 筛选配置页面 (PhotoFilterSelection)
         * - 照片列表（从首页进入的状态列表）
         * - 相册照片列表
         * - 回收站
         * - 成就页面
         * - Smart Gallery 相关页面
         */
        private val fullscreenRoutes = listOf(
            // 整理流程
            "FlowSorter",
            "Workflow",
            // 编辑器
            "PhotoEditor",
            // 对比模式
            "LightTable",
            // 分享页面
            "ShareCopy",
            "ShareCompare",
            // 筛选选择
            "PhotoFilterSelection",
            // 照片列表（从首页进入的状态列表）
            "PhotoList",
            // 相册照片列表
            "AlbumPhotoList",
            // 回收站
            "Trash",
            // 成就
            "Achievements",
            // Smart Gallery 相关
            "SmartGallery",
            "LabelBrowser",
            "LabelPhotos",
            "PersonList",
            "PersonDetail",
            "SmartSearch",
            "SimilarPhotos",
            "MapView"
        )
        
        /**
         * 判断指定路由是否需要显示底部导航
         * 
         * @param route 当前路由，可为 null
         * @return true 表示显示底部导航，false 表示隐藏
         * 
         * 规则：
         * 1. 如果路由为 null，返回 true（默认显示）
         * 2. 如果路由是主 Tab 路由，返回 true
         * 3. 如果路由以 fullscreenRoutes 中任一前缀开头，返回 false
         * 4. 其他情况返回 true（兼容旧路由）
         */
        fun shouldShowBottomNav(route: String?): Boolean {
            if (route == null) return true
            // 主 Tab 路由显示底部导航
            if (entries.any { it.route == route }) return true
            // 全屏路由隐藏底部导航
            return fullscreenRoutes.none { route.startsWith(it) }
        }
        
        /**
         * 根据路由查找对应的目的地
         * 
         * @param route 导航路由
         * @return 匹配的 MainDestination，未找到返回 null
         */
        fun fromRoute(route: String?): MainDestination? {
            return entries.find { it.route == route }
        }
    }
}
