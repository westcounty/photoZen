package com.example.photozen.di

import android.content.Context
import com.example.photozen.ai.AIAnalysisManager
import com.example.photozen.ai.FaceClusteringService
import com.example.photozen.ai.FaceDetector
import com.example.photozen.ai.FaceEmbedding
import com.example.photozen.ai.ImageLabeler
import com.example.photozen.data.local.dao.FaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing AI-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    
    @Provides
    @Singleton
    fun provideImageLabeler(
        @ApplicationContext context: Context
    ): ImageLabeler {
        return ImageLabeler(context)
    }
    
    @Provides
    @Singleton
    fun provideFaceDetector(
        @ApplicationContext context: Context
    ): FaceDetector {
        return FaceDetector(context)
    }
    
    @Provides
    @Singleton
    fun provideFaceEmbedding(
        @ApplicationContext context: Context
    ): FaceEmbedding {
        return FaceEmbedding(context)
    }
    
    @Provides
    @Singleton
    fun provideAIAnalysisManager(
        @ApplicationContext context: Context
    ): AIAnalysisManager {
        return AIAnalysisManager(context)
    }
    
    @Provides
    @Singleton
    fun provideFaceClusteringService(
        faceDao: FaceDao,
        faceEmbedding: FaceEmbedding
    ): FaceClusteringService {
        return FaceClusteringService(faceDao, faceEmbedding)
    }
}
