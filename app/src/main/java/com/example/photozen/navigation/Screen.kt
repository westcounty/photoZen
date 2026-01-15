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
    
    /**
     * Workflow screen - the immersive "Flow Tunnel" experience.
     * Guides user through: Swipe -> Compare -> Victory
     */
    @Serializable
    data object Workflow : Screen
    
    /**
     * Tag Bubble screen - interactive bubble graph for tag visualization.
     */
    @Serializable
    data object TagBubble : Screen
    
    /**
     * Photo List by Tag screen - display photos with a specific tag.
     * @param tagId The ID of the tag to filter by
     */
    @Serializable
    data class PhotoListByTag(val tagId: String) : Screen
    
    /**
     * Quick Tag screen - Flow-style quick tagging for kept photos.
     * Click tag to assign and auto-advance to next photo.
     */
    @Serializable
    data object QuickTag : Screen
    
    /**
     * Achievements screen - displays all achievements and progress.
     */
    @Serializable
    data object Achievements : Screen
    
    /**
     * Photo Filter Selection screen - choose albums and date range for custom filtering.
     * @param mode The sorting mode: "flow" for FlowSorter, "quicktag" for QuickTag
     */
    @Serializable
    data class PhotoFilterSelection(val mode: String = "flow") : Screen
}
