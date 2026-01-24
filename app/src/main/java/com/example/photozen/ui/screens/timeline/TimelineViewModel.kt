package com.example.photozen.ui.screens.timeline

import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.CustomFilterSession
import com.example.photozen.data.repository.GuideRepository
import com.example.photozen.data.repository.PhotoFilterMode
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.DeleteResult
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.EventGrouper
import com.example.photozen.domain.EventGrouper.Companion.getEffectiveTime
import com.example.photozen.domain.GroupingMode
import com.example.photozen.domain.PhotoEvent
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.PhotoBatchOperationUseCase
import com.example.photozen.domain.usecase.SortPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Represents a year-month combination for quick navigation.
 */
data class YearMonth(
    val year: Int,
    val month: Int,
    val eventCount: Int,
    val photoCount: Int,  // Total photos in this month
    val firstEventIndex: Int  // Index in the events list for scrolling
)

/**
 * UI State for Timeline screen.
 */
data class TimelineUiState(
    val events: List<PhotoEvent> = emptyList(),
    val groupingMode: GroupingMode = GroupingMode.AUTO,
    val isLoading: Boolean = true,
    val totalPhotos: Int = 0,
    val expandedEventIds: Set<String> = emptySet(),
    val error: String? = null,
    val navigateToSorter: Boolean = false,
    val navigateToSorterListMode: Boolean = false,  // Navigate to sorter in list mode
    val isDescending: Boolean = true,  // true=最新优先, false=最早优先
    val availableYearMonths: List<YearMonth> = emptyList(),  // 可用的年月列表
    val showNavigator: Boolean = false,  // 是否显示年月导航器
    val scrollToIndex: Int? = null,  // 要滚动到的索引
    // Phase 1-B: 选择模式状态
    val isSelectionMode: Boolean = false,
    val selectedPhotoIds: Set<String> = emptySet(),
    val message: String? = null  // 操作提示消息
) {
    val hasEvents: Boolean get() = events.isNotEmpty()
    val totalEvents: Int get() = events.size
    /** 选中的照片数量 */
    val selectedCount: Int get() = selectedPhotoIds.size
}

/**
 * ViewModel for Timeline screen.
 * Handles grouping photos into events and managing timeline state.
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val photoDao: PhotoDao,
    private val eventGrouper: EventGrouper,
    private val preferencesRepository: PreferencesRepository,
    private val albumBubbleDao: AlbumBubbleDao,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    // Phase 4: 批量操作 UseCase
    private val batchOperationUseCase: PhotoBatchOperationUseCase,
    val guideRepository: GuideRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()
    
    // Album list for picker
    val albumBubbleList: StateFlow<List<AlbumBubbleEntity>> = albumBubbleDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    init {
        loadTimeline()
    }
    
    /**
     * Load and group all photos into timeline events.
     */
    private fun loadTimeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                photoDao.getAllPhotos().collect { photos ->
                    // Filter out photos without valid effective time
                    // Uses getEffectiveTime() which prefers dateTaken, falls back to dateAdded * 1000
                    val validPhotos = photos.filter { it.getEffectiveTime() > 0 }
                    
                    // Group photos into events
                    val events = eventGrouper.groupPhotos(
                        validPhotos, 
                        _uiState.value.groupingMode
                    )
                    
                    // Apply sort order
                    val sortedEvents = applySortOrder(events, _uiState.value.isDescending)
                    
                    // Extract year-month data for navigation
                    val yearMonths = extractYearMonths(sortedEvents)
                    
                    _uiState.update { state ->
                        state.copy(
                            events = sortedEvents,
                            totalPhotos = validPhotos.size,
                            availableYearMonths = yearMonths,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "加载时间线失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Apply sort order to events list.
     */
    private fun applySortOrder(events: List<PhotoEvent>, isDescending: Boolean): List<PhotoEvent> {
        return if (isDescending) {
            events.sortedByDescending { it.startTime }
        } else {
            events.sortedBy { it.startTime }
        }
    }
    
    /**
     * Extract year-month data from events for quick navigation.
     */
    private fun extractYearMonths(events: List<PhotoEvent>): List<YearMonth> {
        val calendar = Calendar.getInstance()
        // Store both event indices and photo counts for each year-month
        data class YearMonthData(val indices: MutableList<Int> = mutableListOf(), var photoCount: Int = 0)
        val yearMonthMap = mutableMapOf<Pair<Int, Int>, YearMonthData>()
        
        events.forEachIndexed { index, event ->
            calendar.timeInMillis = event.startTime
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1  // Calendar.MONTH is 0-based
            val key = year to month
            val data = yearMonthMap.getOrPut(key) { YearMonthData() }
            data.indices.add(index)
            data.photoCount += event.photoCount
        }
        
        return yearMonthMap.map { (key, data) ->
            YearMonth(
                year = key.first,
                month = key.second,
                eventCount = data.indices.size,
                photoCount = data.photoCount,
                firstEventIndex = data.indices.first()
            )
        }.sortedWith(compareByDescending<YearMonth> { it.year }.thenByDescending { it.month })
    }
    
    /**
     * Change the grouping mode and re-group photos.
     */
    fun setGroupingMode(mode: GroupingMode) {
        if (mode == _uiState.value.groupingMode) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(groupingMode = mode, isLoading = true) }
            
            try {
                photoDao.getAllPhotos().collect { photos ->
                    val validPhotos = photos.filter { it.getEffectiveTime() > 0 }
                    val events = eventGrouper.groupPhotos(validPhotos, mode)
                    
                    // Apply sort order
                    val sortedEvents = applySortOrder(events, _uiState.value.isDescending)
                    
                    // Extract year-month data for navigation
                    val yearMonths = extractYearMonths(sortedEvents)
                    
                    _uiState.update { state ->
                        state.copy(
                            events = sortedEvents,
                            expandedEventIds = emptySet(), // Reset expanded state
                            availableYearMonths = yearMonths,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "切换分组模式失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Toggle sort order between ascending and descending.
     */
    fun toggleSortOrder() {
        val newIsDescending = !_uiState.value.isDescending
        val sortedEvents = applySortOrder(_uiState.value.events, newIsDescending)
        val yearMonths = extractYearMonths(sortedEvents)
        
        _uiState.update { state ->
            state.copy(
                isDescending = newIsDescending,
                events = sortedEvents,
                availableYearMonths = yearMonths
            )
        }
    }
    
    /**
     * Toggle event expanded state.
     */
    fun toggleEventExpanded(eventId: String) {
        _uiState.update { state ->
            val newExpanded = if (eventId in state.expandedEventIds) {
                state.expandedEventIds - eventId
            } else {
                state.expandedEventIds + eventId
            }
            state.copy(expandedEventIds = newExpanded)
        }
    }
    
    /**
     * Expand all events.
     */
    fun expandAll() {
        _uiState.update { state ->
            state.copy(expandedEventIds = state.events.map { it.id }.toSet())
        }
    }
    
    /**
     * Collapse all events.
     */
    fun collapseAll() {
        _uiState.update { it.copy(expandedEventIds = emptySet()) }
    }
    
    /**
     * Check if an event is expanded.
     */
    fun isEventExpanded(eventId: String): Boolean {
        return eventId in _uiState.value.expandedEventIds
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Sort a group of photos by setting up custom filter and triggering navigation.
     * Uses precise mode to ensure exact time range filtering.
     * 
     * NOTE: This does NOT change the global PhotoFilterMode. The session filter
     * with preciseMode=true is detected by FlowSorterViewModel and used directly.
     * This preserves the user's original filter mode setting.
     * 
     * @param startTime Start time of the event (milliseconds)
     * @param endTime End time of the event (milliseconds)
     * @param listMode Whether to navigate to list mode (for "view more" button)
     */
    fun sortGroup(startTime: Long, endTime: Long, listMode: Boolean = false) {
        viewModelScope.launch {
            // Set custom filter with precise mode
            // preciseMode=true signals to FlowSorterViewModel to use this filter
            // without requiring PhotoFilterMode.CUSTOM
            preferencesRepository.setSessionCustomFilter(
                CustomFilterSession(
                    albumIds = null,
                    startDate = startTime,
                    endDate = endTime,
                    preciseMode = true  // Use exact timestamps, no day extension
                )
            )
            
            // NOTE: We intentionally do NOT set PhotoFilterMode to CUSTOM here
            // The session filter with preciseMode=true will be detected and used
            // by FlowSorterViewModel, preserving the user's original filter setting
            
            // Trigger navigation
            _uiState.update { 
                it.copy(
                    navigateToSorter = !listMode,
                    navigateToSorterListMode = listMode
                ) 
            }
        }
    }
    
    /**
     * Reset navigation state after navigation is complete.
     */
    fun onNavigationComplete() {
        _uiState.update { 
            it.copy(
                navigateToSorter = false,
                navigateToSorterListMode = false
            ) 
        }
    }
    
    /**
     * Toggle the year-month navigator visibility.
     */
    fun toggleNavigator() {
        _uiState.update { it.copy(showNavigator = !it.showNavigator) }
    }
    
    /**
     * Hide the year-month navigator.
     */
    fun hideNavigator() {
        _uiState.update { it.copy(showNavigator = false) }
    }
    
    /**
     * Scroll to a specific year-month.
     */
    fun scrollToYearMonth(year: Int, month: Int) {
        val yearMonth = _uiState.value.availableYearMonths.find { 
            it.year == year && it.month == month 
        }
        yearMonth?.let {
            _uiState.update { state ->
                state.copy(
                    scrollToIndex = it.firstEventIndex,
                    showNavigator = false
                )
            }
        }
    }
    
    /**
     * Clear the scroll target after scrolling is complete.
     */
    fun clearScrollTarget() {
        _uiState.update { it.copy(scrollToIndex = null) }
    }
    
    // ==================== Phase 1-B: Selection Mode ====================
    
    /**
     * 进入选择模式并选中指定照片
     * 
     * 当用户长按照片时调用此方法。
     * 
     * @param photoId 要选中的照片ID
     */
    fun enterSelectionMode(photoId: String) {
        _uiState.update { it.copy(
            isSelectionMode = true,
            selectedPhotoIds = setOf(photoId)
        )}
    }
    
    /**
     * 切换照片的选中状态
     * 
     * 选择模式下点击照片时调用。
     * 如果选中数量变为0，自动退出选择模式。
     * 
     * @param photoId 照片ID
     */
    fun togglePhotoSelection(photoId: String) {
        _uiState.update { state ->
            val newSelection = if (photoId in state.selectedPhotoIds) {
                state.selectedPhotoIds - photoId
            } else {
                state.selectedPhotoIds + photoId
            }
            state.copy(
                selectedPhotoIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }
    
    /**
     * 清除所有选择并退出选择模式
     */
    fun clearSelection() {
        _uiState.update { it.copy(
            isSelectionMode = false,
            selectedPhotoIds = emptySet()
        )}
    }
    
    /**
     * 全选所有照片
     */
    fun selectAll() {
        _uiState.update { state ->
            val allPhotoIds = state.events.flatMap { event -> 
                event.photos.map { it.id } 
            }.toSet()
            state.copy(
                selectedPhotoIds = allPhotoIds,
                isSelectionMode = true
            )
        }
    }
    
    /**
     * 全选指定事件中的所有照片
     * 
     * @param event 照片事件
     */
    fun selectAllInEvent(event: PhotoEvent) {
        _uiState.update { state ->
            val newSelection = state.selectedPhotoIds + event.photos.map { it.id }
            state.copy(
                selectedPhotoIds = newSelection,
                isSelectionMode = true
            )
        }
    }
    
    /**
     * 批量更新选中照片的状态
     * 
     * Phase 4: 使用 PhotoBatchOperationUseCase 统一处理批量操作，
     * 支持撤销、Snackbar 反馈和统计记录。
     * 
     * @param status 目标状态
     */
    fun batchUpdateStatus(status: PhotoStatus) {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedPhotoIds.toList()
            if (selectedIds.isEmpty()) return@launch
            
            // Phase 4: 使用 BatchUseCase 执行批量操作
            // UseCase 内部已处理撤销、Snackbar、统计
            batchOperationUseCase.batchUpdateStatus(
                photoIds = selectedIds,
                newStatus = status,
                showUndo = true,
                showSnackbar = false // 我们使用本地消息显示
            ).onSuccess { result ->
                val statusLabel = when (status) {
                    PhotoStatus.KEEP -> "保留"
                    PhotoStatus.MAYBE -> "待定"
                    PhotoStatus.TRASH -> "删除"
                    PhotoStatus.UNSORTED -> "未筛选"
                }
                _uiState.update { it.copy(message = "已标记 ${result.affectedCount} 张为$statusLabel") }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "操作失败: ${e.message}") }
            }
            
            // 清除选择
            clearSelection()
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    // ==================== Photo Operations for Fullscreen Viewer ====================
    
    /**
     * Toggle photo status between MAYBE and KEEP.
     * If current status is MAYBE -> KEEP
     * If current status is KEEP or other -> MAYBE
     */
    fun togglePhotoStatus(photoId: String) {
        viewModelScope.launch {
            val photo = photoDao.getById(photoId) ?: return@launch
            
            when (photo.status) {
                PhotoStatus.MAYBE -> sortPhotoUseCase.keepPhoto(photoId)
                else -> sortPhotoUseCase.maybePhoto(photoId)
            }
        }
    }
    
    /**
     * Get delete intent sender for system delete confirmation.
     * @return IntentSender for delete request, or null if deletion failed
     */
    suspend fun getDeleteIntentSender(photoId: String): IntentSender? {
        val photo = photoDao.getById(photoId) ?: return null
        val uri = Uri.parse(photo.systemUri)
        
        return when (val result = mediaStoreDataSource.deletePhotos(listOf(uri))) {
            is DeleteResult.RequiresConfirmation -> result.intentSender
            is DeleteResult.Success -> null // Direct deletion succeeded
            is DeleteResult.Failed -> null
        }
    }
    
    /**
     * Handle photo deletion after user confirms.
     * Removes the photo from Room database.
     */
    fun onPhotoDeleted(photoId: String) {
        viewModelScope.launch {
            photoDao.deleteByIds(listOf(photoId))
        }
    }
    
    /**
     * Add photo to album (copy operation).
     */
    fun addPhotoToAlbum(photoId: String, albumBucketId: String) {
        viewModelScope.launch {
            val photo = photoDao.getById(photoId) ?: return@launch
            val photoUri = Uri.parse(photo.systemUri)
            
            // Get target album path
            val album = albumBubbleList.value.find { it.bucketId == albumBucketId }
            val targetPath = mediaStoreDataSource.getAlbumPath(albumBucketId)
                ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
            
            // Copy photo to album
            val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
            if (result.isSuccess) {
                _uiState.update { it.copy(error = null) }
            } else {
                _uiState.update { it.copy(error = "添加到相册失败") }
            }
        }
    }
}
