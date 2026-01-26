package com.example.photozen.data.repository

import com.example.photozen.data.local.dao.SortingRecordDao
import com.example.photozen.data.local.entity.SortingRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统计摘要数据
 */
data class StatsSummary(
    /** 历史总整理数 */
    val totalSorted: Int,
    /** 本周整理数 */
    val weekSorted: Int,
    /** 本月整理数 */
    val monthSorted: Int,
    /** 本周保留数 */
    val weekKept: Int,
    /** 本周删除数 */
    val weekTrashed: Int,
    /** 本周待定数 */
    val weekMaybe: Int,
    /** 连续整理天数 */
    val consecutiveDays: Int
) {
    companion object {
        val EMPTY = StatsSummary(0, 0, 0, 0, 0, 0, 0)
    }
}

/**
 * 整理统计数据仓库
 * 
 * 提供整理记录的存储和查询功能，支持：
 * - 记录单次整理操作
 * - 获取统计摘要（总数、本周、本月）
 * - 获取日历热力图数据
 * - 计算连续整理天数
 */
@Singleton
class StatsRepository @Inject constructor(
    private val sortingRecordDao: SortingRecordDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * 记录单次整理操作
     * 
     * 自动处理当日记录的创建或增量更新。
     * 
     * @param keptCount 保留数量（默认0）
     * @param trashedCount 删除数量（默认0）
     * @param maybeCount 待定数量（默认0）
     */
    suspend fun recordSorting(
        keptCount: Int = 0,
        trashedCount: Int = 0,
        maybeCount: Int = 0
    ) {
        val today = dateFormat.format(Date())
        val totalSorted = keptCount + trashedCount + maybeCount
        
        // 先尝试增量更新
        val updatedRows = sortingRecordDao.incrementCounts(
            date = today,
            sorted = totalSorted,
            kept = keptCount,
            trashed = trashedCount,
            maybe = maybeCount
        )
        
        // 如果没有更新任何行，说明记录不存在，创建新记录
        if (updatedRows == 0) {
            sortingRecordDao.upsert(
                SortingRecordEntity(
                    date = today,
                    sortedCount = totalSorted,
                    keptCount = keptCount,
                    trashedCount = trashedCount,
                    maybeCount = maybeCount
                )
            )
        }
    }
    
    /**
     * 记录保留操作
     */
    suspend fun recordKeep(count: Int = 1) {
        recordSorting(keptCount = count)
    }
    
    /**
     * 记录删除操作
     */
    suspend fun recordTrash(count: Int = 1) {
        recordSorting(trashedCount = count)
    }
    
    /**
     * 记录待定操作
     */
    suspend fun recordMaybe(count: Int = 1) {
        recordSorting(maybeCount = count)
    }
    
    /**
     * 获取日历热力图数据
     * 
     * @param days 最近天数（默认90天，约3个月）
     * @return Map<日期字符串, 整理数量>
     */
    fun getCalendarData(days: Int = 90): Flow<Map<String, Int>> {
        return sortingRecordDao.getRecentRecords(days).map { records ->
            records.associate { it.date to it.sortedCount }
        }
    }
    
    /**
     * 获取统计摘要
     */
    suspend fun getStatsSummary(): StatsSummary {
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        
        // 计算本周开始日期（周一）
        val weekStart = getWeekStartDate()
        
        // 本月前缀
        val monthPrefix = today.substring(0, 7) // "YYYY-MM"
        
        // 获取各项统计
        val totalStats = sortingRecordDao.getTotalStats()
        val weekStats = sortingRecordDao.getStatsFromDate(weekStart)
        val monthStats = sortingRecordDao.getMonthStats(monthPrefix)
        val consecutiveDays = calculateConsecutiveDays()
        
        return StatsSummary(
            totalSorted = totalStats?.safeTotal ?: 0,
            weekSorted = weekStats?.safeTotal ?: 0,
            monthSorted = monthStats?.safeTotal ?: 0,
            weekKept = weekStats?.safeKept ?: 0,
            weekTrashed = weekStats?.safeTrashed ?: 0,
            weekMaybe = weekStats?.safeMaybe ?: 0,
            consecutiveDays = consecutiveDays
        )
    }

    /**
     * 观察统计摘要（响应式版本）
     * 当整理记录变化时自动更新
     */
    fun observeStatsSummary(): Flow<StatsSummary> {
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        val weekStart = getWeekStartDate()
        val monthPrefix = today.substring(0, 7)

        return combine(
            sortingRecordDao.observeTotalStats(),
            sortingRecordDao.observeStatsFromDate(weekStart),
            sortingRecordDao.observeMonthStats(monthPrefix),
            observeConsecutiveDays()
        ) { totalStats, weekStats, monthStats, consecutiveDays ->
            StatsSummary(
                totalSorted = totalStats?.safeTotal ?: 0,
                weekSorted = weekStats?.safeTotal ?: 0,
                monthSorted = monthStats?.safeTotal ?: 0,
                weekKept = weekStats?.safeKept ?: 0,
                weekTrashed = weekStats?.safeTrashed ?: 0,
                weekMaybe = weekStats?.safeMaybe ?: 0,
                consecutiveDays = consecutiveDays
            )
        }
    }

    /**
     * 观察连续整理天数（响应式版本）
     * 监听最近365天的记录变化，自动重新计算
     */
    private fun observeConsecutiveDays(): Flow<Int> {
        // 监听最近365天的记录，当有变化时重新计算连续天数
        return sortingRecordDao.getRecentRecords(365).map { records ->
            calculateConsecutiveDaysFromRecords(records)
        }
    }

    /**
     * 从记录列表计算连续整理天数
     */
    private fun calculateConsecutiveDaysFromRecords(records: List<SortingRecordEntity>): Int {
        if (records.isEmpty()) return 0

        val recordDates = records
            .filter { it.sortedCount > 0 }
            .map { it.date }
            .toSet()

        val calendar = Calendar.getInstance()
        var consecutiveDays = 0

        // 从今天开始往前数
        for (i in 0..365) {
            val date = dateFormat.format(calendar.time)

            if (date in recordDates) {
                consecutiveDays++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else if (i == 0) {
                // 今天还没整理，不算断，继续检查昨天
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // 之前某天没有整理，连续中断
                break
            }
        }

        return consecutiveDays
    }

    /**
     * 观察今日整理记录
     */
    fun observeTodayRecord(): Flow<SortingRecordEntity?> {
        val today = dateFormat.format(Date())
        return sortingRecordDao.observeRecordByDate(today)
    }
    
    /**
     * 获取今日整理数
     */
    suspend fun getTodaySortedCount(): Int {
        val today = dateFormat.format(Date())
        return sortingRecordDao.getRecordByDate(today)?.sortedCount ?: 0
    }
    
    /**
     * 获取本周开始日期（周一）
     */
    private fun getWeekStartDate(): String {
        val calendar = Calendar.getInstance()
        
        // 设置到本周一
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        // 如果当前是周日，Calendar 会将周一设置为下周一，需要回退一周
        val today = Calendar.getInstance()
        if (calendar.time.after(today.time)) {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
        }
        
        return dateFormat.format(calendar.time)
    }
    
    /**
     * 计算连续整理天数
     * 
     * 从今天开始往前数，连续有整理记录的天数。
     * 如果今天还没有整理，从昨天开始计算。
     */
    private suspend fun calculateConsecutiveDays(): Int {
        val calendar = Calendar.getInstance()
        var consecutiveDays = 0
        
        // 从今天开始检查
        for (i in 0..365) { // 最多检查一年
            val date = dateFormat.format(calendar.time)
            val hasRecord = sortingRecordDao.hasRecordOnDate(date)
            
            if (hasRecord) {
                consecutiveDays++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else if (i == 0) {
                // 今天还没整理，不算断，继续检查昨天
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // 之前某天没有整理，连续中断
                break
            }
        }
        
        return consecutiveDays
    }
    
    /**
     * 获取指定日期范围的记录
     */
    fun getRecordsInRange(startDate: String, endDate: String): Flow<List<SortingRecordEntity>> {
        return sortingRecordDao.getRecordsInRange(startDate, endDate)
    }
    
    /**
     * 清除所有统计记录（用于测试或重置）
     */
    suspend fun clearAllRecords() {
        sortingRecordDao.deleteAll()
    }
}
