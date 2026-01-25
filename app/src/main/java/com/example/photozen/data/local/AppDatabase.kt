package com.example.photozen.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.photozen.data.local.converter.Converters
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.DailyStatsDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.SortingRecordDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.DailyStats
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.SortingRecordEntity

/**
 * Room Database for PicZen app.
 *
 * Contains:
 * - PhotoEntity: Photo records with status, crop state, and metadata
 * - DailyStats: Tracks daily sorting progress
 * - AlbumBubbleEntity: User's album bubble list for quick classification
 * - SortingRecordEntity: Detailed sorting statistics
 */
@Database(
    entities = [
        PhotoEntity::class,
        DailyStats::class,
        AlbumBubbleEntity::class,
        SortingRecordEntity::class
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * DAO for photo operations.
     */
    abstract fun photoDao(): PhotoDao
    
    /**
     * DAO for daily stats.
     */
    abstract fun dailyStatsDao(): DailyStatsDao

    /**
     * DAO for album bubble list operations.
     */
    abstract fun albumBubbleDao(): AlbumBubbleDao
    
    /**
     * DAO for sorting record operations (整理统计).
     */
    abstract fun sortingRecordDao(): SortingRecordDao
    
    companion object {
        const val DATABASE_NAME = "piczen_database"
    }
}
