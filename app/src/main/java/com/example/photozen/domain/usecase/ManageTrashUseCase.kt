package com.example.photozen.domain.usecase

import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for managing trashed photos.
 * 
 * Important: "Trash" in PicZen is non-destructive.
 * - Photos marked as TRASH are only flagged in the database
 * - Original files remain untouched in MediaStore
 * - Only "Empty Trash" permanently removes records from our DB
 * - Actual file deletion would require MediaStore.createDeleteRequest()
 */
class ManageTrashUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    /**
     * Get all trashed photos.
     */
    fun getTrashedPhotos(): Flow<List<PhotoEntity>> {
        return photoRepository.getTrashPhotos()
    }
    
    /**
     * Get count of trashed photos.
     */
    fun getTrashCount(): Flow<Int> {
        return photoRepository.getTrashCount()
    }
    
    /**
     * Delete specific photos permanently from our database.
     * 
     * Note: This only removes records from PicZen's local database.
     * Original files in MediaStore should be deleted via MediaStore API separately.
     */
    suspend fun deletePhotos(photoIds: List<String>) {
        photoRepository.deletePhotosByIds(photoIds)
    }
    
    /**
     * Empty trash - permanently delete all trashed photo records.
     * 
     * Note: This only removes records from PicZen's local database.
     * Original files in MediaStore are NOT deleted.
     * 
     * TODO: Implement actual file deletion with MediaStore.createDeleteRequest()
     * for Android 11+ or direct delete for older versions with proper permissions.
     */
    suspend fun emptyTrash() {
        photoRepository.emptyTrash()
    }
    
    /**
     * Restore a trashed photo back to unsorted.
     */
    suspend fun restoreFromTrash(photoId: String) {
        photoRepository.resetPhotoStatus(photoId)
    }
    
    /**
     * Restore multiple photos from trash.
     */
    suspend fun restoreAllFromTrash(photoIds: List<String>) {
        photoRepository.updatePhotoStatusBatch(photoIds, com.example.photozen.data.model.PhotoStatus.UNSORTED)
    }
}
