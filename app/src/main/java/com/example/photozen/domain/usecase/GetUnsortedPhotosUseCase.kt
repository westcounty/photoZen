package com.example.photozen.domain.usecase

import androidx.paging.PagingData
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for retrieving unsorted photos for the Flow Sorter screen.
 * Unsorted photos are those that haven't been categorized as KEEP, TRASH, or MAYBE.
 */
class GetUnsortedPhotosUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    /**
     * Get all unsorted photos as Flow.
     * Used for Flow Sorter's swipe card stack.
     */
    operator fun invoke(): Flow<List<PhotoEntity>> {
        return photoRepository.getUnsortedPhotos()
    }
    
    /**
     * Get unsorted photos filtered by bucket IDs (for camera only mode).
     */
    fun byBuckets(bucketIds: List<String>): Flow<List<PhotoEntity>> {
        return photoRepository.getUnsortedPhotosByBuckets(bucketIds)
    }
    
    /**
     * Get unsorted photos excluding specific bucket IDs (for exclude camera mode).
     */
    fun excludingBuckets(bucketIds: List<String>): Flow<List<PhotoEntity>> {
        return photoRepository.getUnsortedPhotosExcludingBuckets(bucketIds)
    }
    
    /**
     * Get unsorted photos with paging support.
     */
    fun paged(): Flow<PagingData<PhotoEntity>> {
        return photoRepository.getUnsortedPhotosPaged()
    }
    
    /**
     * Get count of unsorted photos.
     * Used to show progress indicator.
     */
    fun getCount(): Flow<Int> {
        return photoRepository.getUnsortedCount()
    }
}
