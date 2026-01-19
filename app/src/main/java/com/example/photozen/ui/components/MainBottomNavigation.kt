package com.example.photozen.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.photozen.navigation.MainDestination

/**
 * 底部导航栏组件
 * 
 * PhotoZen 主导航的底部 Tab 栏，提供首页、时间线、相册、设置四个入口。
 * 
 * ## 设计规范
 * 
 * - Tab 数量：4 个
 * - 图标状态：未选中 Outlined，选中 Filled
 * - 颜色：遵循 Material 3 NavigationBar 默认样式
 * 
 * ## 使用方式
 * 
 * ```kotlin
 * var currentDestination by remember { mutableStateOf(MainDestination.Home) }
 * 
 * MainBottomNavigation(
 *     currentDestination = currentDestination,
 *     onDestinationSelected = { destination ->
 *         currentDestination = destination
 *         navController.navigate(destination.route)
 *     }
 * )
 * ```
 * 
 * @param currentDestination 当前选中的目的地
 * @param onDestinationSelected Tab 切换回调，参数为选中的目的地
 * @param modifier Modifier
 * 
 * @since Phase 1-A (骨架) → Phase 1-C (启用)
 */
@Composable
fun MainBottomNavigation(
    currentDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        MainDestination.entries.forEach { destination ->
            val selected = currentDestination == destination
            
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}
