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
     * Delete multiple photos by IDs.
     */
    @Query("DELETE FROM photos WHERE id IN (:photoIds)")
    suspend fun deleteByIds(photoIds: List<String>)
    
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
    
    /**
     * Get a random unsorted photo.
     * Uses ORDER BY RANDOM() LIMIT 1.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnsortedPhoto(): PhotoEntity?
    
    /**
     * Get a random unsorted photo filtered by bucket IDs.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND bucket_id IN (:bucketIds) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnsortedPhotoByBuckets(bucketIds: List<String>): PhotoEntity?
    
    /**
     * Get a random unsorted photo filtered by date range.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND date_added >= :startDate AND date_added <= :endDate ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnsortedPhotoByDateRange(startDate: Long, endDate: Long): PhotoEntity?
    
    /**
     * Get a random unsorted photo filtered by bucket IDs and date range.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND bucket_id IN (:bucketIds) AND date_added >= :startDate AND date_added <= :endDate ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnsortedPhotoByBucketsAndDateRange(bucketIds: List<String>, startDate: Long, endDate: Long): PhotoEntity?
    
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
     * LIMITED to 500 to prevent CursorWindow overflow on devices with many photos.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 ORDER BY date_added DESC LIMIT 500")
    fun getUnsortedPhotos(): Flow<List<PhotoEntity>>
    
    /**
     * Get all unsorted photos sorted by date ascending.
     * LIMITED to 500 to prevent CursorWindow overflow on devices with many photos.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 ORDER BY date_added ASC LIMIT 500")
    fun getUnsortedPhotosAsc(): Flow<List<PhotoEntity>>
    
    /**
     * Get unsorted photos filtered by bucket IDs (for camera only / exclude camera).
     * LIMITED to 500 to prevent CursorWindow overflow.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND bucket_id IN (:bucketIds) ORDER BY date_added DESC LIMIT 500")
    fun getUnsortedPhotosByBuckets(bucketIds: List<String>): Flow<List<PhotoEntity>>
    
    /**
     * Get unsorted photos filtered by bucket IDs, sorted by date ascending.
     * LIMITED to 500 to prevent CursorWindow overflow.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND bucket_id IN (:bucketIds) ORDER BY date_added ASC LIMIT 500")
    fun getUnsortedPhotosByBucketsAsc(bucketIds: List<String>): Flow<List<PhotoEntity>>
    
    /**
     * Get unsorted photos excluding specific bucket IDs.
     * LIMITED to 500 to prevent CursorWindow overflow.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND (bucket_id NOT IN (:bucketIds) OR bucket_id IS NULL) ORDER BY date_added DESC LIMIT 500")
    fun getUnsortedPhotosExcludingBuckets(bucketIds: List<String>): Flow<List<PhotoEntity>>
    
    /**
     * Get unsorted photos excluding specific bucket IDs, sorted by date ascending.
     * LIMITED to 500 to prevent CursorWindow overflow.
     */
    @Query("SELECT * FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND (bucket_id NOT IN (:bucketIds) OR bucket_id IS NULL) ORDER BY date_added ASC LIMIT 500")
    fun getUnsortedPhotosExcludingBucketsAsc(bucketIds: List<String>): Flow<List<PhotoEntity>>
    
    /**
     * Get unsorted photos filtered by bucket IDs and date range.
     * LIMITED to 500 to prevent CursorWindow overflow.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' 
        AND is_virtual_copy = 0 
        AND (:bucketIds IS NULL OR bucket_id IN (:bucketIds))
        AND (:startDate IS NULL OR date_added >= :startDate)
        AND (:endDate IS NULL OR date_added <= :endDate)
        ORDER BY date_added DESC
        LIMIT 500
    """)
    fun getUnsortedPhotosFiltered(
        bucketIds: List<String>?,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<PhotoEntity>>
    
    // ==================== ID LIST QUERY (FOR MEMORY SHUFFLE PAGINATION) ====================
    
    /**
     * Get all unsorted photo IDs.
     * Ordered by date_added DESC for consistent snapshot pagination.
     */
    @Query("SELECT id FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 ORDER BY date_added DESC")
    suspend fun getUnsortedPhotoIds(): List<String>
    
    /**
     * Get unsorted photo IDs filtered by buckets.
     * Ordered by date_added DESC.
     */
    @Query("SELECT id FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND bucket_id IN (:bucketIds) ORDER BY date_added DESC")
    suspend fun getUnsortedPhotoIdsByBuckets(bucketIds: List<String>): List<String>
    
    /**
     * Get unsorted photo IDs excluding buckets.
     * Ordered by date_added DESC.
     */
    @Query("SELECT id FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND (bucket_id NOT IN (:bucketIds) OR bucket_id IS NULL) ORDER BY date_added DESC")
    suspend fun getUnsortedPhotoIdsExcludingBuckets(bucketIds: List<String>): List<String>
    
    /**
     * Get unsorted photo IDs filtered by buckets and date range.
     * Ordered by date_added DESC.
     */
    @Query("""
        SELECT id FROM photos 
        WHERE status = 'UNSORTED' 
        AND is_virtual_copy = 0 
        AND (:bucketIds IS NULL OR bucket_id IN (:bucketIds))
        AND (:startDate IS NULL OR date_added >= :startDate)
        AND (:endDate IS NULL OR date_added <= :endDate)
        ORDER BY date_added DESC
    """)
    suspend fun getUnsortedPhotoIdsFiltered(
        bucketIds: List<String>?,
        startDate: Long?,
        endDate: Long?
    ): List<String>
    
    /**
     * Get photos by ID list.
     * Note: SQL IN clause does not guarantee order, so sorting must be done in memory.
     */
    @Query("SELECT * FROM photos WHERE id IN (:ids)")
    suspend fun getPhotosByIds(ids: List<String>): List<PhotoEntity>

    // ==================== PAGED QUERIES FOR FLOW SORTER ====================
    // These queries apply ORDER BY to ALL matching photos, then paginate.
    // This ensures correct sorting across the entire dataset, not just within a page.
    
    /**
     * Get unsorted photos with pagination - Date Descending.
     * ORDER BY is applied to ALL unsorted photos, then paginated via LIMIT/OFFSET.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        ORDER BY date_added DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosPagedDesc(limit: Int, offset: Int): List<PhotoEntity>
    
    /**
     * Get unsorted photos with pagination - Date Ascending.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        ORDER BY date_added ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosPagedAsc(limit: Int, offset: Int): List<PhotoEntity>
    
    /**
     * Get unsorted photos with pagination - Random order using seeded pseudo-random.
     * Uses (CAST(SUBSTR(id, 1, 8) AS INTEGER) * seed) % prime for consistent random ordering.
     * The seed ensures the same random order when fetching subsequent pages.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        ORDER BY (ABS(CAST(SUBSTR(id, 1, 8) AS INTEGER)) * :seed) % 2147483647
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosPagedRandom(seed: Long, limit: Int, offset: Int): List<PhotoEntity>
    
    /**
     * Get unsorted photos filtered by bucket IDs with pagination - Date Descending.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND bucket_id IN (:bucketIds)
        ORDER BY date_added DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosByBucketsPagedDesc(bucketIds: List<String>, limit: Int, offset: Int): List<PhotoEntity>
    
    /**
     * Get unsorted photos filtered by bucket IDs with pagination - Date Ascending.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND bucket_id IN (:bucketIds)
        ORDER BY date_added ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosByBucketsPagedAsc(bucketIds: List<String>, limit: Int, offset: Int): List<PhotoEntity>
    
    /**
     * Get unsorted photos filtered by bucket IDs with pagination - Random order.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND bucket_id IN (:bucketIds)
        ORDER BY (ABS(CAST(SUBSTR(id, 1, 8) AS INTEGER)) * :seed) % 2147483647
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosByBucketsPagedRandom(bucketIds: List<String>, seed: Long, limit: Int, offset: Int): List<PhotoEntity>
    
    /**
     * Get unsorted photos excluding bucket IDs with pagination - Date Descending.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND (bucket_id NOT IN (:bucketIds) OR bucket_id IS NULL)
        ORDER BY date_added DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosExcludingBucketsPagedDesc(bucketIds: List<String>, limit: Int, offset: Int): List<PhotoEntity>
    
    /**
     * Get unsorted photos excluding bucket IDs with pagination - Date Ascending.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND (bucket_id NOT IN (:bucketIds) OR bucket_id IS NULL)
        ORDER BY date_added ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosExcludingBucketsPagedAsc(bucketIds: List<String>, limit: Int, offset: Int): List<PhotoEntity>
    
    /**
     * Get unsorted photos excluding bucket IDs with pagination - Random order.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND (bucket_id NOT IN (:bucketIds) OR bucket_id IS NULL)
        ORDER BY (ABS(CAST(SUBSTR(id, 1, 8) AS INTEGER)) * :seed) % 2147483647
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosExcludingBucketsPagedRandom(bucketIds: List<String>, seed: Long, limit: Int, offset: Int): List<PhotoEntity>
    
    /**
     * Get unsorted photos filtered by bucket IDs (optional) and date range (optional) with pagination - Date Descending.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND (:bucketIds IS NULL OR bucket_id IN (:bucketIds))
        AND (:startDate IS NULL OR date_added >= :startDate)
        AND (:endDate IS NULL OR date_added <= :endDate)
        ORDER BY date_added DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosFilteredPagedDesc(
        bucketIds: List<String>?,
        startDate: Long?,
        endDate: Long?,
        limit: Int, 
        offset: Int
    ): List<PhotoEntity>
    
    /**
     * Get unsorted photos filtered by bucket IDs (optional) and date range (optional) with pagination - Date Ascending.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND (:bucketIds IS NULL OR bucket_id IN (:bucketIds))
        AND (:startDate IS NULL OR date_added >= :startDate)
        AND (:endDate IS NULL OR date_added <= :endDate)
        ORDER BY date_added ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosFilteredPagedAsc(
        bucketIds: List<String>?,
        startDate: Long?,
        endDate: Long?,
        limit: Int, 
        offset: Int
    ): List<PhotoEntity>
    
    /**
     * Get unsorted photos filtered by bucket IDs (optional) and date range (optional) with pagination - Random order.
     */
    @Query("""
        SELECT * FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND (:bucketIds IS NULL OR bucket_id IN (:bucketIds))
        AND (:startDate IS NULL OR date_added >= :startDate)
        AND (:endDate IS NULL OR date_added <= :endDate)
        ORDER BY (ABS(CAST(SUBSTR(id, 1, 8) AS INTEGER)) * :seed) % 2147483647
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getUnsortedPhotosFilteredPagedRandom(
        bucketIds: List<String>?,
        startDate: Long?,
        endDate: Long?,
        seed: Long,
        limit: Int, 
        offset: Int
    ): List<PhotoEntity>
    
    /**
     * Get count of unsorted photos filtered by bucket IDs (optional) and date range (optional).
     */
    @Query("""
        SELECT COUNT(*) FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND (:bucketIds IS NULL OR bucket_id IN (:bucketIds))
        AND (:startDate IS NULL OR date_added >= :startDate)
        AND (:endDate IS NULL OR date_added <= :endDate)
    """)
    fun getUnsortedCountFiltered(
        bucketIds: List<String>?,
        startDate: Long?,
        endDate: Long?
    ): Flow<Int>
    
    /**
     * Get count of unsorted photos filtered by bucket IDs (optional) and date range (optional).
     * Suspend version for one-time queries.
     */
    @Query("""
        SELECT COUNT(*) FROM photos 
        WHERE status = 'UNSORTED' AND is_virtual_copy = 0 
        AND (:bucketIds IS NULL OR bucket_id IN (:bucketIds))
        AND (:startDate IS NULL OR date_added >= :startDate)
        AND (:endDate IS NULL OR date_added <= :endDate)
    """)
    suspend fun getUnsortedCountFilteredSync(
        bucketIds: List<String>?,
        startDate: Long?,
        endDate: Long?
    ): Int

    /**
     * Get all camera bucket IDs from our database.
     */
    @Query("SELECT DISTINCT bucket_id FROM photos WHERE bucket_id IS NOT NULL")
    suspend fun getAllBucketIds(): List<String>
    
    /**
     * Get photos that need bucket_id update (null bucket_id, non-virtual copies).
     */
    @Query("SELECT * FROM photos WHERE bucket_id IS NULL AND is_virtual_copy = 0")
    suspend fun getPhotosWithNullBucketId(): List<PhotoEntity>
    
    /**
     * Update bucket_id for a photo.
     */
    @Query("UPDATE photos SET bucket_id = :bucketId WHERE id = :photoId")
    suspend fun updateBucketId(photoId: String, bucketId: String)
    
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
     * Get count of all photos (synchronous version for background tasks).
     */
    @Query("SELECT COUNT(*) FROM photos WHERE is_virtual_copy = 0")
    suspend fun getPhotoCount(): Int
    
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
    
    /**
     * Get count of unsorted photos filtered by bucket IDs (for camera only mode).
     */
    @Query("SELECT COUNT(*) FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND bucket_id IN (:bucketIds)")
    fun getUnsortedCountByBuckets(bucketIds: List<String>): Flow<Int>
    
    /**
     * Get count of unsorted photos excluding specific bucket IDs (for exclude camera mode).
     */
    @Query("SELECT COUNT(*) FROM photos WHERE status = 'UNSORTED' AND is_virtual_copy = 0 AND (bucket_id NOT IN (:bucketIds) OR bucket_id IS NULL)")
    fun getUnsortedCountExcludingBuckets(bucketIds: List<String>): Flow<Int>
    
    /**
     * Get total count of photos filtered by bucket IDs (for camera only mode).
     */
    @Query("SELECT COUNT(*) FROM photos WHERE is_virtual_copy = 0 AND bucket_id IN (:bucketIds)")
    fun getTotalCountByBuckets(bucketIds: List<String>): Flow<Int>
    
    /**
     * Get total count of photos excluding specific bucket IDs (for exclude camera mode).
     */
    @Query("SELECT COUNT(*) FROM photos WHERE is_virtual_copy = 0 AND (bucket_id NOT IN (:bucketIds) OR bucket_id IS NULL)")
    fun getTotalCountExcludingBuckets(bucketIds: List<String>): Flow<Int>
    
    /**
     * Get all photos in a specific album (bucket).
     */
    @Query("SELECT * FROM photos WHERE bucket_id = :bucketId AND is_virtual_copy = 0 ORDER BY date_added DESC")
    fun getPhotosByBucketId(bucketId: String): Flow<List<PhotoEntity>>
    
    /**
     * Get all photos in a specific album (bucket) - synchronous version.
     */
    @Query("SELECT * FROM photos WHERE bucket_id = :bucketId AND is_virtual_copy = 0 ORDER BY date_added DESC")
    suspend fun getPhotosByBucketIdSync(bucketId: String): List<PhotoEntity>
    
    /**
     * Get sorted count of photos filtered by bucket IDs.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE status != 'UNSORTED' AND is_virtual_copy = 0 AND bucket_id IN (:bucketIds)")
    fun getSortedCountByBuckets(bucketIds: List<String>): Flow<Int>
    
    /**
     * Get sorted count of photos excluding specific bucket IDs.
     */
    @Query("SELECT COUNT(*) FROM photos WHERE status != 'UNSORTED' AND is_virtual_copy = 0 AND (bucket_id NOT IN (:bucketIds) OR bucket_id IS NULL)")
    fun getSortedCountExcludingBuckets(bucketIds: List<String>): Flow<Int>
    
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
    
    /**
     * Get all photo IDs that are not virtual copies.
     * Used for detecting externally deleted photos.
     */
    @Query("SELECT id FROM photos WHERE is_virtual_copy = 0")
    suspend fun getAllNonVirtualPhotoIds(): List<String>
    
    /**
     * Delete all virtual copies of a parent photo.
     */
    @Query("DELETE FROM photos WHERE parent_id = :parentId")
    suspend fun deleteVirtualCopiesByParentId(parentId: String)
    
    // ==================== QUERY - Tag Related ====================
    
    /**
     * Get all photos that have a specific tag.
     * This is a reactive query that will emit new values when:
     * - Photos are added/removed from the tag
     * - Photo entities are updated/deleted
     */
    @Query("""
        SELECT p.* FROM photos p
        INNER JOIN photo_tag_cross_ref ptc ON p.id = ptc.photo_id
        WHERE ptc.tag_id = :tagId AND p.is_virtual_copy = 0
        ORDER BY p.date_added DESC
    """)
    fun getPhotosByTagId(tagId: String): Flow<List<PhotoEntity>>
    
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
