package com.example.photozen.data.model

import androidx.room.ColumnInfo

/**
 * Represents the virtual crop state of a photo.
 * Non-destructive: only stores transformation metadata, doesn't modify original file.
 * 
 * Used for:
 * - Virtual cropping without modifying original image
 * - Storing zoom/pan state for virtual copies
 */
data class CropState(
    /**
     * Scale factor (1.0 = original size, up to 10x zoom)
     */
    @ColumnInfo(name = "crop_scale")
    val scale: Float = 1f,
    
    /**
     * Horizontal offset in pixels
     */
    @ColumnInfo(name = "crop_offset_x")
    val offsetX: Float = 0f,
    
    /**
     * Vertical offset in pixels
     */
    @ColumnInfo(name = "crop_offset_y")
    val offsetY: Float = 0f,
    
    /**
     * Rotation angle in degrees (-45 to 45)
     */
    @ColumnInfo(name = "crop_rotation")
    val rotation: Float = 0f,
    
    /**
     * Aspect ratio identifier (ORIGINAL, SQUARE, etc.)
     */
    @ColumnInfo(name = "crop_aspect_ratio", defaultValue = "original")
    val aspectRatioId: String = AspectRatio.ORIGINAL.id,
    
    /**
     * Custom crop frame width ratio (0-1, relative to original width)
     * Used for FREE aspect ratio mode
     */
    @ColumnInfo(name = "crop_frame_width", defaultValue = "1.0")
    val cropFrameWidth: Float = 1f,
    
    /**
     * Custom crop frame height ratio (0-1, relative to original height)
     * Used for FREE aspect ratio mode
     */
    @ColumnInfo(name = "crop_frame_height", defaultValue = "1.0")
    val cropFrameHeight: Float = 1f
) {
    companion object {
        /**
         * Default crop state with no transformations
         */
        val DEFAULT = CropState()
    }
}

/**
 * Preset aspect ratios for cropping.
 */
enum class AspectRatio(
    val id: String,
    val displayName: String,
    val ratio: Float? // null means original
) {
    ORIGINAL("original", "原始", null),
    SQUARE("1:1", "1:1", 1f),
    RATIO_3_4("3:4", "3:4", 3f / 4f),
    RATIO_4_3("4:3", "4:3", 4f / 3f),
    RATIO_9_16("9:16", "9:16", 9f / 16f),
    RATIO_16_9("16:9", "16:9", 16f / 9f);
    
    companion object {
        fun fromId(id: String): AspectRatio {
            return entries.find { it.id == id } ?: ORIGINAL
        }
    }
}
