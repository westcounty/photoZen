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
     * Scale factor (1.0 = original size, 2.0 = 2x zoom)
     */
    @ColumnInfo(name = "crop_scale")
    val scale: Float = 1f,
    
    /**
     * Horizontal offset in normalized coordinates (0-1 range relative to image width)
     */
    @ColumnInfo(name = "crop_offset_x")
    val offsetX: Float = 0f,
    
    /**
     * Vertical offset in normalized coordinates (0-1 range relative to image height)
     */
    @ColumnInfo(name = "crop_offset_y")
    val offsetY: Float = 0f,
    
    /**
     * Rotation angle in degrees (0, 90, 180, 270)
     */
    @ColumnInfo(name = "crop_rotation")
    val rotation: Float = 0f
) {
    companion object {
        /**
         * Default crop state with no transformations
         */
        val DEFAULT = CropState()
    }
}
