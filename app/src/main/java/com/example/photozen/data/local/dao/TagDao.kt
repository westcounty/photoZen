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
    
    // ==================== BUBBLE GRAPH QUERIES ====================
    
    /**
     * Get all tags with their photo counts for bubble graph.
     */
    @Query("""
        SELECT t.*, COUNT(ptc.photo_id) as photo_count
        FROM tags t
        LEFT JOIN photo_tag_cross_ref ptc ON t.id = ptc.tag_id
        GROUP BY t.id
        ORDER BY photo_count DESC, t.name ASC
    """)
    fun getTagsWithPhotoCount(): Flow<List<TagWithCount>>
    
    /**
     * Get root tags with their photo counts.
     */
    @Query("""
        SELECT t.*, COUNT(ptc.photo_id) as photo_count
        FROM tags t
        LEFT JOIN photo_tag_cross_ref ptc ON t.id = ptc.tag_id
        WHERE t.parent_id IS NULL
        GROUP BY t.id
        ORDER BY photo_count DESC, t.name ASC
    """)
    fun getRootTagsWithPhotoCount(): Flow<List<TagWithCount>>
    
    /**
     * Get child tags with their photo counts.
     */
    @Query("""
        SELECT t.*, COUNT(ptc.photo_id) as photo_count
        FROM tags t
        LEFT JOIN photo_tag_cross_ref ptc ON t.id = ptc.tag_id
        WHERE t.parent_id = :parentId
        GROUP BY t.id
        ORDER BY photo_count DESC, t.name ASC
    """)
    fun getChildTagsWithPhotoCount(parentId: String): Flow<List<TagWithCount>>
    
    /**
     * Get total photo count for a tag including all descendants.
     */
    @Query("SELECT COUNT(DISTINCT ptc.photo_id) FROM photo_tag_cross_ref ptc WHERE ptc.tag_id = :tagId")
    suspend fun getTotalPhotoCountForTag(tagId: String): Int
    
    /**
     * Check if a tag has any child tags.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tags WHERE parent_id = :tagId LIMIT 1)")
    suspend fun hasChildTags(tagId: String): Boolean
    
    /**
     * Get count of child tags for a parent.
     */
    @Query("SELECT COUNT(*) FROM tags WHERE parent_id = :tagId")
    suspend fun getChildTagCount(tagId: String): Int
}

/**
 * Data class for tags with their photo counts.
 */
data class TagWithCount(
    @androidx.room.ColumnInfo(name = "id")
    val id: String,
    @androidx.room.ColumnInfo(name = "name")
    val name: String,
    @androidx.room.ColumnInfo(name = "parent_id")
    val parentId: String?,
    @androidx.room.ColumnInfo(name = "color")
    val color: Int,
    @androidx.room.ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @androidx.room.ColumnInfo(name = "created_at")
    val createdAt: Long,
    @androidx.room.ColumnInfo(name = "photo_count")
    val photoCount: Int
) {
    fun toTagEntity() = TagEntity(
        id = id,
        name = name,
        parentId = parentId,
        color = color,
        sortOrder = sortOrder,
        createdAt = createdAt
    )
}
