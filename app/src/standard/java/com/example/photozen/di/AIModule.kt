package com.example.photozen.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Empty AI Module for Standard flavor.
 * This flavor does not include AI libraries or functionality.
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    // No bindings provided.
}
