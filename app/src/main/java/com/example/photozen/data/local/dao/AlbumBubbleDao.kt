package com.example.photozen.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AlbumBubbleEntity.
 * Provides all database operations for the album bubble list.
 */
@Dao
interface AlbumBubbleDao {
    
    // ==================== INSERT ====================
    
    /**
     * Insert a new album to the bubble list.
     * Uses REPLACE strategy to handle duplicates (update if exists).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: AlbumBubbleEntity)
    
    /**
     * Insert multiple albums to the bubble list.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<AlbumBubbleEntity>)
    
    // ==================== UPDATE ====================
    
    /**
     * Update an album entry.
     */
    @Update
    suspend fun update(album: AlbumBubbleEntity)
    
    /**
     * Update the sort order for a specific album.
     */
    @Query("UPDATE album_bubbles SET sort_order = :sortOrder WHERE bucket_id = :bucketId")
    suspend fun updateSortOrder(bucketId: String, sortOrder: Int)
    
    /**
     * Update display name (in case album is renamed in system).
     */
    @Query("UPDATE album_bubbles SET display_name = :displayName WHERE bucket_id = :bucketId")
    suspend fun updateDisplayName(bucketId: String, displayName: String)
    
    /**
     * Update bucket ID when the real MediaStore bucket ID is discovered.
     * This is needed because newly created albums get a placeholder bucket ID
     * until photos are actually added to them.
     */
    @Query("UPDATE album_bubbles SET bucket_id = :newBucketId WHERE bucket_id = :oldBucketId")
    suspend fun updateBucketId(oldBucketId: String, newBucketId: String)
    
    // ==================== DELETE ====================
    
    /**
     * Delete an album from the bubble list.
     */
    @Delete
    suspend fun delete(album: AlbumBubbleEntity)
    
    /**
     * Delete album by bucket ID.
     */
    @Query("DELETE FROM album_bubbles WHERE bucket_id = :bucketId")
    suspend fun deleteByBucketId(bucketId: String)
    
    /**
     * Delete all albums from the bubble list.
     */
    @Query("DELETE FROM album_bubbles")
    suspend fun deleteAll()
    
    // ==================== QUERIES ====================
    
    /**
     * Get all albums in the bubble list, ordered by sort order.
     */
    @Query("SELECT * FROM album_bubbles ORDER BY sort_order ASC, display_name ASC")
    fun getAll(): Flow<List<AlbumBubbleEntity>>
    
    /**
     * Get all albums synchronously (for one-time queries).
     */
    @Query("SELECT * FROM album_bubbles ORDER BY sort_order ASC, display_name ASC")
    suspend fun getAllSync(): List<AlbumBubbleEntity>
    
    /**
     * Get album by bucket ID.
     */
    @Query("SELECT * FROM album_bubbles WHERE bucket_id = :bucketId")
    suspend fun getByBucketId(bucketId: String): AlbumBubbleEntity?
    
    /**
     * Check if an album exists in the bubble list.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM album_bubbles WHERE bucket_id = :bucketId LIMIT 1)")
    suspend fun exists(bucketId: String): Boolean
    
    /**
     * Get the count of albums in the bubble list.
     */
    @Query("SELECT COUNT(*) FROM album_bubbles")
    fun getCount(): Flow<Int>
    
    /**
     * Get the maximum sort order value (for adding new albums).
     */
    @Query("SELECT MAX(sort_order) FROM album_bubbles")
    suspend fun getMaxSortOrder(): Int?
    
    /**
     * Get all bucket IDs in the bubble list.
     */
    @Query("SELECT bucket_id FROM album_bubbles")
    suspend fun getAllBucketIds(): List<String>
}
