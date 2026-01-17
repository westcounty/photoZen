package com.example.photozen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing an album in the user's album bubble list.
 * 
 * This tracks which system albums the user has added to their album bubble view
 * for quick classification and management.
 * 
 * Note: This is different from the system MediaStore albums - this only stores
 * references to albums that the user has explicitly added to their bubble list.
 */
@Entity(tableName = "album_bubbles")
data class AlbumBubbleEntity(
    /**
     * MediaStore bucket ID of the album.
     * This is the primary key since each album can only be added once.
     */
    @PrimaryKey
    @ColumnInfo(name = "bucket_id")
    val bucketId: String,
    
    /**
     * Display name of the album.
     * Cached from MediaStore for quick access.
     */
    @ColumnInfo(name = "display_name")
    val displayName: String,
    
    /**
     * User-defined sort order for display in bubble view.
     * Lower values appear first.
     */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    
    /**
     * Timestamp when this album was added to the bubble list.
     */
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
