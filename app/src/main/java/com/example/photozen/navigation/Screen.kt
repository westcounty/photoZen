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
     * @param isDailyTask Whether this is a daily task session
     * @param targetCount Target number of photos to sort (if isDailyTask)
     */
    @Serializable
    data class FlowSorter(
        val isDailyTask: Boolean = false,
        val targetCount: Int = -1
    ) : Screen
    
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
     * @param isDailyTask Whether this is a daily task session
     * @param targetCount Target number of photos to sort (if isDailyTask)
     */
    @Serializable
    data class Workflow(
        val isDailyTask: Boolean = false,
        val targetCount: Int = -1
    ) : Screen
    
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
     * @param mode The sorting mode: "flow", "workflow", "flow_daily", "workflow_daily"
     * @param isDailyTask Whether this is a daily task session
     * @param targetCount Target count for daily task
     */
    @Serializable
    data class PhotoFilterSelection(
        val mode: String = "flow",
        val isDailyTask: Boolean = false,
        val targetCount: Int = -1
    ) : Screen
    
    /**
     * Smart Gallery screen - AI-powered photo features (experimental).
     * Includes: AI search, similar photos, person grouping, map view.
     */
    @Serializable
    data object SmartGallery : Screen
}
