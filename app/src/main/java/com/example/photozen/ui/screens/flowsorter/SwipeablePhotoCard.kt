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
import androidx.compose.runtime.mutableFloatStateOf
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
 */
private const val SWIPE_THRESHOLD = 0.35f

/**
 * Rotation multiplier for card tilt during horizontal swipe
 */
private const val ROTATION_MULTIPLIER = 15f

/**
 * Swipeable photo card with gesture detection.
 * Supports horizontal swipes (left/right) and vertical swipe (up).
 * 
 * @param photo The photo to display
 * @param onSwipeLeft Called when swiped left (Trash)
 * @param onSwipeRight Called when swiped right (Keep)
 * @param onSwipeUp Called when swiped up (Maybe)
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
    
    // Calculate normalized progress (-1 to 1 for X, 0 to -1 for Y)
    val progressX = (offsetX.value / screenWidthPx).coerceIn(-1f, 1f)
    val progressY = (offsetY.value / screenHeightPx).coerceIn(-1f, 0f)
    
    // Determine swipe direction based on offset
    LaunchedEffect(offsetX.value, offsetY.value) {
        currentDirection = when {
            abs(offsetY.value) > abs(offsetX.value) && offsetY.value < -50f -> SwipeDirection.UP
            offsetX.value > 50f -> SwipeDirection.RIGHT
            offsetX.value < -50f -> SwipeDirection.LEFT
            else -> SwipeDirection.NONE
        }
    }
    
    // Calculate rotation based on horizontal offset
    val rotation = (offsetX.value / screenWidthPx) * ROTATION_MULTIPLIER
    
    // Calculate scale for "lifting" effect
    val scale = 1f + (abs(offsetX.value) / screenWidthPx) * 0.05f
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
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
                                // Swipe up detected
                                offsetY.value < -thresholdY && abs(offsetY.value) > abs(offsetX.value) -> {
                                    // Strong haptic for action
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Animate off screen
                                    offsetY.animateTo(
                                        targetValue = -screenHeightPx * 1.5f,
                                        animationSpec = tween(300)
                                    )
                                    onSwipeUp()
                                }
                                // Swipe right detected
                                offsetX.value > thresholdX -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    offsetX.animateTo(
                                        targetValue = screenWidthPx * 1.5f,
                                        animationSpec = tween(300)
                                    )
                                    onSwipeRight()
                                }
                                // Swipe left detected
                                offsetX.value < -thresholdX -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    offsetX.animateTo(
                                        targetValue = -screenWidthPx * 1.5f,
                                        animationSpec = tween(300)
                                    )
                                    onSwipeLeft()
                                }
                                // Snap back to center
                                else -> {
                                    launch { 
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    launch {
                                        offsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            launch { offsetX.animateTo(0f, spring()) }
                            launch { offsetY.animateTo(0f, spring()) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            // Allow free dragging in both directions
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            // Only allow upward vertical drag
                            if (offsetY.value + dragAmount.y < 0 || dragAmount.y < 0) {
                                offsetY.snapTo((offsetY.value + dragAmount.y).coerceAtMost(0f))
                            }
                        }
                    }
                )
            }
    ) {
        PhotoCard(
            photo = photo,
            swipeProgress = progressX,
            swipeDirection = currentDirection,
            onPhotoClick = onPhotoClick
        )
    }
}

/**
 * Preview card that shows behind the swipeable card.
 * Provides depth effect in the card stack.
 */
@Composable
fun PreviewPhotoCard(
    photo: PhotoEntity,
    stackIndex: Int,
    modifier: Modifier = Modifier
) {
    val scale = 1f - (stackIndex * 0.05f)
    val offsetY = stackIndex * 16f
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = offsetY
            }
    ) {
        PhotoCard(
            photo = photo,
            swipeProgress = 0f,
            swipeDirection = SwipeDirection.NONE
        )
    }
}
