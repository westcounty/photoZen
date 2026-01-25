package com.example.photozen.ui.screens.flowsorter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.ui.theme.PicZenMotion
import com.example.photozen.ui.util.HapticFeedbackManager
import com.example.photozen.ui.util.SwipeHapticDirection
import com.example.photozen.ui.util.rememberHapticFeedbackManager
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Base threshold for triggering a swipe action (as fraction of screen width/height)
 * This is the default when sensitivity = 1.0
 */
private const val BASE_SWIPE_THRESHOLD = 0.20f

/**
 * Direction-specific threshold multipliers:
 * - Right (Keep): 1.0 (baseline)
 * - Left (Keep): 0.9 (slightly easier)
 * - Up (Trash): 0.8 (easier, as it's destructive)
 * - Down (Maybe): 0.7 (easiest, as it's non-destructive)
 */
private const val THRESHOLD_MULTIPLIER_RIGHT = 1.0f
private const val THRESHOLD_MULTIPLIER_LEFT = 0.9f
private const val THRESHOLD_MULTIPLIER_UP = 0.8f
private const val THRESHOLD_MULTIPLIER_DOWN = 0.7f

/**
 * Rotation multiplier for card tilt during horizontal swipe
 * DES-034: 增强倾斜透视效果，更有 3D 感
 * 增大到 38f 以获得更明显的倾斜感
 */
private const val ROTATION_MULTIPLIER = 38f

/**
 * Swipeable photo card with smooth animations.
 * 
 * @param photo The photo to display
 * @param isTopCard Whether this card is the top card (receives gestures)
 * @param swipeSensitivity Sensitivity setting (0.5 = very sensitive, 1.5 = less sensitive)
 * @param onSwipeLeft Called when swiped left (Keep)
 * @param onSwipeRight Called when swiped right (Keep)
 * @param onSwipeUp Called when swiped up (Trash)
 * @param onSwipeDown Called when swiped down (Maybe)
 * @param onPhotoClick Called when photo is clicked
 * @param showInfoOnImage When true, photo info is displayed on the image itself
 * @param modifier Modifier for the card
 */
@Composable
fun SwipeablePhotoCard(
    photo: PhotoEntity,
    isTopCard: Boolean = true,
    swipeSensitivity: Float = 1.0f,
    hapticFeedbackEnabled: Boolean = true,  // Phase 3-7: 震动反馈开关
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit = {},
    onPhotoClick: (() -> Unit)? = null,
    showInfoOnImage: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // Phase 3-7: 使用设置中的震动反馈开关
    val hapticManager = rememberHapticFeedbackManager(hapticFeedbackEnabled)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Screen dimensions for threshold calculation
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Calculate direction-specific thresholds based on sensitivity
    // Higher sensitivity value = larger threshold = harder to trigger
    val baseThreshold = BASE_SWIPE_THRESHOLD * swipeSensitivity
    val thresholdRight = screenWidthPx * baseThreshold * THRESHOLD_MULTIPLIER_RIGHT
    val thresholdLeft = screenWidthPx * baseThreshold * THRESHOLD_MULTIPLIER_LEFT
    val thresholdUp = screenWidthPx * baseThreshold * THRESHOLD_MULTIPLIER_UP
    val thresholdDown = screenWidthPx * baseThreshold * THRESHOLD_MULTIPLIER_DOWN
    
    // Use rememberUpdatedState to ensure lambda captures latest threshold values
    // This is critical when swipeSensitivity changes while the card is displayed
    val currentThresholdRight by rememberUpdatedState(thresholdRight)
    val currentThresholdLeft by rememberUpdatedState(thresholdLeft)
    val currentThresholdUp by rememberUpdatedState(thresholdUp)
    val currentThresholdDown by rememberUpdatedState(thresholdDown)
    
    // CRITICAL: Use rememberUpdatedState for isTopCard so that when the card
    // becomes the top card (after previous card is swiped), the gesture handler
    // will use the updated value instead of the captured old value
    val currentIsTopCard by rememberUpdatedState(isTopCard)
    
    // Animatable offsets for smooth animations
    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }
    
    // Prevent multiple swipe callbacks
    var hasTriggeredSwipe by remember(photo.id) { mutableStateOf(false) }
    
    // Track current swipe direction for visual feedback
    var currentDirection by remember(photo.id) { mutableStateOf(SwipeDirection.NONE) }
    
    // Track whether threshold has been reached for each direction (for haptic and visual feedback)
    var hasReachedThresholdRight by remember(photo.id) { mutableStateOf(false) }
    var hasReachedThresholdLeft by remember(photo.id) { mutableStateOf(false) }
    var hasReachedThresholdUp by remember(photo.id) { mutableStateOf(false) }
    var hasReachedThresholdDown by remember(photo.id) { mutableStateOf(false) }
    
    // Calculate normalized progress (-1 to 1 for X and Y)
    val progressX = (offsetX.value / screenWidthPx).coerceIn(-1f, 1f)
    val progressY = (offsetY.value / screenHeightPx).coerceIn(-1f, 1f)
    
    // Update direction based on offset
    currentDirection = when {
        abs(offsetY.value) > abs(offsetX.value) && offsetY.value < -50f -> SwipeDirection.UP
        abs(offsetY.value) > abs(offsetX.value) && offsetY.value > 50f -> SwipeDirection.DOWN
        offsetX.value > 50f -> SwipeDirection.RIGHT
        offsetX.value < -50f -> SwipeDirection.LEFT
        else -> SwipeDirection.NONE
    }
    
    // Check if current position has reached threshold
    // Use currentThreshold* (from rememberUpdatedState) to always use latest values
    val currentlyReachedRight = offsetX.value > currentThresholdRight
    val currentlyReachedLeft = offsetX.value < -currentThresholdLeft
    val currentlyReachedUp = offsetY.value < -currentThresholdUp && abs(offsetY.value) > abs(offsetX.value)
    val currentlyReachedDown = offsetY.value > currentThresholdDown && abs(offsetY.value) > abs(offsetX.value)
    
    // Determine overall threshold state for visual feedback
    val hasReachedThreshold = currentlyReachedRight || currentlyReachedLeft || currentlyReachedUp || currentlyReachedDown
    
    // Calculate rotation based on horizontal offset (DES-019: 倾斜透视效果)
    val rotation = (offsetX.value / screenWidthPx) * ROTATION_MULTIPLIER

    // DES-021/DES-034: 倾斜透视效果增强 - 基于垂直偏移的X轴旋转
    val rotationXValue = -(offsetY.value / screenHeightPx) * 12f

    // Scale effect during swipe
    val scale = when {
        offsetY.value > 0 -> 1f - (offsetY.value / screenHeightPx) * 0.15f
        else -> 1f + (abs(offsetX.value) / screenWidthPx) * 0.03f
    }

    // Alpha for sinking effect
    val alpha = if (offsetY.value > 0) {
        1f - (offsetY.value / screenHeightPx) * 0.5f
    } else 1f

    // DES-020: 动态阴影高度 - 基于滑动进度（简化：去掉阴影以获得更干净的视觉效果）
    val dynamicElevation = 0f // 去掉阴影
    
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                rotationZ = rotation
                rotationX = rotationXValue  // DES-021: 垂直倾斜透视
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                shadowElevation = dynamicElevation  // DES-020: 动态阴影
                cameraDistance = 8f * density.density  // DES-034: 更近的透视距离增强 3D 效果
            }
            .pointerInput(photo.id, swipeSensitivity) {
                detectDragGestures(
                    onDragStart = {
                        if (!currentIsTopCard || hasTriggeredSwipe) return@detectDragGestures
                        // Phase 3-7: 使用 HapticFeedbackManager 的拖动开始反馈
                        hapticManager.performDragStart()
                        // Reset threshold states
                        hasReachedThresholdRight = false
                        hasReachedThresholdLeft = false
                        hasReachedThresholdUp = false
                        hasReachedThresholdDown = false
                    },
                    onDragEnd = {
                        if (!currentIsTopCard || hasTriggeredSwipe) return@detectDragGestures
                        
                        // Calculate thresholds in real-time to avoid closure capture issues
                        val reachedRight = offsetX.value > currentThresholdRight
                        val reachedLeft = offsetX.value < -currentThresholdLeft
                        val reachedUp = offsetY.value < -currentThresholdUp && abs(offsetY.value) > abs(offsetX.value)
                        val reachedDown = offsetY.value > currentThresholdDown && abs(offsetY.value) > abs(offsetX.value)
                        
                        when {
                            // Swipe up → Trash
                            // CRITICAL: Call callback AFTER animation completes to prevent flash
                            reachedUp -> {
                                hasTriggeredSwipe = true
                                // Phase 3-7: 使用方向感知的操作完成反馈
                                hapticManager.performActionComplete(SwipeHapticDirection.TRASH)
                                scope.launch {
                                    offsetY.animateTo(-screenHeightPx * 1.5f, tween(150))
                                    onSwipeUp()  // Callback after animation
                                }
                            }
                            // Swipe down → Maybe
                            reachedDown -> {
                                hasTriggeredSwipe = true
                                hapticManager.performActionComplete(SwipeHapticDirection.MAYBE)
                                scope.launch {
                                    offsetY.animateTo(screenHeightPx * 0.6f, tween(200))
                                    onSwipeDown()  // Callback after animation
                                }
                            }
                            // Swipe right → Keep
                            reachedRight -> {
                                hasTriggeredSwipe = true
                                hapticManager.performActionComplete(SwipeHapticDirection.KEEP)
                                scope.launch {
                                    offsetX.animateTo(screenWidthPx * 1.5f, tween(150))
                                    onSwipeRight()  // Callback after animation
                                }
                            }
                            // Swipe left → Keep
                            reachedLeft -> {
                                hasTriggeredSwipe = true
                                hapticManager.performActionComplete(SwipeHapticDirection.KEEP)
                                scope.launch {
                                    offsetX.animateTo(-screenWidthPx * 1.5f, tween(150))
                                    onSwipeLeft()  // Callback after animation
                                }
                            }
                            // Snap back to center
                            else -> {
                                scope.launch {
                                    // DES-033: 手势回弹增强 - 使用更活泼的弹性动画
                                    launch {
                                        offsetX.animateTo(0f, PicZenMotion.Springs.playful())
                                    }
                                    launch {
                                        offsetY.animateTo(0f, PicZenMotion.Springs.playful())
                                    }
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        if (!currentIsTopCard || hasTriggeredSwipe) return@detectDragGestures
                        // DES-033: 手势回弹增强
                        scope.launch {
                            launch { offsetX.animateTo(0f, PicZenMotion.Springs.playful()) }
                            launch { offsetY.animateTo(0f, PicZenMotion.Springs.playful()) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (!currentIsTopCard || hasTriggeredSwipe) return@detectDragGestures
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                        
                        // Check for threshold crossing (for UI and haptic feedback)
                        // Use currentThreshold* to always use latest sensitivity values
                        val newReachedRight = offsetX.value > currentThresholdRight
                        val newReachedLeft = offsetX.value < -currentThresholdLeft
                        val newReachedUp = offsetY.value < -currentThresholdUp && abs(offsetY.value) > abs(offsetX.value)
                        val newReachedDown = offsetY.value > currentThresholdDown && abs(offsetY.value) > abs(offsetX.value)
                        
                        // Phase 3-7: 达到临界点时触发震动反馈（只在首次达到时触发）
                        // Update threshold states (used for visual and haptic feedback)
                        if (newReachedRight && !hasReachedThresholdRight) {
                            hasReachedThresholdRight = true
                            hapticManager.performThresholdReached(SwipeHapticDirection.KEEP)
                        } else if (!newReachedRight) {
                            hasReachedThresholdRight = false
                        }
                        
                        if (newReachedLeft && !hasReachedThresholdLeft) {
                            hasReachedThresholdLeft = true
                            hapticManager.performThresholdReached(SwipeHapticDirection.KEEP)
                        } else if (!newReachedLeft) {
                            hasReachedThresholdLeft = false
                        }
                        
                        if (newReachedUp && !hasReachedThresholdUp) {
                            hasReachedThresholdUp = true
                            hapticManager.performThresholdReached(SwipeHapticDirection.TRASH)
                        } else if (!newReachedUp) {
                            hasReachedThresholdUp = false
                        }
                        
                        if (newReachedDown && !hasReachedThresholdDown) {
                            hasReachedThresholdDown = true
                            hapticManager.performThresholdReached(SwipeHapticDirection.MAYBE)
                        } else if (!newReachedDown) {
                            hasReachedThresholdDown = false
                        }
                    }
                )
            }
    ) {
        PhotoCard(
            photo = photo,
            swipeProgress = progressX,
            swipeProgressY = progressY,
            swipeDirection = currentDirection,
            hasReachedThreshold = hasReachedThreshold,
            onPhotoClick = onPhotoClick,
            showInfoOnImage = showInfoOnImage
        )
    }
}
