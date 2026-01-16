package com.example.photozen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photozen.data.local.entity.DailyStats
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for daily stats.
 */
@Dao
interface DailyStatsDao {
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun getStatsByDate(date: String): Flow<DailyStats?>
    
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsByDateOneShot(date: String): DailyStats?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyStats)
    
    @Query("UPDATE daily_stats SET count = count + :increment WHERE date = :date")
    suspend fun incrementCount(date: String, increment: Int)
    
    @Query("SELECT * FROM daily_stats")
    fun getAllStats(): Flow<List<DailyStats>>
}
