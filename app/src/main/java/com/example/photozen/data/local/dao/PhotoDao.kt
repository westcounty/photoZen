package com.example.photozen.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.PhotoWithTags
import com.example.photozen.data.model.PhotoStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PhotoEntity.
 * Provides all database operations for photos.
 */
@Dao
interface PhotoDao {
    
    // ==================== INSERT ====================
    
    /**
     * Insert a single photo. Replace if exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity)
    
    /**
     * Insert multiple photos. Replace if exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)
    
    /**
     * Upsert (Insert or Update) a photo.
     */
    @Upsert
    suspend fun upsert(photo: PhotoEntity)
    
    /**
     * Upsert multiple photos.
     */
    @Upsert
    suspend fun upsertAll(photos: List<PhotoEntity>)
    
    // ==================== UPDATE ====================
    
    /**
     * Update a single photo.
     */
    @Update
    suspend fun update(photo: PhotoEntity)
    
    /**
     * Update photo status by ID.
     */
    @Query("UPDATE photos SET status = :status, updated_at = :updatedAt WHERE id = :photoId")
    suspend fun updateStatus(photoId: String, status: PhotoStatus, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * Batch update status for multiple photos.
     */
    @Query("UPDATE photos SET status = :status, updated_at = :updatedAt WHERE id IN (:photoIds)")
    suspend fun updateStatusBatch(photoIds: List<String>, status: PhotoStatus, updatedAt: Long = System.currentTimeMillis())
    
    // ==================== DELETE ====================
    
    /**
     * Delete a single photo.
     */
    @Delete
    suspend fun delete(photo: PhotoEntity)
    
    /**
     * Delete photo by ID.
     */
    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deleteById(photoId: String)
    
    /**
     * Delete all photos with TRASH status (for "Empty Trash" feature).
     */
    @Query("DELETE FROM photos WHERE status = 'TRASH'")
    suspend fun deleteAllTrashed()
    
    /**
     * Delete all photos (for testing/reset).
     */
    @Query("DELETE FROM photos")
    suspend fun deleteAll()
    
    // ==================== QUERY - Single ====================
    
    /**
     * Get photo by ID.
     */
    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getById(photoId: String): PhotoEntity?
    
    /**
     * Get photo by ID as Flow for reactive updates.
     */
    @Query("SELECT * FROM photos WHERE id = :photoId")
    fun getByIdFlow(photoId: String): Flow<PhotoEntity?>
    
    /**
     * Get photo with tags by ID.
     */
    @Transaction
    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getWithTagsById(photoId: String): PhotoWithTags?
    
    // ==================== QUERY - Lists ====================
    
    /**
     * Get all photos ordered by date added (newest first).
     */
    @Query("SELECT * FROM photos WHERE is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get all photos with specific status.
     */
    @Query("SELECT * FROM photos WHERE status = :status AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getPhotosByStatus(status: PhotoStatus): Flow<List<PhotoEntity>>
    
    /**
     * Get all unsorted photos for Flow Sorter.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getUnsortedPhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get all "MAYBE" photos for Light Table comparison.
     */
    @Query("SELECT * FROM photos WHERE status = 'MAYBE' AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getMaybePhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get all "KEEP" photos.
     */
    @Query("SELECT * FROM photos WHERE status = 'KEEP' AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getKeepPhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get all "TRASH" photos.
     */
    @Query("SELECT * FROM photos WHERE status = 'TRASH' AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getTrashPhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get all virtual copies of a photo.
     */
    @Query("SELECT * FROM photos WHERE parent_id = :parentId ORDER BY created_at ASC")
    fun getVirtualCopies(parentId: String): Flow<List<PhotoEntity>>
    
    // ==================== QUERY - Paging ====================
    
    /**
     * Get all photos as PagingSource for efficient large list loading.
     */
    @Query("SELECT * FROM photos WHERE is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getAllPhotosPaged(): PagingSource<Int, PhotoEntity>
    
    /**
     * Get unsorted photos as PagingSource.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getUnsortedPhotosPaged(): PagingSource<Int, PhotoEntity>
    
    /**
     * Get photos by status as PagingSource.
     */
    @Query("SELECT * FROM photos WHERE status = :status AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getPhotosByStatusPaged(status: PhotoStatus): PagingSource<Int, PhotoEntity>
    
    // ==================== QUERY - Counts ====================
    
    /**
     * Get count of all photos.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE is_virtual_copy = 0")
    fun getTotalCount(): Flow<Int>
    
    /**
     * Get count of photos by status.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE status = :status AND is_virtual_copy = 0")
    fun getCountByStatus(status: PhotoStatus): Flow<Int>
    
    /**
     * Get count of unsorted photos.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0")
    fun getUnsortedCount(): Flow<Int>
    
    // ==================== QUERY - Search ====================
    
    /**
     * Search photos by display name.
     */
    @Query("SELECT * FROM photos WHERE display_name LIKE '%' || :query || '%' AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun searchByName(query: String): Flow<List<PhotoEntity>>
    
    /**
     * Get photos by camera model (for Camera Collection feature).
     */
    @Query("SELECT * FROM photos WHERE camera_model = :cameraModel AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getPhotosByCameraModel(cameraModel: String): Flow<List<PhotoEntity>>
    
    /**
     * Get distinct camera models (for Camera Collection achievement).
     */
    @Query("SELECT DISTINCT camera_model FROM photos WHERE camera_model IS NOT NULL AND is_virtual_copy = 0")
    fun getDistinctCameraModels(): Flow<List<String>>
    
    // ==================== QUERY - Sync ====================
    
    /**
     * Check if photo with system URI exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM photos WHERE system_uri = :systemUri LIMIT 1)")
    suspend fun existsBySystemUri(systemUri: String): Boolean
    
    /**
     * Get all system URIs for sync comparison.
     */
    @Query("SELECT system_uri FROM photos WHERE is_virtual_copy = 0")
    suspend fun getAllSystemUris(): List<String>
    
    // ==================== QUERY - GPS/Location ====================
    
    /**
     * Get photos that haven't been scanned for GPS data yet.
     * Limited to avoid memory issues with large batches.
     */
    @Query("SELECT * FROM photos WHERE gps_scanned = 0 AND is_virtual_copy = 0 LIMIT :limit")
    suspend fun getPhotosNeedingGpsScan(limit: Int = 100): List<PhotoEntity>
    
    /**
     * Update GPS coordinates for a photo.
     */
    @Query("""
        UPDATE photos 
        SET latitude = :latitude, longitude = :longitude, gps_scanned = 1, updated_at = :updatedAt 
        WHERE id = :photoId
    """)
    suspend fun updateGpsLocation(
        photoId: String, 
        latitude: Double?, 
        longitude: Double?,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Mark photo as GPS scanned (even if no GPS data found).
     */
    @Query("UPDATE photos SET gps_scanned = 1, updated_at = :updatedAt WHERE id = :photoId")
    suspend fun markGpsScanned(photoId: String, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * Get photos with GPS coordinates, sorted by date taken.
     * Used for trajectory map visualization.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND is_virtual_copy = 0 
        ORDER BY date_taken ASC
    """)
    fun getPhotosWithGps(): Flow<List<PhotoEntity>>
    
    /**
     * Get count of photos with GPS data.
     */
    @Query("""
        SELECT COUNT(*) FROM photos 
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND is_virtual_copy = 0
    """)
    fun getPhotosWithGpsCount(): Flow<Int>
    
    /**
     * Get count of photos pending GPS scan.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE gps_scanned = 0 AND is_virtual_copy = 0")
    fun getPendingGpsScanCount(): Flow<Int>
    
    /**
     * Get count of photos pending GPS scan (synchronous version for Worker).
     */
    @Query("SELECT COUNT(*) FROM photos WHERE gps_scanned = 0 AND is_virtual_copy = 0")
    suspend fun getPendingGpsScanCountSync(): Int
}
