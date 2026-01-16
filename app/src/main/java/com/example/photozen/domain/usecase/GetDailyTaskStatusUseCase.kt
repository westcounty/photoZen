package com.example.photozen.domain.usecase

import com.example.photozen.data.local.entity.DailyStats
import com.example.photozen.data.repository.DailyTaskMode
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
 */
class GetDailyTaskStatusUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<DailyTaskStatus> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
            
        return combine(
            preferencesRepository.getDailyTaskEnabled(),
            preferencesRepository.getDailyTaskTarget(),
            preferencesRepository.getDailyTaskMode(),
            photoRepository.getDailyStats(today)
        ) { enabled, target, mode, stats ->
            DailyTaskStatus(
                current = stats?.count ?: 0,
                target = target,
                isEnabled = enabled,
                mode = mode
            )
        }
    }
}
