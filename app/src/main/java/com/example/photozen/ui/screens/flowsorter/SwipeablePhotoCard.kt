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
private const val SWIPE_THRESHOLD = 0.25f

/**
 * Rotation multiplier for card tilt during horizontal swipe
 */
private const val ROTATION_MULTIPLIER = 15f

/**
 * Swipeable photo card with smooth animations.
 * 
 * @param photo The photo to display
 * @param isTopCard Whether this card is the top card (receives gestures)
 * @param onSwipeLeft Called when swiped left (Keep)
 * @param onSwipeRight Called when swiped right (Keep)
 * @param onSwipeUp Called when swiped up (Trash)
 * @param onSwipeDown Called when swiped down (Maybe)
 * @param onPhotoClick Called when photo is clicked
 * @param modifier Modifier for the card
 */
@Composable
fun SwipeablePhotoCard(
    photo: PhotoEntity,
    isTopCard: Boolean = true,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit = {},
    onPhotoClick: (() -> Unit)? = null,
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
    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }
    
    // Prevent multiple swipe callbacks
    var hasTriggeredSwipe by remember(photo.id) { mutableStateOf(false) }
    
    // Track current swipe direction for visual feedback
    var currentDirection by remember(photo.id) { mutableStateOf(SwipeDirection.NONE) }
    
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
            .pointerInput(photo.id) {
                detectDragGestures(
                    onDragStart = {
                        if (!isTopCard || hasTriggeredSwipe) return@detectDragGestures
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragEnd = {
                        if (!isTopCard || hasTriggeredSwipe) return@detectDragGestures
                        
                        val thresholdX = screenWidthPx * SWIPE_THRESHOLD
                        val thresholdY = screenHeightPx * SWIPE_THRESHOLD
                        
                        when {
                            // Swipe up → Trash
                            offsetY.value < -thresholdY && abs(offsetY.value) > abs(offsetX.value) -> {
                                hasTriggeredSwipe = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwipeUp()
                                scope.launch {
                                    offsetY.animateTo(-screenHeightPx * 1.5f, tween(150))
                                }
                            }
                            // Swipe down → Maybe
                            offsetY.value > thresholdY && abs(offsetY.value) > abs(offsetX.value) -> {
                                hasTriggeredSwipe = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwipeDown()
                                scope.launch {
                                    offsetY.animateTo(screenHeightPx * 0.6f, tween(200))
                                }
                            }
                            // Swipe right → Keep
                            offsetX.value > thresholdX -> {
                                hasTriggeredSwipe = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwipeRight()
                                scope.launch {
                                    offsetX.animateTo(screenWidthPx * 1.5f, tween(150))
                                }
                            }
                            // Swipe left → Keep
                            offsetX.value < -thresholdX -> {
                                hasTriggeredSwipe = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwipeLeft()
                                scope.launch {
                                    offsetX.animateTo(-screenWidthPx * 1.5f, tween(150))
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
                        if (!isTopCard || hasTriggeredSwipe) return@detectDragGestures
                        scope.launch {
                            launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                            launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (!isTopCard || hasTriggeredSwipe) return@detectDragGestures
                        change.consume()
                        scope.launch {
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
