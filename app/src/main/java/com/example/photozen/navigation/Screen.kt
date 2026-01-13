package com.example.photozen.navigation

import com.example.photozen.data.model.PhotoStatus
import kotlinx.serialization.Serializable

/**
 * Sealed class defining all navigation destinations in PicZen.
 * Using type-safe navigation with Kotlin serialization.
 */
sealed interface Screen {
    
    /**
     * Home screen - entry point showing gallery overview
     */
    @Serializable
    data object Home : Screen
    
    /**
     * Flow Sorter screen - Tinder-style swipe to sort photos
     */
    @Serializable
    data object FlowSorter : Screen
    
    /**
     * Light Table screen - compare "Maybe" photos with sync zoom
     */
    @Serializable
    data object LightTable : Screen
    
    /**
     * Photo List screen - display photos by status
     * @param statusName The PhotoStatus name (KEEP, TRASH, MAYBE, UNSORTED)
     */
    @Serializable
    data class PhotoList(val statusName: String) : Screen
    
    /**
     * Trash screen - manage and permanently delete photos
     */
    @Serializable
    data object Trash : Screen
    
    /**
     * Photo Editor screen - non-destructive cropping and virtual copies
     * @param photoId The ID of the photo to edit
     */
    @Serializable
    data class PhotoEditor(val photoId: String) : Screen
    
    /**
     * Settings screen - app preferences
     */
    @Serializable
    data object Settings : Screen
}
