package com.example.photozen.domain.usecase

import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoRepository
import javax.inject.Inject

/**
 * UseCase for sorting photos in the Flow Sorter.
 * Handles Keep (right swipe), Trash (left swipe), and Maybe (up swipe) actions.
 * 
 * Non-destructive: Only updates status in local database, never deletes original files.
 */
class SortPhotoUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    /**
     * Mark photo to Keep (swipe right).
     * User wants to preserve this photo.
     */
    suspend fun keepPhoto(photoId: String) {
        photoRepository.keepPhoto(photoId)
    }
    
    /**
     * Mark photo for Trash (swipe left).
     * Non-destructive: only marks status, doesn't delete file.
     */
    suspend fun trashPhoto(photoId: String) {
        photoRepository.trashPhoto(photoId)
    }
    
    /**
     * Mark photo as Maybe (swipe up).
     * Photo will appear in Light Table for comparison.
     */
    suspend fun maybePhoto(photoId: String) {
        photoRepository.maybePhoto(photoId)
    }
    
    /**
     * Reset photo back to Unsorted.
     * Useful for undo functionality.
     */
    suspend fun resetPhoto(photoId: String) {
        photoRepository.resetPhotoStatus(photoId)
    }
    
    /**
     * Update photo with specific status.
     */
    suspend fun updateStatus(photoId: String, status: PhotoStatus) {
        photoRepository.updatePhotoStatus(photoId, status)
    }
    
    /**
     * Batch update status for multiple photos.
     * Useful for bulk operations in Light Table.
     */
    suspend fun batchUpdateStatus(photoIds: List<String>, status: PhotoStatus) {
        photoRepository.updatePhotoStatusBatch(photoIds, status)
    }
}
