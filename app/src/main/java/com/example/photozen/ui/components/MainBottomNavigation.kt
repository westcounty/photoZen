package com.example.photozen.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.photozen.navigation.MainDestination
import com.example.photozen.ui.theme.PicZenMotion
import kotlin.math.roundToInt

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
 * ## Enhanced Features
 * - Sliding indicator that follows selected tab
 * - Icon scale animations (1.1f selected, 0.85f pressed)
 * - Overall press scale (0.92f)
 * - Playful spring for indicator, snappy spring for press
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
    val destinations = MainDestination.entries
    val selectedIndex = destinations.indexOf(currentDestination).coerceAtLeast(0)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // Calculate indicator position and width
    val tabWidth = screenWidth / destinations.size
    val indicatorWidth = tabWidth * 0.5f

    // Animated indicator offset - uses playful spring for bouncy feel
    val indicatorOffset by animateDpAsState(
        targetValue = tabWidth * selectedIndex + (tabWidth - indicatorWidth) / 2,
        animationSpec = PicZenMotion.Springs.playful(),
        label = "indicatorOffset"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box {
            // Navigation items row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                destinations.forEach { destination ->
                    val selected = currentDestination == destination

                    EnhancedNavigationItem(
                        selected = selected,
                        onClick = { onDestinationSelected(destination) },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            // Sliding indicator at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                    .width(indicatorWidth)
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Enhanced Navigation Item with press and selection animations
 *
 * ## Animation Specs
 * - Selected: icon scale 1.1f with playful spring
 * - Pressed: icon scale 0.85f, overall scale 0.92f with snappy spring
 */
@Composable
private fun RowScope.EnhancedNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Overall press scale - 0.92f for navigation items
    val overallScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = PicZenMotion.Springs.snappy(),
        label = "navItemScale"
    )

    // Icon scale - 1.1f selected, 0.85f pressed
    val iconScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.85f
            selected -> 1.1f
            else -> 1f
        },
        animationSpec = if (selected && !isPressed)
            PicZenMotion.Springs.playful()
        else
            PicZenMotion.Springs.snappy(),
        label = "navIconScale"
    )

    // Color animation
    val contentColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer {
                scaleX = overallScale
                scaleY = overallScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable ripple, use custom animation
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Icon with scale animation
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            },
            contentAlignment = Alignment.Center
        ) {
            // Apply tint via CompositionLocal would be better, but for simplicity:
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                icon()
            }
        }

        // Label with color
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor
        ) {
            label()
        }
    }
}
