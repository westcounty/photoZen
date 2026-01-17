package com.example.photozen.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing person data (clustered faces).
 * A person represents a group of faces that belong to the same individual.
 */
@Entity(
    tableName = "persons",
    indices = [
        Index(value = ["name"]),
        Index(value = ["faceCount"])
    ]
)
data class PersonEntity(
    @PrimaryKey
    val id: String,
    
    /**
     * User-assigned name for this person.
     * Null if not yet named.
     */
    val name: String? = null,
    
    /**
     * ID of the face to use as the cover/representative image.
     */
    val coverFaceId: String,
    
    /**
     * Number of faces assigned to this person.
     */
    val faceCount: Int = 1,
    
    /**
     * Timestamp when this person was first created.
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp when this person was last updated.
     */
    val updatedAt: Long = System.currentTimeMillis(),
    
    /**
     * Whether this person is marked as favorite.
     */
    val isFavorite: Boolean = false,
    
    /**
     * Whether this person is hidden from the UI.
     */
    val isHidden: Boolean = false,
    
    /**
     * Average embedding of all faces for this person.
     * Used for faster similarity matching.
     * 128-dimensional float vector stored as ByteArray.
     */
    val averageEmbedding: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersonEntity

        if (id != other.id) return false
        if (name != other.name) return false
        if (coverFaceId != other.coverFaceId) return false
        if (faceCount != other.faceCount) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (isFavorite != other.isFavorite) return false
        if (isHidden != other.isHidden) return false
        if (averageEmbedding != null) {
            if (other.averageEmbedding == null) return false
            if (!averageEmbedding.contentEquals(other.averageEmbedding)) return false
        } else if (other.averageEmbedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + coverFaceId.hashCode()
        result = 31 * result + faceCount
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + isHidden.hashCode()
        result = 31 * result + (averageEmbedding?.contentHashCode() ?: 0)
        return result
    }
}
