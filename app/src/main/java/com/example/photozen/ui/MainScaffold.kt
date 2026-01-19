package com.example.photozen.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.photozen.navigation.MainDestination
import com.example.photozen.ui.components.MainBottomNavigation
import com.example.photozen.ui.components.StyledSnackbar
import com.example.photozen.ui.state.SnackbarEvent
import com.example.photozen.ui.state.SnackbarManager

/**
 * 主框架组件 - 包含底部导航栏和全局 Snackbar
 * 
 * PhotoZen 的主 UI 框架，负责：
 * 1. 显示/隐藏底部导航栏（基于当前路由判断）
 * 2. 处理 Tab 切换导航
 * 3. 保持 Tab 页面状态
 * 4. Phase 3-8: 全局 Snackbar 管理
 * 
 * ## 设计原则
 * 
 * - **状态保持**: 使用 `saveState`/`restoreState` 保持各 Tab 页面状态
 * - **智能隐藏**: 全屏页面（整理流程/编辑器等）自动隐藏底部导航
 * - **动画过渡**: 底部导航显示/隐藏使用滑动动画
 * - **全局 Snackbar**: 统一管理所有页面的 Snackbar 显示
 * 
 * ## 使用方式
 * 
 * ```kotlin
 * // 在 MainActivity 中使用
 * MainScaffold(
 *     navController = navController,
 *     snackbarManager = snackbarManager
 * ) { paddingValues ->
 *     PicZenNavHost(
 *         navController = navController,
 *         modifier = Modifier.padding(paddingValues)
 *     )
 * }
 * 
 * // 在 ViewModel 中使用 SnackbarManager
 * snackbarManager.showSuccess("操作成功")
 * snackbarManager.showError("操作失败")
 * snackbarManager.showWithUndo("已删除") { undo() }
 * ```
 * 
 * ## 隐藏场景
 * 
 * 以下路由前缀的页面会隐藏底部导航：
 * - FlowSorter（滑动整理）
 * - Workflow（一站式整理）
 * - PhotoEditor（照片编辑）
 * - LightTable（照片对比）
 * - ShareCopy/ShareCompare（外部分享）
 * - PhotoFilterSelection（筛选配置）
 * 
 * @param navController 导航控制器
 * @param snackbarManager 全局 Snackbar 管理器 (Phase 3-8)
 * @param modifier Modifier
 * @param content 内容区域，接收 PaddingValues 用于处理底部导航栏的间距
 * 
 * @since Phase 1-A (骨架) → Phase 1-C (启用) → Phase 3-8 (全局 Snackbar)
 */
@Composable
fun MainScaffold(
    navController: NavHostController,
    snackbarManager: SnackbarManager,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    // Phase 3-8: 全局 SnackbarHost 状态
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 当前选中的 Tab 路由，使用 rememberSaveable 在配置变更后保持
    // 存储路由字符串而非 sealed class 对象，以支持状态保存
    var selectedTabRoute by rememberSaveable { 
        mutableStateOf(MainDestination.Home.route) 
    }
    
    // Phase 3-8: 监听 SnackbarManager 事件并显示 Snackbar
    var currentSnackbarEvent by remember { mutableStateOf<SnackbarEvent?>(null) }
    
    LaunchedEffect(Unit) {
        snackbarManager.events.collect { event ->
            currentSnackbarEvent = event
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = when (event) {
                    is SnackbarEvent.WithAction -> event.actionLabel
                    is SnackbarEvent.SuccessWithAction -> event.actionLabel
                    else -> null
                },
                duration = event.duration
            )
            
            // 处理操作按钮点击
            if (result == SnackbarResult.ActionPerformed) {
                when (event) {
                    is SnackbarEvent.WithAction -> event.onAction()
                    is SnackbarEvent.SuccessWithAction -> event.onAction()
                    else -> {}
                }
            }
            currentSnackbarEvent = null
        }
    }
    
    // 根据路由获取当前选中的目的地
    val currentDestination = MainDestination.fromRoute(selectedTabRoute) ?: MainDestination.Home
    
    // 监听导航变化，获取当前路由（用于判断是否显示底部导航）
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavRoute = navBackStackEntry?.destination?.route
    
    // 同步：当导航到主 Tab 路由时，更新选中状态
    // 这确保了通过其他方式（如深链接）导航到 Tab 页面时，底部导航也能正确高亮
    LaunchedEffect(currentNavRoute) {
        MainDestination.fromRoute(currentNavRoute)?.let {
            selectedTabRoute = it.route
        }
    }
    
    // 判断是否显示底部导航
    val showBottomNav = MainDestination.shouldShowBottomNav(currentNavRoute)
    
    // 处理返回键：在非首页 Tab 按返回键时，切换到首页
    // 首页按返回键时，由系统处理（退出应用）
    val isOnMainTab = MainDestination.entries.any { it.route == currentNavRoute }
    val isNotOnHome = currentNavRoute != MainDestination.Home.route
    
    BackHandler(enabled = isOnMainTab && isNotOnHome) {
        // 在非首页主 Tab 上按返回键，切换到首页
        selectedTabRoute = MainDestination.Home.route
        navController.navigate(MainDestination.Home.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    
    Scaffold(
        modifier = modifier,
        // Phase 3-8: 全局 SnackbarHost 带样式
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                StyledSnackbar(
                    snackbarData = data,
                    snackbarEvent = currentSnackbarEvent
                )
            }
        },
        bottomBar = {
            // 底部导航栏，带动画显示/隐藏
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically { it },  // 从底部滑入
                exit = slideOutVertically { it }   // 向底部滑出
            ) {
                MainBottomNavigation(
                    currentDestination = currentDestination,
                    onDestinationSelected = { destination ->
                        // 更新当前选中的 Tab 路由
                        selectedTabRoute = destination.route
                        
                        // 执行导航
                        navController.navigate(destination.route) {
                            // 弹出到起始目的地，避免堆积过多返回栈
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // 避免重复创建相同目的地
                            launchSingleTop = true
                            // 恢复之前保存的状态
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

/**
 * MainScaffold 的状态持有类
 * 
 * 用于在更复杂的场景下管理 MainScaffold 的状态。
 * 
 * @property currentDestination 当前选中的目的地
 * @property showBottomNav 是否显示底部导航
 */
data class MainScaffoldState(
    val currentDestination: MainDestination = MainDestination.Home,
    val showBottomNav: Boolean = true
)
