package com.example.photozen.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entity for photo-label associations.
 * Optimized for fast label aggregation queries on large datasets (40k+ photos).
 * 
 * Uses a composite primary key (photoId, label) for uniqueness.
 * Indexes on both columns enable efficient queries in both directions.
 */
@Entity(
    tableName = "photo_labels",
    primaryKeys = ["photoId", "label"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["photoId"]),
        Index(value = ["label"]),
        Index(value = ["label", "photoId"]) // For COUNT queries
    ]
)
data class PhotoLabelEntity(
    /**
     * The photo this label belongs to.
     */
    val photoId: String,
    
    /**
     * The label text (normalized to lowercase for consistent matching).
     */
    val label: String,
    
    /**
     * Confidence score from ML Kit (0.0 - 1.0).
     */
    val confidence: Float = 0f
)
