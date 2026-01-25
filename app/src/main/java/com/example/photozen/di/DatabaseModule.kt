package com.example.photozen.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.photozen.data.local.AppDatabase
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.SortingRecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Migration from version 3 to 4: Add GPS location fields to photos table.
 * - latitude: GPS latitude from EXIF
 * - longitude: GPS longitude from EXIF  
 * - gps_scanned: Flag to track if GPS has been extracted
 */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add latitude column
        db.execSQL("ALTER TABLE photos ADD COLUMN latitude REAL DEFAULT NULL")
        // Add longitude column
        db.execSQL("ALTER TABLE photos ADD COLUMN longitude REAL DEFAULT NULL")
        // Add gps_scanned flag
        db.execSQL("ALTER TABLE photos ADD COLUMN gps_scanned INTEGER NOT NULL DEFAULT 0")
        // Create index for GPS queries
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_latitude_longitude ON photos (latitude, longitude)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_gps_scanned ON photos (gps_scanned)")
    }
}

/**
 * Migration from version 4 to 5: Add bucket_id field for album filtering.
 * - bucket_id: MediaStore bucket ID (album ID)
 */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add bucket_id column
        db.execSQL("ALTER TABLE photos ADD COLUMN bucket_id TEXT DEFAULT NULL")
        // Create index for bucket_id queries
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_bucket_id ON photos (bucket_id)")
    }
}

/**
 * Migration from version 5 to 6: Add album linking fields to tags table.
 * - linked_album_id: MediaStore bucket ID of the linked system album
 * - linked_album_name: Display name of the linked album
 * - album_copy_mode: COPY or MOVE mode for syncing photos
 */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add linked_album_id column
        db.execSQL("ALTER TABLE tags ADD COLUMN linked_album_id TEXT DEFAULT NULL")
        // Add linked_album_name column
        db.execSQL("ALTER TABLE tags ADD COLUMN linked_album_name TEXT DEFAULT NULL")
        // Add album_copy_mode column
        db.execSQL("ALTER TABLE tags ADD COLUMN album_copy_mode TEXT DEFAULT NULL")
        // Create index for linked_album_id queries
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tags_linked_album_id ON tags (linked_album_id)")
    }
}

/**
 * Migration from version 6 to 7: No schema changes.
 * This is a placeholder migration to ensure the migration path is complete.
 * 
 * Note: If there were actual schema changes between 6 and 7, they should be added here.
 * Currently this is an empty migration to bridge the gap.
 */
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No schema changes in this version
        // This placeholder ensures a valid migration path from 6 to 8
    }
}

/**
 * Migration from version 7 to 8: Add Smart Gallery tables.
 * - photo_analysis: AI analysis results (labels, embeddings, GPS)
 * - faces: Detected faces with embeddings
 * - persons: Clustered persons from face recognition
 * - photo_labels: Photo-label associations for fast queries
 */
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create photo_analysis table with all fields including duplicate detection
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS photo_analysis (
                photoId TEXT NOT NULL PRIMARY KEY,
                labels TEXT NOT NULL DEFAULT '[]',
                embedding BLOB,
                analyzedAt INTEGER NOT NULL DEFAULT 0,
                hasGps INTEGER NOT NULL DEFAULT 0,
                latitude REAL,
                longitude REAL,
                faceCount INTEGER NOT NULL DEFAULT 0,
                primaryCategory TEXT,
                primaryCategoryConfidence REAL NOT NULL DEFAULT 0,
                phash TEXT,
                dominantColor TEXT,
                accentColor TEXT,
                luminance INTEGER NOT NULL DEFAULT 0,
                chroma INTEGER NOT NULL DEFAULT 0,
                quality INTEGER NOT NULL DEFAULT 0,
                sharpness INTEGER NOT NULL DEFAULT 0,
                aspectRatio REAL NOT NULL DEFAULT 0,
                FOREIGN KEY (photoId) REFERENCES photos(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photo_analysis_photoId ON photo_analysis (photoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photo_analysis_hasGps ON photo_analysis (hasGps)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photo_analysis_analyzedAt ON photo_analysis (analyzedAt)")
        
        // Create persons table (must be created before faces due to foreign key)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS persons (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT,
                coverFaceId TEXT NOT NULL,
                faceCount INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                isFavorite INTEGER NOT NULL DEFAULT 0,
                isHidden INTEGER NOT NULL DEFAULT 0,
                averageEmbedding BLOB
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_persons_name ON persons (name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_persons_faceCount ON persons (faceCount)")
        
        // Create faces table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS faces (
                id TEXT NOT NULL PRIMARY KEY,
                photoId TEXT NOT NULL,
                boundingBox TEXT NOT NULL,
                embedding BLOB,
                personId TEXT,
                confidence REAL NOT NULL DEFAULT 0,
                detectedAt INTEGER NOT NULL DEFAULT 0,
                isManuallyVerified INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (photoId) REFERENCES photos(id) ON DELETE CASCADE,
                FOREIGN KEY (personId) REFERENCES persons(id) ON DELETE SET NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_faces_photoId ON faces (photoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_faces_personId ON faces (personId)")
        
        // Create photo_labels table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS photo_labels (
                photoId TEXT NOT NULL,
                label TEXT NOT NULL,
                confidence REAL NOT NULL DEFAULT 0,
                PRIMARY KEY (photoId, label),
                FOREIGN KEY (photoId) REFERENCES photos(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photo_labels_photoId ON photo_labels (photoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photo_labels_label ON photo_labels (label)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photo_labels_label_photoId ON photo_labels (label, photoId)")
    }
}

/**
 * Migration from version 8 to 9: Add album_bubbles table for album classification mode.
 * - album_bubbles: User's album bubble list for quick classification
 */
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create album_bubbles table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS album_bubbles (
                bucket_id TEXT NOT NULL PRIMARY KEY,
                display_name TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                added_at INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}

/**
 * Migration from version 9 to 10: Remove tag-related tables (tags feature removed).
 * - Drop tags table
 * - Drop photo_tag_cross_ref table
 */
private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop photo_tag_cross_ref table first (due to foreign key)
        db.execSQL("DROP TABLE IF EXISTS photo_tag_cross_ref")
        // Drop tags table
        db.execSQL("DROP TABLE IF EXISTS tags")
    }
}

/**
 * Migration from version 10 to 11: Add sorting_records table for detailed stats.
 * - sorting_records: Daily sorting statistics with breakdown (kept/trashed/maybe)
 * - Used for stats page, calendar heatmap, and streak calculation
 */
private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create sorting_records table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sorting_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date TEXT NOT NULL,
                sorted_count INTEGER NOT NULL DEFAULT 0,
                kept_count INTEGER NOT NULL DEFAULT 0,
                trashed_count INTEGER NOT NULL DEFAULT 0,
                maybe_count INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
        """)
        // Create unique index on date
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sorting_records_date ON sorting_records (date)")
    }
}

/**
 * Migration from version 11 to 12: Remove Smart Gallery tables (feature removed).
 * - Drop photo_analysis table
 * - Drop faces table
 * - Drop persons table
 * - Drop photo_labels table
 */
private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop tables in correct order (respect foreign keys)
        db.execSQL("DROP TABLE IF EXISTS photo_labels")
        db.execSQL("DROP TABLE IF EXISTS faces")
        db.execSQL("DROP TABLE IF EXISTS persons")
        db.execSQL("DROP TABLE IF EXISTS photo_analysis")
    }
}

/**
 * Hilt module for providing Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the Room database instance.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
            .fallbackToDestructiveMigration(dropAllTables = true) // For development - use migrations in production
            .build()
    }
    
    /**
     * Provides PhotoDao from the database.
     */
    @Provides
    @Singleton
    fun providePhotoDao(database: AppDatabase): PhotoDao {
        return database.photoDao()
    }

    /**
     * Provides DailyStatsDao from the database.
     */
    @Provides
    @Singleton
    fun provideDailyStatsDao(database: AppDatabase): com.example.photozen.data.local.dao.DailyStatsDao {
        return database.dailyStatsDao()
    }

    /**
     * Provides AlbumBubbleDao from the database.
     */
    @Provides
    @Singleton
    fun provideAlbumBubbleDao(database: AppDatabase): AlbumBubbleDao {
        return database.albumBubbleDao()
    }
    
    /**
     * Provides SortingRecordDao from the database.
     * Used for detailed sorting statistics (Phase 3 - Stats feature).
     */
    @Provides
    @Singleton
    fun provideSortingRecordDao(database: AppDatabase): SortingRecordDao {
        return database.sortingRecordDao()
    }
}
