package com.example.photozen.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.photozen.data.local.converter.Converters
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.DailyStatsDao
import com.example.photozen.data.local.dao.FaceDao
import com.example.photozen.data.local.dao.PhotoAnalysisDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.PhotoLabelDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.DailyStats
import com.example.photozen.data.local.entity.FaceEntity
import com.example.photozen.data.local.entity.PersonEntity
import com.example.photozen.data.local.entity.PhotoAnalysisEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.PhotoLabelEntity

/**
 * Room Database for PicZen app.
 * 
 * Contains:
 * - PhotoEntity: Photo records with status, crop state, and metadata
 * - DailyStats: Tracks daily sorting progress
 * - PhotoAnalysisEntity: AI analysis results (labels, embeddings, GPS)
 * - FaceEntity: Detected faces with embeddings
 * - PersonEntity: Clustered persons from face recognition
 * - PhotoLabelEntity: Photo-label associations for fast queries
 * - AlbumBubbleEntity: User's album bubble list for quick classification
 */
@Database(
    entities = [
        PhotoEntity::class,
        DailyStats::class,
        PhotoAnalysisEntity::class,
        FaceEntity::class,
        PersonEntity::class,
        PhotoLabelEntity::class,
        AlbumBubbleEntity::class
    ],
    version = 10,
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
     * DAO for photo AI analysis operations.
     */
    abstract fun photoAnalysisDao(): PhotoAnalysisDao
    
    /**
     * DAO for face and person operations.
     */
    abstract fun faceDao(): FaceDao
    
    /**
     * DAO for photo-label associations.
     */
    abstract fun photoLabelDao(): PhotoLabelDao
    
    /**
     * DAO for album bubble list operations.
     */
    abstract fun albumBubbleDao(): AlbumBubbleDao
    
    companion object {
        const val DATABASE_NAME = "piczen_database"
    }
}
