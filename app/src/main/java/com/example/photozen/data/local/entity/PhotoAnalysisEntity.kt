package com.example.photozen.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing AI analysis results for photos.
 * Contains labels, embeddings for similarity search, and GPS coordinates.
 */
@Entity(
    tableName = "photo_analysis",
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
        Index(value = ["hasGps"]),
        Index(value = ["analyzedAt"])
    ]
)
data class PhotoAnalysisEntity(
    @PrimaryKey
    val photoId: String,
    
    /**
     * JSON array of detected labels.
     * Example: ["cat", "outdoor", "sunset", "animal"]
     */
    val labels: String = "[]",
    
    /**
     * Image embedding vector for similarity search.
     * 512-dimensional float vector stored as ByteArray.
     */
    val embedding: ByteArray? = null,
    
    /**
     * Timestamp when the photo was analyzed.
     */
    val analyzedAt: Long = System.currentTimeMillis(),
    
    /**
     * Whether the photo has GPS coordinates.
     */
    val hasGps: Boolean = false,
    
    /**
     * GPS latitude coordinate.
     */
    val latitude: Double? = null,
    
    /**
     * GPS longitude coordinate.
     */
    val longitude: Double? = null,
    
    /**
     * Number of faces detected in the photo.
     */
    val faceCount: Int = 0,
    
    /**
     * Primary detected scene/category.
     * Example: "landscape", "portrait", "food", etc.
     */
    val primaryCategory: String? = null,
    
    /**
     * Confidence score for the primary category (0.0 - 1.0).
     */
    val primaryCategoryConfidence: Float = 0f,
    
    // ==================== Duplicate Detection Fields ====================
    
    /**
     * Perceptual hash (pHash) for duplicate detection.
     * 64-bit hash stored as 16-character hex string.
     */
    val phash: String? = null,
    
    /**
     * Dominant color in hex format (e.g., "#FF5733").
     */
    val dominantColor: String? = null,
    
    /**
     * Accent (secondary) color in hex format.
     */
    val accentColor: String? = null,
    
    /**
     * Average luminance/brightness (0-100).
     */
    val luminance: Int = 0,
    
    /**
     * Color saturation level (0-100).
     */
    val chroma: Int = 0,
    
    /**
     * Overall image quality score (0-100).
     */
    val quality: Int = 0,
    
    /**
     * Image sharpness score (0-100).
     */
    val sharpness: Int = 0,
    
    /**
     * Image aspect ratio (width/height).
     */
    val aspectRatio: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhotoAnalysisEntity

        if (photoId != other.photoId) return false
        if (labels != other.labels) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (analyzedAt != other.analyzedAt) return false
        if (hasGps != other.hasGps) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (faceCount != other.faceCount) return false
        if (primaryCategory != other.primaryCategory) return false
        if (primaryCategoryConfidence != other.primaryCategoryConfidence) return false
        if (phash != other.phash) return false
        if (dominantColor != other.dominantColor) return false
        if (accentColor != other.accentColor) return false
        if (luminance != other.luminance) return false
        if (chroma != other.chroma) return false
        if (quality != other.quality) return false
        if (sharpness != other.sharpness) return false
        if (aspectRatio != other.aspectRatio) return false

        return true
    }

    override fun hashCode(): Int {
        var result = photoId.hashCode()
        result = 31 * result + labels.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + analyzedAt.hashCode()
        result = 31 * result + hasGps.hashCode()
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        result = 31 * result + faceCount
        result = 31 * result + (primaryCategory?.hashCode() ?: 0)
        result = 31 * result + primaryCategoryConfidence.hashCode()
        result = 31 * result + (phash?.hashCode() ?: 0)
        result = 31 * result + (dominantColor?.hashCode() ?: 0)
        result = 31 * result + (accentColor?.hashCode() ?: 0)
        result = 31 * result + luminance
        result = 31 * result + chroma
        result = 31 * result + quality
        result = 31 * result + sharpness
        result = 31 * result + aspectRatio.hashCode()
        return result
    }
}
