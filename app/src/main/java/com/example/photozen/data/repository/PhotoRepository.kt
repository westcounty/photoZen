package com.example.photozen.data.repository

import androidx.paging.PagingData
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.PhotoWithTags
import com.example.photozen.data.model.CropState
import com.example.photozen.data.model.PhotoStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for photo operations.
 * Abstracts data sources (Room DB + MediaStore) from the domain layer.
 */
interface PhotoRepository {
    
    // ==================== SYNC ====================
    
    /**
     * Sync photos from MediaStore to local database.
     * Adds new photos, keeps existing ones with their status.
     * 
     * @return Number of new photos added
     */
    suspend fun syncFromMediaStore(): Int
    
    /**
     * Check if initial sync has been performed.
     */
    suspend fun hasPerformedInitialSync(): Boolean
    
    // ==================== READ - Single ====================
    
    /**
     * Get a single photo by ID.
     */
    suspend fun getPhotoById(photoId: String): PhotoEntity?
    
    /**
     * Get a photo by ID as Flow for reactive updates.
     */
    fun getPhotoByIdFlow(photoId: String): Flow<PhotoEntity?>
    
    /**
     * Get photo with its tags.
     */
    suspend fun getPhotoWithTags(photoId: String): PhotoWithTags?
    
    // ==================== READ - Lists ====================
    
    /**
     * Get all photos as Flow.
     */
    fun getAllPhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get photos by status as Flow.
     */
    fun getPhotosByStatus(status: PhotoStatus): Flow<List<PhotoEntity>>
    
    /**
     * Get unsorted photos for Flow Sorter.
     */
    fun getUnsortedPhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get "Maybe" photos for Light Table.
     */
    fun getMaybePhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get "Keep" photos.
     */
    fun getKeepPhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get "Trash" photos.
     */
    fun getTrashPhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get virtual copies of a photo.
     */
    fun getVirtualCopies(parentId: String): Flow<List<PhotoEntity>>
    
    // ==================== READ - Paging ====================
    
    /**
     * Get all photos as PagingData.
     */
    fun getAllPhotosPaged(): Flow<PagingData<PhotoEntity>>
    
    /**
     * Get unsorted photos as PagingData.
     */
    fun getUnsortedPhotosPaged(): Flow<PagingData<PhotoEntity>>
    
    /**
     * Get photos by status as PagingData.
     */
    fun getPhotosByStatusPaged(status: PhotoStatus): Flow<PagingData<PhotoEntity>>
    
    // ==================== READ - Counts ====================
    
    /**
     * Get total photo count.
     */
    fun getTotalCount(): Flow<Int>
    
    /**
     * Get count by status.
     */
    fun getCountByStatus(status: PhotoStatus): Flow<Int>
    
    /**
     * Get unsorted count.
     */
    fun getUnsortedCount(): Flow<Int>
    
    // ==================== WRITE - Status ====================
    
    /**
     * Update photo status (KEEP, TRASH, MAYBE).
     */
    suspend fun updatePhotoStatus(photoId: String, status: PhotoStatus)
    
    /**
     * Batch update status for multiple photos.
     */
    suspend fun updatePhotoStatusBatch(photoIds: List<String>, status: PhotoStatus)
    
    /**
     * Move photo to Keep (swipe right).
     */
    suspend fun keepPhoto(photoId: String)
    
    /**
     * Move photo to Trash (swipe left).
     */
    suspend fun trashPhoto(photoId: String)
    
    /**
     * Move photo to Maybe (swipe up).
     */
    suspend fun maybePhoto(photoId: String)
    
    /**
     * Reset photo to Unsorted.
     */
    suspend fun resetPhotoStatus(photoId: String)
    
    // ==================== WRITE - Virtual Copies ====================
    
    /**
     * Create a virtual copy of a photo.
     * 
     * @param photoId Original photo ID
     * @param cropState Optional crop state to apply to the virtual copy
     * @return ID of the new virtual copy
     */
    suspend fun createVirtualCopy(photoId: String, cropState: CropState? = null): String
    
    /**
     * Delete a virtual copy.
     */
    suspend fun deleteVirtualCopy(virtualCopyId: String)
    
    // ==================== WRITE - Crop State ====================
    
    /**
     * Update crop state for a photo.
     */
    suspend fun updateCropState(
        photoId: String, 
        scale: Float, 
        offsetX: Float, 
        offsetY: Float, 
        rotation: Float,
        aspectRatioId: String = "free",
        cropFrameWidth: Float = 1f,
        cropFrameHeight: Float = 1f
    )
    
    /**
     * Reset crop state to default.
     */
    suspend fun resetCropState(photoId: String)
    
    // ==================== DELETE ====================
    
    /**
     * Permanently delete specific photos by IDs.
     * This removes from local DB only - actual file deletion requires MediaStore API.
     */
    suspend fun deletePhotosByIds(photoIds: List<String>)
    
    /**
     * Permanently delete all trashed photos.
     * This removes from local DB only - actual file deletion requires MediaStore API.
     */
    suspend fun emptyTrash()
    
    /**
     * Get count of photos in trash.
     */
    fun getTrashCount(): Flow<Int>
    
    // ==================== CAMERA COLLECTION ====================
    
    /**
     * Get distinct camera models for achievement system.
     */
    fun getDistinctCameraModels(): Flow<List<String>>
    
    /**
     * Get photos by camera model.
     */
    fun getPhotosByCameraModel(cameraModel: String): Flow<List<PhotoEntity>>
}
