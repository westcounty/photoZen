package com.example.photozen

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.photozen.data.local.dao.DailyStatsDao
import com.example.photozen.data.local.dao.SortingRecordDao
import com.example.photozen.data.local.entity.SortingRecordEntity
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.repository.StatsRepository
import com.example.photozen.service.DailyProgressService
import com.example.photozen.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PicZen Application class.
 * Entry point for Hilt dependency injection.
 * 
 * Implements Configuration.Provider for WorkManager with HiltWorkerFactory.
 * Uses lazy initialization to ensure Hilt injection is complete before accessing workerFactory.
 */
@HiltAndroidApp
class PicZenApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var dailyStatsDao: DailyStatsDao

    @Inject
    lateinit var sortingRecordDao: SortingRecordDao

    @Inject
    lateinit var statsRepository: StatsRepository

    override fun onCreate() {
        super.onCreate()
        
        // CrashLogger already initialized by CrashLoggerInitProvider (ContentProvider)
        // This is the full initialization which clears old logs and starts fresh session
        CrashLogger.init(this)
        CrashLogger.logStartupEvent(this, "Application.onCreate started")
        
        // Log Hilt injection status (workerFactory should be initialized at this point)
        val isWorkerFactoryInitialized = ::workerFactory.isInitialized
        CrashLogger.logStartupEvent(this, "Hilt injection complete, workerFactory initialized: $isWorkerFactoryInitialized")
        
        // Note: MapLibre initialization is now done lazily in MapLibreInitializer
        // to avoid potential initialization issues during app startup

        // 执行数据迁移（从 daily_stats 迁移到 sorting_records）
        migrateSortingRecords()

        // Start foreground progress service if enabled
        initProgressService()
        
        CrashLogger.logStartupEvent(this, "Application.onCreate completed")
        Log.i("PicZenApp", "Application initialization complete")
    }
    
    /**
     * 从 daily_stats 表迁移历史数据到 sorting_records 表。
     * 用于修复从 1.6 升级到 2.0 后连续整理天数丢失的问题。
     *
     * 迁移逻辑：
     * 1. 检查是否已经迁移过
     * 2. 读取 daily_stats 表中的所有历史记录
     * 3. 将每条记录转换为 sorting_records 格式并插入
     * 4. 标记迁移完成
     */
    private fun migrateSortingRecords() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 检查是否已迁移
                if (preferencesRepository.isSortingRecordsMigrated()) {
                    return@launch
                }

                // 读取 daily_stats 表中的所有历史记录
                val dailyStatsList = dailyStatsDao.getAllStatsSync()

                if (dailyStatsList.isNotEmpty()) {
                    CrashLogger.logStartupEvent(
                        this@PicZenApplication,
                        "开始迁移 ${dailyStatsList.size} 条历史整理记录"
                    )

                    // 将每条记录转换为 SortingRecordEntity 并插入
                    for (stats in dailyStatsList) {
                        // 检查 sorting_records 表中是否已有该日期的记录
                        val existingRecord = sortingRecordDao.getRecordByDate(stats.date)
                        if (existingRecord == null && stats.count > 0) {
                            // 只迁移有实际整理数的记录
                            val record = SortingRecordEntity(
                                date = stats.date,
                                sortedCount = stats.count,
                                // 旧版本没有详细分类，全部计入 kept
                                keptCount = stats.count,
                                trashedCount = 0,
                                maybeCount = 0,
                                createdAt = System.currentTimeMillis()
                            )
                            sortingRecordDao.upsert(record)
                        }
                    }

                    CrashLogger.logStartupEvent(
                        this@PicZenApplication,
                        "历史整理记录迁移完成"
                    )
                }

                // 迁移完成后，重新计算连续天数并更新到 DataStore
                // 这样成就系统也能获取到正确的连续天数
                val statsSummary = statsRepository.getStatsSummary()
                preferencesRepository.syncConsecutiveDays(statsSummary.consecutiveDays)

                CrashLogger.logStartupEvent(
                    this@PicZenApplication,
                    "连续天数已同步: ${statsSummary.consecutiveDays} 天"
                )

                // 标记迁移完成
                preferencesRepository.markSortingRecordsMigrated()

            } catch (e: Exception) {
                Log.e("PicZenApp", "数据迁移失败", e)
                CrashLogger.logStartupEvent(
                    this@PicZenApplication,
                    "数据迁移失败: ${e.message}"
                )
            }
        }
    }

    /**
     * Initialize foreground progress service on app startup.
     * This service displays daily progress in status bar and keeps the app alive.
     */
    private fun initProgressService() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = preferencesRepository.getProgressNotificationEnabled().first()
                if (enabled) {
                    // 使用主线程启动服务，确保稳定性
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        DailyProgressService.start(this@PicZenApplication)
                    }
                }
            } catch (e: Exception) {
                Log.e("PicZenApp", "Failed to start progress service", e)
                CrashLogger.logStartupEvent(this@PicZenApplication, "Progress service start failed: ${e.message}")
            }
        }
    }
    
    /**
     * WorkManager configuration using HiltWorkerFactory.
     * Uses lazy initialization to ensure Hilt has completed injection before accessing workerFactory.
     */
    override val workManagerConfiguration: Configuration by lazy {
        CrashLogger.logStartupEvent(this, "WorkManager config requested, workerFactory initialized: ${::workerFactory.isInitialized}")
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    }
}
