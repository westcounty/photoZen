package com.example.photozen.di

import android.content.Context
import androidx.room.Room
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
