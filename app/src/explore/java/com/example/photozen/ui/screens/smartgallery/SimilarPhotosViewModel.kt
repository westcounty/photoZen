package com.example.photozen.ui.screens.smartgallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.ai.VectorSearchService
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.PhotoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Similar Photos Screen.
 */
data class SimilarPhotosUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val scanStatusText: String = "",
    val similarGroups: List<SimilarGroupUiModel> = emptyList(),
    val selectedGroupId: String? = null,
    val threshold: Float = 0.85f,
    val totalPhotosScanned: Int = 0,
    val estimatedSavings: Long = 0L, // Bytes that could be saved
    val error: String? = null,
    val isEmbeddingAvailable: Boolean = true
)

/**
 * UI model for a group of similar photos.
 */
data class SimilarGroupUiModel(
    val groupId: String,
    val photos: List<PhotoEntity>,
    val averageSimilarity: Float,
    val representativePhoto: PhotoEntity,
    val isExpanded: Boolean = false,
    val totalSize: Long = 0L
)

/**
 * ViewModel for Similar Photos detection and management.
 */
@HiltViewModel
class SimilarPhotosViewModel @Inject constructor(
    private val vectorSearchService: VectorSearchService,
    private val photoDao: PhotoDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SimilarPhotosUiState())
    val uiState: StateFlow<SimilarPhotosUiState> = _uiState.asStateFlow()
    
    private var scanJob: Job? = null
    
    init {
        checkEmbeddingAvailability()
    }
    
    /**
     * Check if image embedding is available.
     */
    private fun checkEmbeddingAvailability() {
        val available = vectorSearchService.isEmbeddingAvailable()
        _uiState.update { it.copy(isEmbeddingAvailable = available) }
    }
    
    /**
     * Start scanning for similar photos.
     */
    fun startScan() {
        if (_uiState.value.isScanning) return
        
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isScanning = true, 
                    scanProgress = 0f,
                    scanStatusText = "正在准备扫描...",
                    similarGroups = emptyList(),
                    error = null
                )
            }
            
            try {
                vectorSearchService.detectSimilarGroups(
                    threshold = _uiState.value.threshold
                ).collect { (progress, groups) ->
                    val progressPercent = if (progress.total > 0) {
                        progress.current.toFloat() / progress.total
                    } else 0f
                    
                    _uiState.update { 
                        it.copy(
                            scanProgress = progressPercent,
                            scanStatusText = if (groups.isEmpty()) {
                                "正在比较照片... ${progress.current}/${progress.total}"
                            } else {
                                "扫描完成，发现 ${groups.size} 组相似照片"
                            },
                            totalPhotosScanned = progress.total
                        )
                    }
                    
                    // Final update with groups
                    if (groups.isNotEmpty() || progress.current == progress.total) {
                        val groupUiModels = groups.map { group ->
                            createGroupUiModel(group)
                        }
                        
                        val totalSavings = groupUiModels.sumOf { group ->
                            // Estimate: all photos except one in each group could be deleted
                            group.photos.drop(1).sumOf { it.size }
                        }
                        
                        _uiState.update { 
                            it.copy(
                                isScanning = false,
                                similarGroups = groupUiModels,
                                estimatedSavings = totalSavings,
                                scanStatusText = if (groupUiModels.isEmpty()) {
                                    "没有发现相似照片"
                                } else {
                                    "发现 ${groupUiModels.size} 组相似照片"
                                }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isScanning = false,
                        error = "扫描失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Stop the current scan.
     */
    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { 
            it.copy(
                isScanning = false,
                scanStatusText = "扫描已停止"
            )
        }
    }
    
    /**
     * Update similarity threshold.
     */
    fun updateThreshold(threshold: Float) {
        _uiState.update { it.copy(threshold = threshold) }
    }
    
    /**
     * Toggle group expansion.
     */
    fun toggleGroupExpansion(groupId: String) {
        _uiState.update { state ->
            val updatedGroups = state.similarGroups.map { group ->
                if (group.groupId == groupId) {
                    group.copy(isExpanded = !group.isExpanded)
                } else {
                    group
                }
            }
            state.copy(
                similarGroups = updatedGroups,
                selectedGroupId = if (state.selectedGroupId == groupId) null else groupId
            )
        }
    }
    
    /**
     * Keep only the best photo in a group and trash others.
     */
    fun keepBestInGroup(groupId: String) {
        viewModelScope.launch {
            val group = _uiState.value.similarGroups.find { it.groupId == groupId } ?: return@launch
            
            // Keep the first (representative) photo, trash the rest
            val photosToTrash = group.photos.drop(1)
            
            try {
                photosToTrash.forEach { photo ->
                    photoDao.updateStatus(photo.id, com.example.photozen.data.model.PhotoStatus.TRASH)
                }
                
                // Remove the group from the list
                _uiState.update { state ->
                    state.copy(
                        similarGroups = state.similarGroups.filter { it.groupId != groupId },
                        estimatedSavings = state.estimatedSavings - photosToTrash.sumOf { it.size }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "操作失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Keep all photos in a group (remove from similar list).
     */
    fun keepAllInGroup(groupId: String) {
        _uiState.update { state ->
            val group = state.similarGroups.find { it.groupId == groupId }
            state.copy(
                similarGroups = state.similarGroups.filter { it.groupId != groupId },
                estimatedSavings = state.estimatedSavings - (group?.photos?.drop(1)?.sumOf { it.size } ?: 0)
            )
        }
    }
    
    /**
     * Create UI model from service model.
     */
    private suspend fun createGroupUiModel(
        group: VectorSearchService.SimilarPhotoGroup
    ): SimilarGroupUiModel {
        val photos = group.photoIds.mapNotNull { photoId ->
            photoDao.getById(photoId)
        }
        
        val representativePhoto = photos.firstOrNull() ?: photos.first()
        val totalSize = photos.sumOf { it.size }
        
        return SimilarGroupUiModel(
            groupId = group.groupId,
            photos = photos,
            averageSimilarity = group.averageSimilarity,
            representativePhoto = representativePhoto,
            totalSize = totalSize
        )
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Format bytes to human readable string.
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
