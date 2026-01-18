package com.example.photozen.domain

import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Represents a group of photos that belong to the same event.
 */
data class PhotoEvent(
    val id: String,
    val title: String,
    val subtitle: String?,
    val startTime: Long,
    val endTime: Long,
    val location: String?,
    val coverPhotoUri: String,
    val photos: List<PhotoEntity>,
    val isExpanded: Boolean = false
) {
    val photoCount: Int get() = photos.size
    val sortedCount: Int get() = photos.count { it.status != PhotoStatus.UNSORTED }
    val durationMinutes: Long get() = (endTime - startTime) / (1000 * 60)
}

/**
 * Grouping mode for timeline.
 */
enum class GroupingMode {
    AUTO,   // Smart grouping based on time gaps and location changes
    DAY,    // Group by day
    MONTH,  // Group by month
    YEAR    // Group by year
}

/**
 * Smart event grouper that automatically clusters photos into events
 * based on time gaps and location changes.
 */
@Singleton
class EventGrouper @Inject constructor() {
    
    companion object {
        // Time threshold for auto grouping: 4 hours in milliseconds
        private const val TIME_GAP_THRESHOLD_MS = 4 * 60 * 60 * 1000L
        
        // Distance threshold for auto grouping: 50 km in degrees (approximate)
        private const val DISTANCE_THRESHOLD_DEGREES = 0.45 // ~50km at equator
        
        // Date formatters
        private val dayFormatter = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
        private val monthFormatter = SimpleDateFormat("yyyy年M月", Locale.CHINA)
        private val yearFormatter = SimpleDateFormat("yyyy年", Locale.CHINA)
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.CHINA)
        
        /**
         * Get effective time for a photo.
         * Prefers dateTaken (EXIF), falls back to dateAdded * 1000 (MediaStore).
         */
        fun PhotoEntity.getEffectiveTime(): Long {
            return if (dateTaken > 0) dateTaken else dateAdded * 1000
        }
    }
    
    /**
     * Group photos into events based on the specified mode.
     */
    fun groupPhotos(photos: List<PhotoEntity>, mode: GroupingMode): List<PhotoEvent> {
        if (photos.isEmpty()) return emptyList()
        
        // Sort by effective time (prefers dateTaken, falls back to dateAdded)
        val sortedPhotos = photos.sortedBy { it.getEffectiveTime() }
        
        return when (mode) {
            GroupingMode.AUTO -> groupByAuto(sortedPhotos)
            GroupingMode.DAY -> groupByDay(sortedPhotos)
            GroupingMode.MONTH -> groupByMonth(sortedPhotos)
            GroupingMode.YEAR -> groupByYear(sortedPhotos)
        }
    }
    
    /**
     * Smart auto-grouping based on time gaps and location changes.
     */
    private fun groupByAuto(photos: List<PhotoEntity>): List<PhotoEvent> {
        val events = mutableListOf<MutableList<PhotoEntity>>()
        var currentEvent = mutableListOf<PhotoEntity>()
        
        for (i in photos.indices) {
            val photo = photos[i]
            
            if (currentEvent.isEmpty()) {
                currentEvent.add(photo)
                continue
            }
            
            val lastPhoto = currentEvent.last()
            val shouldStartNewEvent = shouldStartNewEvent(lastPhoto, photo)
            
            if (shouldStartNewEvent) {
                events.add(currentEvent)
                currentEvent = mutableListOf(photo)
            } else {
                currentEvent.add(photo)
            }
        }
        
        // Add the last event
        if (currentEvent.isNotEmpty()) {
            events.add(currentEvent)
        }
        
        // Convert to PhotoEvent objects
        return events.mapIndexed { index, eventPhotos ->
            createPhotoEvent("auto_$index", eventPhotos, GroupingMode.AUTO)
        }
    }
    
    /**
     * Determine if a new event should start based on time gap and location change.
     */
    private fun shouldStartNewEvent(lastPhoto: PhotoEntity, currentPhoto: PhotoEntity): Boolean {
        // Check time gap using effective time
        val timeGap = currentPhoto.getEffectiveTime() - lastPhoto.getEffectiveTime()
        if (timeGap > TIME_GAP_THRESHOLD_MS) {
            return true
        }
        
        // Check location change (if both have GPS data)
        if (lastPhoto.latitude != null && lastPhoto.longitude != null &&
            currentPhoto.latitude != null && currentPhoto.longitude != null) {
            
            val distance = calculateDistance(
                lastPhoto.latitude, lastPhoto.longitude,
                currentPhoto.latitude, currentPhoto.longitude
            )
            
            if (distance > DISTANCE_THRESHOLD_DEGREES) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Group photos by day.
     */
    private fun groupByDay(photos: List<PhotoEntity>): List<PhotoEvent> {
        val groups = photos.groupBy { photo ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = photo.getEffectiveTime()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        
        return groups.entries
            .sortedByDescending { it.key }
            .mapIndexed { index, (dayStart, dayPhotos) ->
                createPhotoEvent("day_$index", dayPhotos, GroupingMode.DAY)
            }
    }
    
    /**
     * Group photos by month.
     */
    private fun groupByMonth(photos: List<PhotoEntity>): List<PhotoEvent> {
        val groups = photos.groupBy { photo ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = photo.getEffectiveTime()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        
        return groups.entries
            .sortedByDescending { it.key }
            .mapIndexed { index, (monthStart, monthPhotos) ->
                createPhotoEvent("month_$index", monthPhotos, GroupingMode.MONTH)
            }
    }
    
    /**
     * Group photos by year.
     */
    private fun groupByYear(photos: List<PhotoEntity>): List<PhotoEvent> {
        val groups = photos.groupBy { photo ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = photo.getEffectiveTime()
            calendar.get(Calendar.YEAR)
        }
        
        return groups.entries
            .sortedByDescending { it.key }
            .mapIndexed { index, (year, yearPhotos) ->
                createPhotoEvent("year_$index", yearPhotos, GroupingMode.YEAR)
            }
    }
    
    /**
     * Create a PhotoEvent from a list of photos.
     */
    private fun createPhotoEvent(
        id: String,
        photos: List<PhotoEntity>,
        mode: GroupingMode
    ): PhotoEvent {
        val sortedPhotos = photos.sortedBy { it.getEffectiveTime() }
        val startTime = sortedPhotos.first().getEffectiveTime()
        val endTime = sortedPhotos.last().getEffectiveTime()
        
        // Generate title based on mode
        val title = generateTitle(startTime, endTime, mode)
        
        // Generate subtitle
        val subtitle = generateSubtitle(sortedPhotos.size, startTime, endTime, mode)
        
        // Detect location (use most common location if available)
        val location = detectLocation(photos)
        
        // Use first photo as cover
        val coverPhotoUri = sortedPhotos.first().systemUri
        
        return PhotoEvent(
            id = id,
            title = title,
            subtitle = subtitle,
            startTime = startTime,
            endTime = endTime,
            location = location,
            coverPhotoUri = coverPhotoUri,
            photos = sortedPhotos
        )
    }
    
    /**
     * Generate event title based on date range and mode.
     */
    private fun generateTitle(startTime: Long, endTime: Long, mode: GroupingMode): String {
        val startDate = Date(startTime)
        val endDate = Date(endTime)
        
        return when (mode) {
            GroupingMode.AUTO -> {
                // For auto mode, show date with optional time range
                val startDay = dayFormatter.format(startDate)
                val endDay = dayFormatter.format(endDate)
                
                if (startDay == endDay) {
                    // Same day event
                    "$startDay ${timeFormatter.format(startDate)} - ${timeFormatter.format(endDate)}"
                } else {
                    // Multi-day event
                    "$startDay - $endDay"
                }
            }
            GroupingMode.DAY -> dayFormatter.format(startDate)
            GroupingMode.MONTH -> monthFormatter.format(startDate)
            GroupingMode.YEAR -> yearFormatter.format(startDate)
        }
    }
    
    /**
     * Generate event subtitle.
     */
    private fun generateSubtitle(
        photoCount: Int,
        startTime: Long,
        endTime: Long,
        mode: GroupingMode
    ): String {
        val countText = "$photoCount 张照片"
        
        return when (mode) {
            GroupingMode.AUTO -> {
                val durationMinutes = (endTime - startTime) / (1000 * 60)
                when {
                    durationMinutes < 60 -> "$countText · ${durationMinutes}分钟"
                    durationMinutes < 1440 -> "$countText · ${durationMinutes / 60}小时"
                    else -> "$countText · ${durationMinutes / 1440}天"
                }
            }
            else -> countText
        }
    }
    
    /**
     * Detect a location name from photos (placeholder - could use reverse geocoding).
     */
    private fun detectLocation(photos: List<PhotoEntity>): String? {
        // Find photos with GPS data
        val photosWithGps = photos.filter { it.latitude != null && it.longitude != null }
        if (photosWithGps.isEmpty()) return null
        
        // Return coordinate-based location hint
        val avgLat = photosWithGps.mapNotNull { it.latitude }.average()
        val avgLng = photosWithGps.mapNotNull { it.longitude }.average()
        
        return String.format(Locale.US, "%.2f, %.2f", avgLat, avgLng)
    }
    
    /**
     * Calculate distance between two GPS points in degrees.
     */
    private fun calculateDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val latDiff = lat1 - lat2
        val lngDiff = (lng1 - lng2) * cos(Math.toRadians(lat1))
        return sqrt(latDiff.pow(2) + lngDiff.pow(2))
    }
}
