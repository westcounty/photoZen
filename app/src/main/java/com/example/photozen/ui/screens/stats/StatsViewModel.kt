package com.example.photozen.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.repository.StatsRepository
import com.example.photozen.data.repository.StatsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统计页面 UI 状态
 */
data class StatsUiState(
    /** 是否正在加载 */
    val isLoading: Boolean = true,
    /** 统计摘要数据 */
    val summary: StatsSummary = StatsSummary.EMPTY,
    /** 日历热力图数据 (日期 -> 整理数) */
    val calendarData: Map<String, Int> = emptyMap(),
    /** 选中的日期 */
    val selectedDate: String? = null,
    /** 选中日期的整理数 */
    val selectedDateCount: Int = 0,
    /** 错误信息 */
    val error: String? = null
)

/**
 * 统计页面 ViewModel
 * 
 * 管理统计数据的加载和展示。
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()
    
    init {
        loadStats()
        observeCalendarData()
    }
    
    /**
     * 加载统计数据
     */
    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val summary = statsRepository.getStatsSummary()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        summary = summary
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "加载统计数据失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * 观察日历热力图数据
     */
    private fun observeCalendarData() {
        statsRepository.getCalendarData(90)
            .onEach { data ->
                _uiState.update { it.copy(calendarData = data) }
            }
            .catch { e ->
                // 日历数据加载失败不影响主要统计
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * 处理日期点击
     */
    fun onDayClicked(date: String, count: Int) {
        _uiState.update { 
            it.copy(
                selectedDate = if (it.selectedDate == date) null else date,
                selectedDateCount = count
            ) 
        }
    }
    
    /**
     * 刷新统计数据
     */
    fun refresh() {
        loadStats()
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
