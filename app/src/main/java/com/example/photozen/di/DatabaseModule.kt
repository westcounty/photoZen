package com.example.photozen.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.photozen.data.local.AppDatabase
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.TagDao
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
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
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
     * Provides TagDao from the database.
     */
    @Provides
    @Singleton
    fun provideTagDao(database: AppDatabase): TagDao {
        return database.tagDao()
    }
}
