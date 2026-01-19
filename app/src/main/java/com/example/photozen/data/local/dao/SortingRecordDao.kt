package com.example.photozen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photozen.data.local.entity.SortingRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 周期统计汇总数据
 */
data class PeriodStats(
    val total: Int?,
    val kept: Int?,
    val trashed: Int?,
    val maybe: Int?
) {
    /** 安全获取总数，null 时返回 0 */
    val safeTotal: Int get() = total ?: 0
    val safeKept: Int get() = kept ?: 0
    val safeTrashed: Int get() = trashed ?: 0
    val safeMaybe: Int get() = maybe ?: 0
}

/**
 * 整理记录数据访问对象
 * 
 * 提供整理记录的 CRUD 操作和统计查询。
 */
@Dao
interface SortingRecordDao {
    
    // ==================== 单条记录查询 ====================
    
    /**
     * 获取指定日期的记录
     * @param date 日期字符串，格式 YYYY-MM-DD
     */
    @Query("SELECT * FROM sorting_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): SortingRecordEntity?
    
    /**
     * 观察指定日期的记录（Flow）
     */
    @Query("SELECT * FROM sorting_records WHERE date = :date LIMIT 1")
    fun observeRecordByDate(date: String): Flow<SortingRecordEntity?>
    
    // ==================== 批量查询 ====================
    
    /**
     * 获取日期范围内的所有记录
     * @param startDate 开始日期（含）
     * @param endDate 结束日期（含）
     */
    @Query("""
        SELECT * FROM sorting_records 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date ASC
    """)
    fun getRecordsInRange(startDate: String, endDate: String): Flow<List<SortingRecordEntity>>
    
    /**
     * 获取最近 N 天的记录（按日期降序）
     * @param days 天数
     */
    @Query("SELECT * FROM sorting_records ORDER BY date DESC LIMIT :days")
    fun getRecentRecords(days: Int): Flow<List<SortingRecordEntity>>
    
    /**
     * 获取所有记录
     */
    @Query("SELECT * FROM sorting_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<SortingRecordEntity>>
    
    // ==================== 统计查询 ====================
    
    /**
     * 获取所有记录的汇总统计
     */
    @Query("""
        SELECT SUM(sorted_count) as total, 
               SUM(kept_count) as kept, 
               SUM(trashed_count) as trashed, 
               SUM(maybe_count) as maybe
        FROM sorting_records
    """)
    suspend fun getTotalStats(): PeriodStats?
    
    /**
     * 获取指定日期之后的统计（用于本周统计）
     * @param startDate 开始日期（含）
     */
    @Query("""
        SELECT SUM(sorted_count) as total, 
               SUM(kept_count) as kept, 
               SUM(trashed_count) as trashed, 
               SUM(maybe_count) as maybe
        FROM sorting_records 
        WHERE date >= :startDate
    """)
    suspend fun getStatsFromDate(startDate: String): PeriodStats?
    
    /**
     * 获取指定月份的统计
     * @param monthPrefix 月份前缀，格式 YYYY-MM
     */
    @Query("""
        SELECT SUM(sorted_count) as total, 
               SUM(kept_count) as kept, 
               SUM(trashed_count) as trashed, 
               SUM(maybe_count) as maybe
        FROM sorting_records 
        WHERE date LIKE :monthPrefix || '%'
    """)
    suspend fun getMonthStats(monthPrefix: String): PeriodStats?
    
    /**
     * 获取有整理记录的天数（用于连续天数计算）
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    @Query("""
        SELECT COUNT(*) FROM sorting_records 
        WHERE sorted_count > 0 
        AND date >= :startDate
        AND date <= :endDate
    """)
    suspend fun getActiveDaysInRange(startDate: String, endDate: String): Int
    
    /**
     * 检查指定日期是否有整理记录
     */
    @Query("SELECT EXISTS(SELECT 1 FROM sorting_records WHERE date = :date AND sorted_count > 0)")
    suspend fun hasRecordOnDate(date: String): Boolean
    
    // ==================== 写入操作 ====================
    
    /**
     * 插入或替换记录（用于新建当日记录）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: SortingRecordEntity)
    
    /**
     * 增量更新当日记录
     * 
     * @param date 日期
     * @param sorted 增加的整理数
     * @param kept 增加的保留数
     * @param trashed 增加的删除数
     * @param maybe 增加的待定数
     * @return 更新的行数（0 表示记录不存在）
     */
    @Query("""
        UPDATE sorting_records 
        SET sorted_count = sorted_count + :sorted,
            kept_count = kept_count + :kept,
            trashed_count = trashed_count + :trashed,
            maybe_count = maybe_count + :maybe
        WHERE date = :date
    """)
    suspend fun incrementCounts(
        date: String, 
        sorted: Int, 
        kept: Int, 
        trashed: Int, 
        maybe: Int
    ): Int
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除指定日期的记录
     */
    @Query("DELETE FROM sorting_records WHERE date = :date")
    suspend fun deleteByDate(date: String)
    
    /**
     * 删除所有记录
     */
    @Query("DELETE FROM sorting_records")
    suspend fun deleteAll()
}
