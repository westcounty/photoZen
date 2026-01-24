package com.example.photozen.domain.usecase

import com.example.photozen.data.local.entity.DailyStats
import com.example.photozen.data.repository.DailyTaskMode
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Status of the daily sorting task.
 */
data class DailyTaskStatus(
    val current: Int = 0,
    val target: Int = 100,
    val isEnabled: Boolean = true,
    val mode: DailyTaskMode = DailyTaskMode.FLOW
) {
    val isCompleted: Boolean
        get() = current >= target

    val progress: Float
        get() = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
}

/**
 * Use case to get the status of the daily task.
 *
 * REQ-065: 修复跨天进度不更新问题
 * - 使用 currentDateFlow 每分钟检查日期变化
 * - 当日期变化时自动切换到新日期的统计
 */
class GetDailyTaskStatusUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val preferencesRepository: PreferencesRepository
) {
    private val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

    /**
     * Flow that emits the current date string.
     * Checks for date changes every minute to handle midnight crossover (REQ-065).
     */
    private val currentDateFlow: Flow<String> = flow {
        while (true) {
            emit(dateFormat.format(java.util.Date()))
            delay(60_000L) // Check every minute
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<DailyTaskStatus> {
        return combine(
            preferencesRepository.getDailyTaskEnabled(),
            preferencesRepository.getDailyTaskTarget(),
            preferencesRepository.getDailyTaskMode(),
            currentDateFlow
        ) { enabled, target, mode, today ->
            // Return a tuple with config + today's date
            DailyTaskConfig(enabled, target, mode, today)
        }.flatMapLatest { config ->
            // When date changes, fetch new day's stats
            photoRepository.getDailyStats(config.today).map { stats ->
                DailyTaskStatus(
                    current = stats?.count ?: 0,
                    target = config.target,
                    isEnabled = config.enabled,
                    mode = config.mode
                )
            }
        }
    }

    private data class DailyTaskConfig(
        val enabled: Boolean,
        val target: Int,
        val mode: DailyTaskMode,
        val today: String
    )
}
