package com.example.photozen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photozen.data.local.entity.PhotoLabelEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data class for label with count.
 */
data class LabelCount(
    val label: String,
    val count: Int
)

/**
 * Data Access Object for photo-label associations.
 * Optimized for fast aggregation queries on large datasets.
 */
@Dao
interface PhotoLabelDao {
    
    // ==================== INSERT ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photoLabel: PhotoLabelEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photoLabels: List<PhotoLabelEntity>)
    
    // ==================== DELETE ====================
    
    @Query("DELETE FROM photo_labels WHERE photoId = :photoId")
    suspend fun deleteByPhotoId(photoId: String)
    
    @Query("DELETE FROM photo_labels WHERE label = :label")
    suspend fun deleteByLabel(label: String)
    
    @Query("DELETE FROM photo_labels")
    suspend fun deleteAll()
    
    // ==================== AGGREGATION QUERIES (Optimized) ====================
    
    /**
     * Get all labels with their photo counts, ordered by count descending.
     * This uses SQL aggregation which is extremely fast even on 50k+ records.
     * 
     * Performance: ~10-50ms on 50k photos with proper indexing.
     */
    @Query("""
        SELECT label, COUNT(*) as count 
        FROM photo_labels 
        GROUP BY label 
        ORDER BY count DESC
    """)
    fun getAllLabelsWithCount(): Flow<List<LabelCount>>
    
    @Query("""
        SELECT label, COUNT(*) as count 
        FROM photo_labels 
        GROUP BY label 
        ORDER BY count DESC
    """)
    suspend fun getAllLabelsWithCountSync(): List<LabelCount>
    
    /**
     * Get top N labels by count.
     */
    @Query("""
        SELECT label, COUNT(*) as count 
        FROM photo_labels 
        GROUP BY label 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    fun getTopLabels(limit: Int): Flow<List<LabelCount>>
    
    @Query("""
        SELECT label, COUNT(*) as count 
        FROM photo_labels 
        GROUP BY label 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getTopLabelsSync(limit: Int): List<LabelCount>
    
    /**
     * Search labels by query string.
     */
    @Query("""
        SELECT label, COUNT(*) as count 
        FROM photo_labels 
        WHERE label LIKE :query
        GROUP BY label 
        ORDER BY count DESC
    """)
    fun searchLabels(query: String): Flow<List<LabelCount>>
    
    /**
     * Search labels by query string (sync version).
     */
    @Query("""
        SELECT label, COUNT(*) as count 
        FROM photo_labels 
        WHERE label LIKE :query
        GROUP BY label 
        ORDER BY count DESC
    """)
    suspend fun searchLabelsSync(query: String): List<LabelCount>
    
    /**
     * Get labels with count in a specific range.
     */
    @Query("""
        SELECT label, COUNT(*) as count 
        FROM photo_labels 
        GROUP BY label 
        HAVING count >= :minCount
        ORDER BY count DESC
    """)
    fun getLabelsWithMinCount(minCount: Int): Flow<List<LabelCount>>
    
    // ==================== PHOTO QUERIES ====================
    
    /**
     * Get photo IDs for a specific label.
     * Uses index on label column for fast lookup.
     */
    @Query("SELECT photoId FROM photo_labels WHERE label = :label")
    suspend fun getPhotoIdsByLabel(label: String): List<String>
    
    /**
     * Get photo IDs for a specific label (sync alias).
     */
    @Query("SELECT photoId FROM photo_labels WHERE label = :label")
    suspend fun getPhotoIdsByLabelSync(label: String): List<String>
    
    @Query("SELECT photoId FROM photo_labels WHERE label = :label")
    fun getPhotoIdsByLabelFlow(label: String): Flow<List<String>>
    
    /**
     * Get photo IDs for a label with pagination.
     */
    @Query("""
        SELECT photoId FROM photo_labels 
        WHERE label = :label 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPhotoIdsByLabelPaged(label: String, limit: Int, offset: Int): List<String>
    
    /**
     * Get count of photos with a specific label.
     */
    @Query("SELECT COUNT(*) FROM photo_labels WHERE label = :label")
    suspend fun getPhotoCountByLabel(label: String): Int
    
    @Query("SELECT COUNT(*) FROM photo_labels WHERE label = :label")
    fun getPhotoCountByLabelFlow(label: String): Flow<Int>
    
    // ==================== LABEL QUERIES ====================
    
    /**
     * Get all labels for a specific photo.
     */
    @Query("SELECT label FROM photo_labels WHERE photoId = :photoId ORDER BY confidence DESC")
    suspend fun getLabelsForPhoto(photoId: String): List<String>
    
    @Query("SELECT * FROM photo_labels WHERE photoId = :photoId ORDER BY confidence DESC")
    suspend fun getPhotoLabelsForPhoto(photoId: String): List<PhotoLabelEntity>
    
    /**
     * Check if a photo has a specific label.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM photo_labels WHERE photoId = :photoId AND label = :label)")
    suspend fun hasLabel(photoId: String, label: String): Boolean
    
    // ==================== STATS ====================
    
    /**
     * Get total number of unique labels.
     */
    @Query("SELECT COUNT(DISTINCT label) FROM photo_labels")
    suspend fun getUniqueLabelCount(): Int
    
    @Query("SELECT COUNT(DISTINCT label) FROM photo_labels")
    fun getUniqueLabelCountFlow(): Flow<Int>
    
    /**
     * Get total number of photo-label associations.
     */
    @Query("SELECT COUNT(*) FROM photo_labels")
    suspend fun getTotalAssociationCount(): Int
    
    /**
     * Get a sample photo ID for a label (for thumbnail preview).
     */
    @Query("""
        SELECT photoId FROM photo_labels 
        WHERE label = :label 
        ORDER BY confidence DESC 
        LIMIT 1
    """)
    suspend fun getSamplePhotoIdForLabel(label: String): String?
    
    /**
     * Get multiple sample photo IDs for a label.
     */
    @Query("""
        SELECT photoId FROM photo_labels 
        WHERE label = :label 
        ORDER BY confidence DESC 
        LIMIT :limit
    """)
    suspend fun getSamplePhotoIdsForLabel(label: String, limit: Int): List<String>
}
