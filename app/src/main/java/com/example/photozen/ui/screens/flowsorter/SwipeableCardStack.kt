package com.example.photozen.ui.screens.flowsorter

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.example.photozen.data.local.entity.PhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "SwipeableCardStack"

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
 * A swipeable card stack with smooth animations and instant response.
 * 
 * Uses local state (swipedOutIds) to provide instant UI feedback when swiping,
 * while the ViewModel processes the actual data update in the background.
 * 
 * @param photos List of photos to display in the stack
 * @param swipeSensitivity Sensitivity setting for swipe gestures (0.5 = very sensitive, 1.5 = less sensitive)
 * @param onSwipeLeft Called with photo ID when swiped left (Keep)
 * @param onSwipeRight Called with photo ID when swiped right (Keep)
 * @param onSwipeUp Called with photo ID when swiped up (Trash)
 * @param onSwipeDown Called with photo ID when swiped down (Maybe)
 * @param stackSize Number of cards to render in the stack
 * @param showInfoOnImage When true, photo info is displayed on the image itself
 * @param modifier Modifier for the stack container
 */
@Composable
fun SwipeableCardStack(
    photos: List<PhotoEntity>,
    swipeSensitivity: Float = 1.0f,
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
    
    // LOCAL STATE: Track cards that have been swiped out
    // Using SnapshotStateList for thread-safe mutations
    val swipedOutIds = remember { mutableStateListOf<String>() }
    
    // Clean up swipedOutIds when photos list changes (ViewModel processed the swipe)
    // IMPORTANT: Only clean up after a small delay to ensure smooth animation
    LaunchedEffect(photos) {
        // Small delay to let animations complete before cleanup
        kotlinx.coroutines.delay(100)
        val currentPhotoIds = photos.map { it.id }.toSet()
        // Remove IDs that are no longer in the photos list
        swipedOutIds.removeAll { it !in currentPhotoIds }
    }
    
    // Calculate visible photos directly from both photos and swipedOutIds
    // This ensures immediate UI update when user swipes
    val visiblePhotos = photos.filter { it.id !in swipedOutIds }.take(stackSize)
    
    // The top card ID - directly derived from visiblePhotos
    val topCardId = visiblePhotos.firstOrNull()?.id
    
    // High priority: Synchronously load first few images to prevent flash
    // This ensures the visible cards have images ready before display
    LaunchedEffect(photos) {
        withContext(Dispatchers.IO) {
            // First, synchronously load the visible stack cards (high priority)
            photos.take(stackSize).forEach { photo ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(Uri.parse(photo.systemUri))
                        .memoryCacheKey(photo.id)
                        .diskCacheKey(photo.id)
                        .size(Size(PRELOAD_MAX_SIZE, PRELOAD_MAX_SIZE))
                        .build()
                    // Use execute (synchronous) for visible cards to ensure cache is populated
                    imageLoader.execute(request)
                } catch (_: Exception) {}
            }
            
            // Then, async preload more images in background
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
    
    // Preload more as user swipes - use async only to avoid any UI blocking
    LaunchedEffect(swipedOutIds.size) {
        if (swipedOutIds.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val remainingPhotos = photos.filter { it.id !in swipedOutIds }
                
                // Preload visible + buffer cards using async enqueue (non-blocking)
                // The initial LaunchedEffect(photos) already cached the first batch
                remainingPhotos.take(PRELOAD_COUNT).forEach { photo ->
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(Uri.parse(photo.systemUri))
                            .memoryCacheKey(photo.id)
                            .diskCacheKey(photo.id)
                            .size(Size(PRELOAD_MAX_SIZE, PRELOAD_MAX_SIZE))
                            .build()
                        imageLoader.enqueue(request)  // Async, non-blocking
                    } catch (_: Exception) {}
                }
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Render cards in reverse order (bottom to top)
        visiblePhotos.asReversed().forEach { photo ->
            val isTopCard = photo.id == topCardId
            val photoId = photo.id
            
            key(photoId) {
                SwipeablePhotoCard(
                    photo = photo,
                    isTopCard = isTopCard,
                    swipeSensitivity = swipeSensitivity,
                    showInfoOnImage = showInfoOnImage,
                    onSwipeLeft = {
                        if (photoId !in swipedOutIds) {
                            Log.d(TAG, "onSwipeLeft: photoId=$photoId, swipedOutIds before=${swipedOutIds.toList()}")
                            // IMPORTANT: Notify ViewModel FIRST (updates counter immediately)
                            // Then add to local state (triggers UI removal)
                            // This ensures counter is incremented before the card disappears
                            onSwipeLeft(photoId)
                            swipedOutIds.add(photoId)
                            Log.d(TAG, "onSwipeLeft: swipedOutIds after=${swipedOutIds.toList()}")
                        }
                    },
                    onSwipeRight = {
                        if (photoId !in swipedOutIds) {
                            Log.d(TAG, "onSwipeRight: photoId=$photoId, swipedOutIds before=${swipedOutIds.toList()}")
                            onSwipeRight(photoId)
                            swipedOutIds.add(photoId)
                            Log.d(TAG, "onSwipeRight: swipedOutIds after=${swipedOutIds.toList()}")
                        }
                    },
                    onSwipeUp = {
                        if (photoId !in swipedOutIds) {
                            Log.d(TAG, "onSwipeUp: photoId=$photoId, swipedOutIds before=${swipedOutIds.toList()}")
                            onSwipeUp(photoId)
                            swipedOutIds.add(photoId)
                            Log.d(TAG, "onSwipeUp: swipedOutIds after=${swipedOutIds.toList()}")
                        }
                    },
                    onSwipeDown = {
                        if (photoId !in swipedOutIds) {
                            Log.d(TAG, "onSwipeDown: photoId=$photoId, swipedOutIds before=${swipedOutIds.toList()}")
                            onSwipeDown(photoId)
                            swipedOutIds.add(photoId)
                            Log.d(TAG, "onSwipeDown: swipedOutIds after=${swipedOutIds.toList()}")
                        }
                    },
                    onPhotoClick = null
                )
            }
        }
    }
}
