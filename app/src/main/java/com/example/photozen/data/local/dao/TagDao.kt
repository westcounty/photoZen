package com.example.photozen.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.photozen.data.local.entity.PhotoTagCrossRef
import com.example.photozen.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TagEntity and PhotoTagCrossRef.
 * Provides all database operations for tags and photo-tag relationships.
 */
@Dao
interface TagDao {
    
    // ==================== TAG CRUD ====================
    
    /**
     * Insert a new tag.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity)
    
    /**
     * Insert multiple tags.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)
    
    /**
     * Update a tag.
     */
    @Update
    suspend fun update(tag: TagEntity)
    
    /**
     * Delete a tag.
     */
    @Delete
    suspend fun delete(tag: TagEntity)
    
    /**
     * Delete tag by ID.
     */
    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteById(tagId: String)
    
    // ==================== TAG QUERIES ====================
    
    /**
     * Get tag by ID.
     */
    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getById(tagId: String): TagEntity?
    
    /**
     * Get all tags ordered by sort order.
     */
    @Query("SELECT * FROM tags ORDER BY sort_order ASC, name ASC")
    fun getAllTags(): Flow<List<TagEntity>>
    
    /**
     * Get root-level tags (no parent).
     */
    @Query("SELECT * FROM tags WHERE parent_id IS NULL ORDER BY sort_order ASC, name ASC")
    fun getRootTags(): Flow<List<TagEntity>>
    
    /**
     * Get child tags of a parent tag.
     */
    @Query("SELECT * FROM tags WHERE parent_id = :parentId ORDER BY sort_order ASC, name ASC")
    fun getChildTags(parentId: String): Flow<List<TagEntity>>
    
    /**
     * Search tags by name.
     */
    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTags(query: String): Flow<List<TagEntity>>
    
    // ==================== PHOTO-TAG RELATIONSHIP ====================
    
    /**
     * Add tag to a photo.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToPhoto(crossRef: PhotoTagCrossRef)
    
    /**
     * Remove tag from a photo.
     */
    @Query("DELETE FROM photo_tag_cross_ref WHERE photo_id = :photoId AND tag_id = :tagId")
    suspend fun removeTagFromPhoto(photoId: String, tagId: String)
    
    /**
     * Remove all tags from a photo.
     */
    @Query("DELETE FROM photo_tag_cross_ref WHERE photo_id = :photoId")
    suspend fun removeAllTagsFromPhoto(photoId: String)
    
    /**
     * Get all tags for a specific photo.
     */
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN photo_tag_cross_ref ptc ON t.id = ptc.tag_id
        WHERE ptc.photo_id = :photoId
        ORDER BY t.sort_order ASC, t.name ASC
    """)
    fun getTagsForPhoto(photoId: String): Flow<List<TagEntity>>
    
    /**
     * Get all photo IDs that have a specific tag.
     */
    @Query("SELECT photo_id FROM photo_tag_cross_ref WHERE tag_id = :tagId")
    fun getPhotoIdsWithTag(tagId: String): Flow<List<String>>
    
    /**
     * Get count of photos with a specific tag.
     */
    @Query("SELECT COUNT(*) FROM photo_tag_cross_ref WHERE tag_id = :tagId")
    fun getPhotoCountForTag(tagId: String): Flow<Int>
    
    /**
     * Check if a photo has a specific tag.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM photo_tag_cross_ref WHERE photo_id = :photoId AND tag_id = :tagId LIMIT 1)")
    suspend fun photoHasTag(photoId: String, tagId: String): Boolean
}
