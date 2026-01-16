package com.example.photozen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track daily sorting progress.
 */
@Entity(tableName = "daily_stats")
data class DailyStats(
    /**
     * Date in format "yyyy-MM-dd".
     */
    @PrimaryKey
    val date: String,
    
    /**
     * Number of photos sorted on this day.
     */
    val count: Int = 0,
    
    /**
     * The target goal for this day (snapshot).
     */
    val target: Int = 100
)
