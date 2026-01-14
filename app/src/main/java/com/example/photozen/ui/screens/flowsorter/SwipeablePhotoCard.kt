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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * Threshold for triggering a swipe action (as fraction of screen width/height)
 * Lower value = more responsive, easier to trigger
 */
private const val SWIPE_THRESHOLD = 0.25f

/**
 * Rotation multiplier for card tilt during horizontal swipe
 */
private const val ROTATION_MULTIPLIER = 15f

/**
 * Swipeable photo card with gesture detection.
 * 
 * Gesture mapping:
 * - Left/Right swipe → Keep (preserve)
 * - Up swipe → Trash (delete)
 * - Down swipe → Maybe (review later) with "sinking" animation
 * 
 * @param photo The photo to display
 * @param onSwipeLeft Called when swiped left (Keep)
 * @param onSwipeRight Called when swiped right (Keep)
 * @param onSwipeUp Called when swiped up (Trash)
 * @param onSwipeDown Called when swiped down (Maybe)
 * @param onPhotoClick Called when photo is clicked (for fullscreen view)
 * @param enabled Whether swipe gestures are enabled
 * @param modifier Modifier for the card
 */
@Composable
fun SwipeablePhotoCard(
    photo: PhotoEntity,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit = {},
    onPhotoClick: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Screen dimensions for threshold calculation
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Animatable offsets for smooth animations
    // Key by photo.id to reset state when photo changes
    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }
    
    // Track current swipe direction
    var currentDirection by remember(photo.id) { mutableStateOf(SwipeDirection.NONE) }
    
    // Calculate normalized progress (-1 to 1 for X and Y)
    val progressX = (offsetX.value / screenWidthPx).coerceIn(-1f, 1f)
    val progressY = (offsetY.value / screenHeightPx).coerceIn(-1f, 1f)
    
    // Determine swipe direction based on offset
    LaunchedEffect(offsetX.value, offsetY.value) {
        currentDirection = when {
            abs(offsetY.value) > abs(offsetX.value) && offsetY.value < -50f -> SwipeDirection.UP
            abs(offsetY.value) > abs(offsetX.value) && offsetY.value > 50f -> SwipeDirection.DOWN
            offsetX.value > 50f -> SwipeDirection.RIGHT
            offsetX.value < -50f -> SwipeDirection.LEFT
            else -> SwipeDirection.NONE
        }
    }
    
    // Calculate rotation based on horizontal offset
    val rotation = (offsetX.value / screenWidthPx) * ROTATION_MULTIPLIER
    
    // Calculate scale - shrink for down swipe (sinking effect), lift for others
    val scale = when {
        offsetY.value > 0 -> 1f - (offsetY.value / screenHeightPx) * 0.3f // Shrink when swiping down
        else -> 1f + (abs(offsetX.value) / screenWidthPx) * 0.05f // Lift for horizontal
    }
    
    // Calculate alpha for sinking effect
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
            .pointerInput(enabled, photo.id) {
                if (!enabled) return@pointerInput
                
                detectDragGestures(
                    onDragStart = {
                        // Light haptic on drag start
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragEnd = {
                        scope.launch {
                            val thresholdX = screenWidthPx * SWIPE_THRESHOLD
                            val thresholdY = screenHeightPx * SWIPE_THRESHOLD
                            
                            when {
                                // Swipe up detected → TRASH (delete)
                                offsetY.value < -thresholdY && abs(offsetY.value) > abs(offsetX.value) -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Call callback first
                                    onSwipeUp()
                                    // Fire-and-forget animation
                                    launch {
                                        try {
                                            offsetY.animateTo(
                                                targetValue = -screenHeightPx * 1.5f,
                                                animationSpec = tween(120)
                                            )
                                        } catch (_: Exception) {}
                                    }
                                }
                                // Swipe down detected → MAYBE (sinking into pool effect)
                                offsetY.value > thresholdY && abs(offsetY.value) > abs(offsetX.value) -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Call callback first
                                    onSwipeDown()
                                    // Fire-and-forget sinking animation
                                    launch {
                                        try {
                                            offsetY.animateTo(
                                                targetValue = screenHeightPx * 0.6f,
                                                animationSpec = tween(180)
                                            )
                                        } catch (_: Exception) {}
                                    }
                                }
                                // Swipe right detected → KEEP
                                offsetX.value > thresholdX -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeRight()
                                    launch {
                                        try {
                                            offsetX.animateTo(
                                                targetValue = screenWidthPx * 1.5f,
                                                animationSpec = tween(120)
                                            )
                                        } catch (_: Exception) {}
                                    }
                                }
                                // Swipe left detected → KEEP (same as right)
                                offsetX.value < -thresholdX -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeLeft()
                                    launch {
                                        try {
                                            offsetX.animateTo(
                                                targetValue = -screenWidthPx * 1.5f,
                                                animationSpec = tween(120)
                                            )
                                        } catch (_: Exception) {}
                                    }
                                }
                                // Snap back to center with bounce effect
                                else -> {
                                    launch { 
                                        try {
                                            offsetX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        } catch (_: Exception) {}
                                    }
                                    launch {
                                        try {
                                            offsetY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                            launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            // Allow free dragging in all directions
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
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
            onPhotoClick = onPhotoClick
        )
    }
}

/**
 * Preview card that shows behind the swipeable card.
 * Shows at full size with no offset for instant, seamless transition.
 */
@Composable
fun PreviewPhotoCard(
    photo: PhotoEntity,
    stackIndex: Int,
    modifier: Modifier = Modifier
) {
    // No offset, no scale - card is exactly the same size and position
    // This ensures zero visual shift when front card is swiped away
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 1f
                scaleY = 1f
                translationY = 0f
                alpha = 0.95f
            }
    ) {
        PhotoCard(
            photo = photo,
            swipeProgress = 0f,
            swipeDirection = SwipeDirection.NONE
        )
    }
}
