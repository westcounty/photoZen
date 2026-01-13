package com.example.photozen.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Data class representing a Photo with all its associated Tags.
 * Used for Room's @Relation queries to fetch photos with their tags in one query.
 */
data class PhotoWithTags(
    @Embedded
    val photo: PhotoEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PhotoTagCrossRef::class,
            parentColumn = "photo_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity>
)
