package com.example.photozen.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing detected face data.
 * Each face belongs to a photo and can be assigned to a person through clustering.
 */
@Entity(
    tableName = "faces",
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["photoId"]),
        Index(value = ["personId"])
    ]
)
data class FaceEntity(
    @PrimaryKey
    val id: String,
    
    /**
     * The photo this face belongs to.
     */
    val photoId: String,
    
    /**
     * JSON representation of the bounding box.
     * Format: {"left": 0.1, "top": 0.2, "right": 0.5, "bottom": 0.6}
     * Values are normalized (0.0 - 1.0) relative to image dimensions.
     */
    val boundingBox: String,
    
    /**
     * Face embedding vector for clustering/recognition.
     * 128-dimensional float vector stored as ByteArray.
     */
    val embedding: ByteArray? = null,
    
    /**
     * The person this face is assigned to (after clustering).
     * Null if not yet clustered or unrecognized.
     */
    val personId: String? = null,
    
    /**
     * Confidence score from face detection (0.0 - 1.0).
     */
    val confidence: Float = 0f,
    
    /**
     * Timestamp when this face was detected.
     */
    val detectedAt: Long = System.currentTimeMillis(),
    
    /**
     * Whether the user has manually confirmed/rejected this face assignment.
     */
    val isManuallyVerified: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEntity

        if (id != other.id) return false
        if (photoId != other.photoId) return false
        if (boundingBox != other.boundingBox) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (personId != other.personId) return false
        if (confidence != other.confidence) return false
        if (detectedAt != other.detectedAt) return false
        if (isManuallyVerified != other.isManuallyVerified) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + photoId.hashCode()
        result = 31 * result + boundingBox.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + (personId?.hashCode() ?: 0)
        result = 31 * result + confidence.hashCode()
        result = 31 * result + detectedAt.hashCode()
        result = 31 * result + isManuallyVerified.hashCode()
        return result
    }
}
