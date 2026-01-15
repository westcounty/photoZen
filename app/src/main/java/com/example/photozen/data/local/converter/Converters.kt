package com.example.photozen.data.local.converter

import androidx.room.TypeConverter
import com.example.photozen.data.local.entity.AlbumCopyMode
import com.example.photozen.data.model.PhotoStatus

/**
 * Room TypeConverters for custom data types.
 * Converts enums and complex types to/from database-compatible formats.
 */
class Converters {
    
    /**
     * Convert PhotoStatus enum to String for database storage.
     */
    @TypeConverter
    fun fromPhotoStatus(status: PhotoStatus): String {
        return status.name
    }
    
    /**
     * Convert String from database to PhotoStatus enum.
     */
    @TypeConverter
    fun toPhotoStatus(value: String): PhotoStatus {
        return try {
            PhotoStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            PhotoStatus.UNSORTED // Default fallback
        }
    }
    
    /**
     * Convert AlbumCopyMode enum to String for database storage.
     */
    @TypeConverter
    fun fromAlbumCopyMode(mode: AlbumCopyMode?): String? {
        return mode?.name
    }
    
    /**
     * Convert String from database to AlbumCopyMode enum.
     */
    @TypeConverter
    fun toAlbumCopyMode(value: String?): AlbumCopyMode? {
        return value?.let {
            try {
                AlbumCopyMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
