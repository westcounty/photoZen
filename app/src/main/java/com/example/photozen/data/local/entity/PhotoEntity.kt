package com.example.photozen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.photozen.data.model.CropState
import com.example.photozen.data.model.PhotoStatus

/**
 * Room Entity representing a photo in the local database.
 * 
 * Design principles:
 * - Non-destructive: Original files are never modified
 * - Virtual copies: Multiple versions of same photo via parentId reference
 * - Metadata only: Crop state stored as data, not applied to file
 */
@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["system_uri"], unique = false),
        Index(value = ["status"]),
        Index(value = ["parent_id"]),
        Index(value = ["date_added"])
    ]
)
data class PhotoEntity(
    /**
     * Unique identifier (UUID for new entries, or derived from MediaStore ID)
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    /**
     * URI to the original photo in system MediaStore
     * Format: content://media/external/images/media/{id}
     */
    @ColumnInfo(name = "system_uri")
    val systemUri: String,
    
    /**
     * Current sorting status of the photo
     */
    @ColumnInfo(name = "status")
    val status: PhotoStatus = PhotoStatus.UNSORTED,
    
    /**
     * Virtual crop/transformation state (non-destructive)
     */
    @Embedded
    val cropState: CropState = CropState.DEFAULT,
    
    /**
     * Whether this is a virtual copy of another photo
     */
    @ColumnInfo(name = "is_virtual_copy")
    val isVirtualCopy: Boolean = false,
    
    /**
     * Reference to parent photo ID (for virtual copies)
     * Null for original photos
     */
    @ColumnInfo(name = "parent_id")
    val parentId: String? = null,
    
    /**
     * Display name of the photo file
     */
    @ColumnInfo(name = "display_name")
    val displayName: String = "",
    
    /**
     * File size in bytes
     */
    @ColumnInfo(name = "size")
    val size: Long = 0,
    
    /**
     * Image width in pixels
     */
    @ColumnInfo(name = "width")
    val width: Int = 0,
    
    /**
     * Image height in pixels
     */
    @ColumnInfo(name = "height")
    val height: Int = 0,
    
    /**
     * MIME type (e.g., "image/jpeg")
     */
    @ColumnInfo(name = "mime_type")
    val mimeType: String = "",
    
    /**
     * Date photo was taken (from EXIF) or added to device
     * Unix timestamp in milliseconds
     */
    @ColumnInfo(name = "date_taken")
    val dateTaken: Long = 0,
    
    /**
     * Date photo was added to MediaStore
     * Unix timestamp in seconds (MediaStore format)
     */
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = 0,
    
    /**
     * Date photo was last modified
     * Unix timestamp in seconds
     */
    @ColumnInfo(name = "date_modified")
    val dateModified: Long = 0,
    
    /**
     * Camera make/manufacturer (from EXIF)
     */
    @ColumnInfo(name = "camera_make")
    val cameraMake: String? = null,
    
    /**
     * Camera model (from EXIF)
     */
    @ColumnInfo(name = "camera_model")
    val cameraModel: String? = null,
    
    /**
     * Whether the photo has been synced from MediaStore
     */
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true,
    
    /**
     * Timestamp when this record was created in our DB
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp when this record was last updated
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
