package com.example.photozen.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Extension property for DataStore.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "piczen_preferences"
)

/**
 * Hilt module for providing DataStore.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    
    /**
     * Provides the DataStore instance for app preferences.
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}
