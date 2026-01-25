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
     * @param albumBucketId Optional bucket ID to filter photos to a specific album
     * @param initialListMode Whether to start in list view mode (for "view more" from timeline)
     */
    @Serializable
    data class FlowSorter(
        val isDailyTask: Boolean = false,
        val targetCount: Int = -1,
        val albumBucketId: String? = null,
        val initialListMode: Boolean = false
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
     * Album Bubble screen - interactive bubble graph for album visualization.
     */
    @Serializable
    data object AlbumBubble : Screen
    
    /**
     * Album Photo List screen - display photos in a specific album.
     * @param bucketId The MediaStore bucket ID of the album
     * @param albumName The display name of the album
     */
    @Serializable
    data class AlbumPhotoList(
        val bucketId: String,
        val albumName: String
    ) : Screen
    
    /**
     * Achievements screen - displays all achievements and progress.
     */
    @Serializable
    data object Achievements : Screen
    
    /**
     * Stats screen - displays sorting statistics, calendar heatmap, and streak.
     * Phase 3 feature for tracking sorting progress over time.
     */
    @Serializable
    data object Stats : Screen
    
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
     * Timeline screen - smart event-based timeline view.
     */
    @Serializable
    data object Timeline : Screen

    /**
     * Timeline Detail screen - displays all photos in a specific time range.
     * @param title The display title (e.g., "2024年1月")
     * @param startTime Start time in milliseconds
     * @param endTime End time in milliseconds
     */
    @Serializable
    data class TimelineDetail(
        val title: String,
        val startTime: Long,
        val endTime: Long
    ) : Screen
    
    // ==================== Share Screens ====================
    
    /**
     * Share Copy screen - copy shared photos from external apps to album.
     * @param urisJson Comma-separated URI strings
     */
    @Serializable
    data class ShareCopy(val urisJson: String) : Screen
    
    /**
     * Share Compare screen - compare shared photos from external apps.
     * @param urisJson Comma-separated URI strings
     */
    @Serializable
    data class ShareCompare(val urisJson: String) : Screen
}
