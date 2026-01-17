package com.example.photozen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.photozen.data.local.entity.PhotoAnalysisEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for photo analysis operations.
 */
@Dao
interface PhotoAnalysisDao {
    
    // ==================== INSERT / UPDATE ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: PhotoAnalysisEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(analyses: List<PhotoAnalysisEntity>)
    
    @Update
    suspend fun update(analysis: PhotoAnalysisEntity)
    
    // ==================== DELETE ====================
    
    @Query("DELETE FROM photo_analysis WHERE photoId = :photoId")
    suspend fun deleteByPhotoId(photoId: String)
    
    @Query("DELETE FROM photo_analysis")
    suspend fun deleteAll()
    
    // ==================== QUERY - Single ====================
    
    @Query("SELECT * FROM photo_analysis WHERE photoId = :photoId")
    suspend fun getByPhotoId(photoId: String): PhotoAnalysisEntity?
    
    @Query("SELECT * FROM photo_analysis WHERE photoId = :photoId")
    fun getByPhotoIdFlow(photoId: String): Flow<PhotoAnalysisEntity?>
    
    // ==================== QUERY - Lists ====================
    
    @Query("SELECT * FROM photo_analysis ORDER BY analyzedAt DESC")
    fun getAllFlow(): Flow<List<PhotoAnalysisEntity>>
    
    @Query("SELECT * FROM photo_analysis ORDER BY analyzedAt DESC")
    suspend fun getAllSync(): List<PhotoAnalysisEntity>
    
    @Query("SELECT * FROM photo_analysis WHERE hasGps = 1 ORDER BY analyzedAt DESC")
    fun getWithGpsFlow(): Flow<List<PhotoAnalysisEntity>>
    
    @Query("SELECT * FROM photo_analysis WHERE faceCount > 0 ORDER BY analyzedAt DESC")
    fun getWithFacesFlow(): Flow<List<PhotoAnalysisEntity>>
    
    // ==================== QUERY - Labels ====================
    
    /**
     * Get photos containing a specific label.
     * Uses LIKE query on JSON array.
     */
    @Query("SELECT * FROM photo_analysis WHERE labels LIKE '%' || :label || '%' ORDER BY analyzedAt DESC")
    fun getByLabel(label: String): Flow<List<PhotoAnalysisEntity>>
    
    /**
     * Get photos by primary category.
     */
    @Query("SELECT * FROM photo_analysis WHERE primaryCategory = :category ORDER BY primaryCategoryConfidence DESC")
    fun getByCategory(category: String): Flow<List<PhotoAnalysisEntity>>
    
    // ==================== QUERY - GPS ====================
    
    /**
     * Get photos within a geographic bounding box.
     */
    @Query("""
        SELECT * FROM photo_analysis 
        WHERE hasGps = 1 
        AND latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLng AND :maxLng
        ORDER BY analyzedAt DESC
    """)
    fun getInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<PhotoAnalysisEntity>>
    
    // ==================== QUERY - Counts ====================
    
    @Query("SELECT COUNT(*) FROM photo_analysis")
    fun getAnalyzedCountFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM photo_analysis")
    suspend fun getAnalyzedCount(): Int
    
    @Query("SELECT COUNT(*) FROM photo_analysis WHERE hasGps = 1")
    suspend fun getWithGpsCount(): Int
    
    @Query("SELECT COUNT(*) FROM photo_analysis WHERE faceCount > 0")
    suspend fun getWithFacesCount(): Int
    
    // ==================== QUERY - Distinct Labels ====================
    
    /**
     * Get all unique labels across all photos.
     * Returns raw JSON strings that need to be parsed.
     */
    @Query("SELECT DISTINCT labels FROM photo_analysis WHERE labels != '[]'")
    suspend fun getAllLabelsRaw(): List<String>
    
    /**
     * Get all unique primary categories.
     */
    @Query("SELECT DISTINCT primaryCategory FROM photo_analysis WHERE primaryCategory IS NOT NULL")
    suspend fun getAllCategories(): List<String>
    
    // ==================== QUERY - Unanalyzed Photos ====================
    
    /**
     * Get IDs of photos that haven't been analyzed yet.
     */
    @Query("""
        SELECT p.id FROM photos p 
        LEFT JOIN photo_analysis pa ON p.id = pa.photoId 
        WHERE pa.photoId IS NULL
        LIMIT :limit
    """)
    suspend fun getUnanalyzedPhotoIds(limit: Int = 100): List<String>
    
    /**
     * Get count of unanalyzed photos.
     */
    @Query("""
        SELECT COUNT(*) FROM photos p 
        LEFT JOIN photo_analysis pa ON p.id = pa.photoId 
        WHERE pa.photoId IS NULL
    """)
    suspend fun getUnanalyzedCount(): Int
    
    @Query("""
        SELECT COUNT(*) FROM photos p 
        LEFT JOIN photo_analysis pa ON p.id = pa.photoId 
        WHERE pa.photoId IS NULL
    """)
    fun getUnanalyzedCountFlow(): Flow<Int>
    
    // ==================== QUERY - Embedding ====================
    
    /**
     * Get all photos that have embeddings generated.
     */
    @Query("SELECT * FROM photo_analysis WHERE embedding IS NOT NULL")
    suspend fun getAllWithEmbedding(): List<PhotoAnalysisEntity>
    
    /**
     * Get count of photos with embeddings.
     */
    @Query("SELECT COUNT(*) FROM photo_analysis WHERE embedding IS NOT NULL")
    suspend fun getEmbeddingCount(): Int
    
    /**
     * Get count of photos with embeddings as Flow.
     */
    @Query("SELECT COUNT(*) FROM photo_analysis WHERE embedding IS NOT NULL")
    fun getEmbeddingCountFlow(): Flow<Int>
    
    /**
     * Get photos without embeddings (for background processing).
     */
    @Query("""
        SELECT pa.* FROM photo_analysis pa 
        WHERE pa.embedding IS NULL
        LIMIT :limit
    """)
    suspend fun getPhotosWithoutEmbedding(limit: Int = 100): List<PhotoAnalysisEntity>
    
    // ==================== QUERY - pHash / Duplicate Detection ====================
    
    /**
     * Get all photos that have pHash calculated.
     */
    @Query("SELECT * FROM photo_analysis WHERE phash IS NOT NULL")
    suspend fun getPhotosWithPhashSync(): List<PhotoAnalysisEntity>
    
    /**
     * Get photos without pHash (for background processing).
     */
    @Query("SELECT * FROM photo_analysis WHERE phash IS NULL LIMIT :limit")
    suspend fun getPhotosWithoutPhashSync(limit: Int = 100): List<PhotoAnalysisEntity>
    
    /**
     * Update pHash and quality analysis fields.
     */
    @Query("""
        UPDATE photo_analysis SET 
            phash = :phash,
            dominantColor = :dominantColor,
            accentColor = :accentColor,
            luminance = :luminance,
            chroma = :chroma,
            quality = :quality,
            sharpness = :sharpness,
            aspectRatio = :aspectRatio
        WHERE photoId = :photoId
    """)
    suspend fun updatePhotoAnalysis(
        photoId: String,
        phash: String?,
        dominantColor: String?,
        accentColor: String?,
        luminance: Int,
        chroma: Int,
        quality: Int,
        sharpness: Int,
        aspectRatio: Float
    )
    
    /**
     * Get count of photos with pHash.
     */
    @Query("SELECT COUNT(*) FROM photo_analysis WHERE phash IS NOT NULL")
    suspend fun getPhashCount(): Int
    
    /**
     * Get count of photos with pHash as Flow.
     */
    @Query("SELECT COUNT(*) FROM photo_analysis WHERE phash IS NOT NULL")
    fun getPhashCountFlow(): Flow<Int>
}
