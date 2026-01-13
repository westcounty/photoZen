package com.example.photozen.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.photozen.data.local.converter.Converters
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.PhotoTagCrossRef
import com.example.photozen.data.local.entity.TagEntity

/**
 * Room Database for PicZen app.
 * 
 * Contains:
 * - PhotoEntity: Photo records with status, crop state, and metadata
 * - TagEntity: User-defined tags for organizing photos
 * - PhotoTagCrossRef: Many-to-many relationship between photos and tags
 */
@Database(
    entities = [
        PhotoEntity::class,
        TagEntity::class,
        PhotoTagCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * DAO for photo operations.
     */
    abstract fun photoDao(): PhotoDao
    
    /**
     * DAO for tag operations.
     */
    abstract fun tagDao(): TagDao
    
    companion object {
        const val DATABASE_NAME = "piczen_database"
    }
}
