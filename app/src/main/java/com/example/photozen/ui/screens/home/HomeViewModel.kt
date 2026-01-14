package com.example.photozen.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.di.LocationScanScheduler
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.SyncPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Home screen.
 */
data class HomeUiState(
    val totalPhotos: Int = 0,
    val unsortedCount: Int = 0,
    val keepCount: Int = 0,
    val trashCount: Int = 0,
    val maybeCount: Int = 0,
    val hasPermission: Boolean = false,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val syncResult: String? = null,
    val error: String? = null
) {
    val sortedCount: Int
        get() = keepCount + trashCount + maybeCount
    
    val progress: Float
        get() = if (totalPhotos > 0) sortedCount.toFloat() / totalPhotos else 0f
    
    val hasPhotos: Boolean
        get() = totalPhotos > 0
}

/**
 * ViewModel for Home screen.
 * Manages photo statistics and sync status.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPhotosUseCase: GetPhotosUseCase,
    private val syncPhotosUseCase: SyncPhotosUseCase,
    private val locationScanScheduler: LocationScanScheduler
) : ViewModel() {
    
    private val _hasPermission = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(true)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncResult = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)
    
    /**
     * UI State exposed to the screen.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        getPhotosUseCase.getTotalCount(),
        getPhotosUseCase.getCountByStatus(PhotoStatus.UNSORTED),
        getPhotosUseCase.getCountByStatus(PhotoStatus.KEEP),
        getPhotosUseCase.getCountByStatus(PhotoStatus.TRASH),
        getPhotosUseCase.getCountByStatus(PhotoStatus.MAYBE),
        _hasPermission,
        _isLoading,
        _isSyncing,
        _syncResult,
        _error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            totalPhotos = values[0] as Int,
            unsortedCount = values[1] as Int,
            keepCount = values[2] as Int,
            trashCount = values[3] as Int,
            maybeCount = values[4] as Int,
            hasPermission = values[5] as Boolean,
            isLoading = values[6] as Boolean,
            isSyncing = values[7] as Boolean,
            syncResult = values[8] as String?,
            error = values[9] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
    
    /**
     * Called when permission is granted.
     */
    fun onPermissionGranted() {
        _hasPermission.value = true
        syncPhotos()
    }
    
    /**
     * Called when permission is denied.
     */
    fun onPermissionDenied() {
        _hasPermission.value = false
        _isLoading.value = false
        _error.value = "需要存储权限才能访问照片"
    }
    
    /**
     * Sync photos from MediaStore.
     */
    fun syncPhotos() {
        if (!_hasPermission.value) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = syncPhotosUseCase()
                _syncResult.value = if (result.isInitialSync) {
                    "已导入 ${result.newPhotosCount} 张照片"
                } else if (result.newPhotosCount > 0) {
                    "发现 ${result.newPhotosCount} 张新照片"
                } else {
                    null
                }
                
                // Trigger GPS location scan for new photos
                if (result.newPhotosCount > 0 || result.isInitialSync) {
                    locationScanScheduler.scheduleOneTimeScan()
                }
            } catch (e: Exception) {
                _error.value = "同步失败: ${e.message}"
            } finally {
                _isSyncing.value = false
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear sync result message.
     */
    fun clearSyncResult() {
        _syncResult.value = null
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
}
