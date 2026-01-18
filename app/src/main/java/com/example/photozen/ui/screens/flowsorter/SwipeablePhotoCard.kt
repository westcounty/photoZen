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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.photozen.data.local.entity.PhotoEntity
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
 */
private const val ROTATION_MULTIPLIER = 15f

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
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit = {},
    onPhotoClick: (() -> Unit)? = null,
    showInfoOnImage: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
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
    
    // Calculate rotation based on horizontal offset
    val rotation = (offsetX.value / screenWidthPx) * ROTATION_MULTIPLIER
    
    // Scale effect during swipe
    val scale = when {
        offsetY.value > 0 -> 1f - (offsetY.value / screenHeightPx) * 0.3f
        else -> 1f + (abs(offsetX.value) / screenWidthPx) * 0.05f
    }
    
    // Alpha for sinking effect
    val alpha = if (offsetY.value > 0) {
        1f - (offsetY.value / screenHeightPx) * 0.5f
    } else 1f
    
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .pointerInput(photo.id, swipeSensitivity) {
                detectDragGestures(
                    onDragStart = {
                        if (!currentIsTopCard || hasTriggeredSwipe) return@detectDragGestures
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    offsetY.animateTo(-screenHeightPx * 1.5f, tween(150))
                                    onSwipeUp()  // Callback after animation
                                }
                            }
                            // Swipe down → Maybe
                            reachedDown -> {
                                hasTriggeredSwipe = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    offsetY.animateTo(screenHeightPx * 0.6f, tween(200))
                                    onSwipeDown()  // Callback after animation
                                }
                            }
                            // Swipe right → Keep
                            reachedRight -> {
                                hasTriggeredSwipe = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    offsetX.animateTo(screenWidthPx * 1.5f, tween(150))
                                    onSwipeRight()  // Callback after animation
                                }
                            }
                            // Swipe left → Keep
                            reachedLeft -> {
                                hasTriggeredSwipe = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    offsetX.animateTo(-screenWidthPx * 1.5f, tween(150))
                                    onSwipeLeft()  // Callback after animation
                                }
                            }
                            // Snap back to center
                            else -> {
                                scope.launch {
                                    launch {
                                        offsetX.animateTo(0f, spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ))
                                    }
                                    launch {
                                        offsetY.animateTo(0f, spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ))
                                    }
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        if (!currentIsTopCard || hasTriggeredSwipe) return@detectDragGestures
                        scope.launch {
                            launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                            launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (!currentIsTopCard || hasTriggeredSwipe) return@detectDragGestures
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                        
                        // Check for threshold crossing (for UI feedback, no haptic)
                        // Use currentThreshold* to always use latest sensitivity values
                        val newReachedRight = offsetX.value > currentThresholdRight
                        val newReachedLeft = offsetX.value < -currentThresholdLeft
                        val newReachedUp = offsetY.value < -currentThresholdUp && abs(offsetY.value) > abs(offsetX.value)
                        val newReachedDown = offsetY.value > currentThresholdDown && abs(offsetY.value) > abs(offsetX.value)
                        
                        // Update threshold states (used for visual feedback)
                        if (newReachedRight && !hasReachedThresholdRight) {
                            hasReachedThresholdRight = true
                        } else if (!newReachedRight) {
                            hasReachedThresholdRight = false
                        }
                        
                        if (newReachedLeft && !hasReachedThresholdLeft) {
                            hasReachedThresholdLeft = true
                        } else if (!newReachedLeft) {
                            hasReachedThresholdLeft = false
                        }
                        
                        if (newReachedUp && !hasReachedThresholdUp) {
                            hasReachedThresholdUp = true
                        } else if (!newReachedUp) {
                            hasReachedThresholdUp = false
                        }
                        
                        if (newReachedDown && !hasReachedThresholdDown) {
                            hasReachedThresholdDown = true
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
