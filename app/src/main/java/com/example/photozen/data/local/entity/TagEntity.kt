package com.example.photozen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a tag for organizing photos.
 * Supports hierarchical tags via parentId for nested categories.
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"]),
        Index(value = ["parent_id"])
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
    val createdAt: Long = System.currentTimeMillis()
)
