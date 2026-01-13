package com.example.photozen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Cross-reference table for Many-to-Many relationship between Photos and Tags.
 * A photo can have multiple tags, and a tag can be applied to multiple photos.
 */
@Entity(
    tableName = "photo_tag_cross_ref",
    primaryKeys = ["photo_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["photo_id"]),
        Index(value = ["tag_id"])
    ]
)
data class PhotoTagCrossRef(
    @ColumnInfo(name = "photo_id")
    val photoId: String,
    
    @ColumnInfo(name = "tag_id")
    val tagId: String,
    
    /**
     * Timestamp when the tag was applied to the photo
     */
    @ColumnInfo(name = "tagged_at")
    val taggedAt: Long = System.currentTimeMillis()
)
