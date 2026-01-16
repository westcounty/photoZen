package com.example.photozen.ui.screens.flowsorter

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * @param onSwipeLeft Called with photo ID when swiped left (Keep)
 * @param onSwipeRight Called with photo ID when swiped right (Keep)
 * @param onSwipeUp Called with photo ID when swiped up (Trash)
 * @param onSwipeDown Called with photo ID when swiped down (Maybe)
 * @param stackSize Number of cards to render in the stack
 * @param modifier Modifier for the stack container
 */
@Composable
fun SwipeableCardStack(
    photos: List<PhotoEntity>,
    onSwipeLeft: (String) -> Unit,
    onSwipeRight: (String) -> Unit,
    onSwipeUp: (String) -> Unit,
    onSwipeDown: (String) -> Unit,
    stackSize: Int = DEFAULT_STACK_SIZE,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    
    // LOCAL STATE: Track cards that have been swiped out
    // Using SnapshotStateList for thread-safe mutations
    val swipedOutIds = remember { mutableStateListOf<String>() }
    
    // Clean up swipedOutIds when photos list changes (ViewModel processed the swipe)
    LaunchedEffect(photos) {
        val currentPhotoIds = photos.map { it.id }.toSet()
        // Remove IDs that are no longer in the photos list
        swipedOutIds.removeAll { it !in currentPhotoIds }
    }
    
    // Calculate visible photos
    val visiblePhotos = remember(photos, swipedOutIds.size) {
        photos.filter { it.id !in swipedOutIds }.take(stackSize)
    }
    
    // The top card ID
    val topCardId = visiblePhotos.firstOrNull()?.id
    
    // Preload images on initial composition and when photos change
    LaunchedEffect(photos) {
        withContext(Dispatchers.IO) {
            photos.take(PRELOAD_COUNT).forEach { photo ->
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
    
    // Preload more as user swipes
    LaunchedEffect(swipedOutIds.size) {
        if (swipedOutIds.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                photos.filter { it.id !in swipedOutIds }
                    .take(PRELOAD_COUNT)
                    .forEach { photo ->
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
                    onSwipeLeft = {
                        if (photoId !in swipedOutIds) {
                            swipedOutIds.add(photoId)
                            onSwipeLeft(photoId)
                        }
                    },
                    onSwipeRight = {
                        if (photoId !in swipedOutIds) {
                            swipedOutIds.add(photoId)
                            onSwipeRight(photoId)
                        }
                    },
                    onSwipeUp = {
                        if (photoId !in swipedOutIds) {
                            swipedOutIds.add(photoId)
                            onSwipeUp(photoId)
                        }
                    },
                    onSwipeDown = {
                        if (photoId !in swipedOutIds) {
                            swipedOutIds.add(photoId)
                            onSwipeDown(photoId)
                        }
                    },
                    onPhotoClick = null
                )
            }
        }
    }
}
