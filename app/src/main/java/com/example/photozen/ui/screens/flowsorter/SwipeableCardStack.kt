package com.example.photozen.ui.screens.flowsorter

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.example.photozen.data.local.entity.PhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Default number of cards to render in the stack.
 */
private const val DEFAULT_STACK_SIZE = 5

/**
 * Number of images to preload ahead for smoother experience.
 */
private const val PRELOAD_COUNT = 30

/**
 * Max size for preloaded images (width or height).
 */
private const val PRELOAD_MAX_SIZE = 1200

/**
 * A swipeable card stack with smooth animations.
 * 
 * IMPORTANT: This component relies entirely on the ViewModel to manage the photo list.
 * When a photo is swiped, the callback notifies the ViewModel, which updates the list.
 * The SwipeablePhotoCard handles its own swipe-triggered state (hasTriggeredSwipe)
 * to prevent double-callbacks during animation.
 * 
 * NO LOCAL STATE for tracking swiped photos - this caused synchronization issues.
 * 
 * @param photos List of photos to display (pre-filtered by ViewModel)
 * @param swipeSensitivity Sensitivity setting for swipe gestures
 * @param onSwipeLeft Called when swiped left (Keep)
 * @param onSwipeRight Called when swiped right (Keep)
 * @param onSwipeUp Called when swiped up (Trash)
 * @param onSwipeDown Called when swiped down (Maybe)
 * @param stackSize Number of cards to render
 * @param showInfoOnImage When true, photo info is displayed on the image
 * @param modifier Modifier for the container
 */
@Composable
fun SwipeableCardStack(
    photos: List<PhotoEntity>,
    swipeSensitivity: Float = 1.0f,
    hapticFeedbackEnabled: Boolean = true,  // Phase 3-7: 震动反馈开关
    onSwipeLeft: (String) -> Unit,
    onSwipeRight: (String) -> Unit,
    onSwipeUp: (String) -> Unit,
    onSwipeDown: (String) -> Unit,
    stackSize: Int = DEFAULT_STACK_SIZE,
    showInfoOnImage: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    
    // Take only the visible stack
    val visiblePhotos = photos.take(stackSize)
    val topCardId = visiblePhotos.firstOrNull()?.id
    
    // Preload images for smooth experience
    LaunchedEffect(photos) {
        withContext(Dispatchers.IO) {
            // Synchronously load visible stack cards first
            photos.take(stackSize).forEach { photo ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(Uri.parse(photo.systemUri))
                        .memoryCacheKey(photo.id)
                        .diskCacheKey(photo.id)
                        .size(Size(PRELOAD_MAX_SIZE, PRELOAD_MAX_SIZE))
                        .build()
                    imageLoader.execute(request)
                } catch (_: Exception) {}
            }
            
            // Async preload more in background
            photos.drop(stackSize).take(PRELOAD_COUNT - stackSize).forEach { photo ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(Uri.parse(photo.systemUri))
                        .memoryCacheKey(photo.id)
                        .diskCacheKey(photo.id)
                        .size(Size(PRELOAD_MAX_SIZE, PRELOAD_MAX_SIZE))
                        .build()
                    imageLoader.enqueue(request)
                } catch (_: Exception) {}
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Render cards in reverse order (bottom to top)
        // key(photoId) ensures each card is uniquely identified and will be
        // properly removed/re-composed when the photos list changes
        visiblePhotos.asReversed().forEach { photo ->
            val isTopCard = photo.id == topCardId
            
            key(photo.id) {
                SwipeablePhotoCard(
                    photo = photo,
                    isTopCard = isTopCard,
                    swipeSensitivity = swipeSensitivity,
                    hapticFeedbackEnabled = hapticFeedbackEnabled,  // Phase 3-7
                    showInfoOnImage = showInfoOnImage,
                    onSwipeLeft = { onSwipeLeft(photo.id) },
                    onSwipeRight = { onSwipeRight(photo.id) },
                    onSwipeUp = { onSwipeUp(photo.id) },
                    onSwipeDown = { onSwipeDown(photo.id) },
                    onPhotoClick = null
                )
            }
        }
    }
}
