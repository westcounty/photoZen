package com.example.photozen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mode for copying photos to linked album.
 */
enum class AlbumCopyMode {
    /** Copy photos - original stays in place */
    COPY,
    /** Move photos - original is removed from source */
    MOVE
}

/**
 * Room Entity representing a tag for organizing photos.
 * Supports hierarchical tags via parentId for nested categories.
 * Supports linking to system albums for external app integration.
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"]),
        Index(value = ["parent_id"]),
        Index(value = ["linked_album_id"])
    ]
)
data class TagEntity(
    /**
     * Unique identifier for the tag (UUID)
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    /**
     * Display name of the tag
     */
    @ColumnInfo(name = "name")
    val name: String,
    
    /**
     * Reference to parent tag ID for hierarchical organization
     * Null for root-level tags
     */
    @ColumnInfo(name = "parent_id")
    val parentId: String? = null,
    
    /**
     * Color for the tag chip (ARGB hex value)
     */
    @ColumnInfo(name = "color")
    val color: Int = 0xFF5EEAD4.toInt(), // Default to primary teal
    
    /**
     * Sort order within the same parent level
     */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    
    /**
     * Timestamp when this tag was created
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    // ==================== Album Linking Fields ====================
    
    /**
     * MediaStore bucket ID of the linked system album.
     * Null if not linked to any album.
     */
    @ColumnInfo(name = "linked_album_id", defaultValue = "NULL")
    val linkedAlbumId: String? = null,
    
    /**
     * Name of the linked album (for display purposes).
     */
    @ColumnInfo(name = "linked_album_name", defaultValue = "NULL")
    val linkedAlbumName: String? = null,
    
    /**
     * Mode used when copying photos to the linked album.
     * COPY = keep original, MOVE = delete original after copy.
     */
    @ColumnInfo(name = "album_copy_mode", defaultValue = "NULL")
    val albumCopyMode: AlbumCopyMode? = null
) {
    /**
     * Whether this tag is linked to a system album.
     */
    val isLinkedToAlbum: Boolean
        get() = linkedAlbumId != null
}
