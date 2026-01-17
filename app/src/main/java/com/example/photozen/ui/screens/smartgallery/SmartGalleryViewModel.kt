package com.example.photozen.ui.screens.smartgallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.ai.AIAnalysisManager
import com.example.photozen.data.local.dao.FaceDao
import com.example.photozen.data.local.dao.PhotoAnalysisDao
import com.example.photozen.data.local.dao.PhotoDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Smart Gallery screen.
 */
data class SmartGalleryUiState(
    val totalPhotos: Int = 0,
    val analyzedPhotos: Int = 0,
    val unanalyzedPhotos: Int = 0,
    val photosWithGps: Int = 0,
    val totalFaces: Int = 0,
    val totalPersons: Int = 0,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val error: String? = null
) {
    val analysisPercentage: Int
        get() = if (totalPhotos > 0) ((analyzedPhotos.toFloat() / totalPhotos) * 100).toInt() else 0
    
    val hasAnalyzedPhotos: Boolean
        get() = analyzedPhotos > 0
}

/**
 * Feature item for Smart Gallery grid.
 */
data class SmartGalleryFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val count: Int = 0,
    val isEnabled: Boolean = true
)

/**
 * ViewModel for Smart Gallery screen.
 * Manages AI analysis status and feature access.
 */
@HiltViewModel
class SmartGalleryViewModel @Inject constructor(
    private val photoDao: PhotoDao,
    private val photoAnalysisDao: PhotoAnalysisDao,
    private val faceDao: FaceDao,
    private val aiAnalysisManager: AIAnalysisManager
) : ViewModel() {
    
    private val _isAnalyzing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<SmartGalleryUiState> = combine(
        photoDao.getTotalCount(),
        photoAnalysisDao.getAnalyzedCountFlow(),
        photoAnalysisDao.getUnanalyzedCountFlow(),
        faceDao.getTotalFaceCountFlow(),
        faceDao.getPersonCountFlow(),
        photoDao.getPhotosWithGpsCount(),
        _isAnalyzing,
        _error
    ) { values ->
        val totalPhotos = values[0] as Int
        val analyzedPhotos = values[1] as Int
        val unanalyzedPhotos = values[2] as Int
        val totalFaces = values[3] as Int
        val totalPersons = values[4] as Int
        val photosWithGps = values[5] as Int
        val isAnalyzing = values[6] as Boolean
        val error = values[7] as String?
        
        SmartGalleryUiState(
            totalPhotos = totalPhotos,
            analyzedPhotos = analyzedPhotos,
            unanalyzedPhotos = unanalyzedPhotos,
            photosWithGps = photosWithGps,
            totalFaces = totalFaces,
            totalPersons = totalPersons,
            isAnalyzing = isAnalyzing,
            analysisProgress = if (totalPhotos > 0) analyzedPhotos.toFloat() / totalPhotos else 0f,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SmartGalleryUiState()
    )
    
    /**
     * Get list of available features with their current counts.
     */
    fun getFeatures(state: SmartGalleryUiState): List<SmartGalleryFeature> {
        return listOf(
            SmartGalleryFeature(
                id = "faces",
                title = "人物相册",
                subtitle = "${state.totalPersons} 个人物",
                iconName = "Face",
                count = state.totalPersons,
                isEnabled = state.totalPersons > 0
            ),
            SmartGalleryFeature(
                id = "labels",
                title = "智能标签",
                subtitle = "${state.analyzedPhotos} 张已分析",
                iconName = "Label",
                count = state.analyzedPhotos,
                isEnabled = state.analyzedPhotos > 0
            ),
            SmartGalleryFeature(
                id = "search",
                title = "智能搜索",
                subtitle = "用自然语言搜索",
                iconName = "Search",
                count = 0,
                isEnabled = state.analyzedPhotos > 0
            ),
            SmartGalleryFeature(
                id = "similar",
                title = "相似照片",
                subtitle = "找出重复或相似",
                iconName = "ContentCopy",
                count = 0,
                isEnabled = state.analyzedPhotos > 0
            ),
            SmartGalleryFeature(
                id = "map",
                title = "地图视图",
                subtitle = "${state.photosWithGps} 张有位置",
                iconName = "Map",
                count = state.photosWithGps,
                isEnabled = state.photosWithGps > 0
            ),
            SmartGalleryFeature(
                id = "timeline",
                title = "智能时间线",
                subtitle = "按事件自动分组",
                iconName = "Timeline",
                count = state.totalPhotos,
                isEnabled = state.totalPhotos > 0
            )
        )
    }
    
    /**
     * Start AI analysis for all unanalyzed photos.
     */
    fun startAnalysis() {
        if (_isAnalyzing.value) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                aiAnalysisManager.startAnalysis()
            } catch (e: Exception) {
                _error.value = "分析启动失败: ${e.message}"
                _isAnalyzing.value = false
            }
        }
    }
    
    /**
     * Stop ongoing AI analysis.
     */
    fun stopAnalysis() {
        viewModelScope.launch {
            aiAnalysisManager.stopAnalysis()
            _isAnalyzing.value = false
        }
    }
    
    init {
        // Monitor analysis status from WorkManager
        viewModelScope.launch {
            aiAnalysisManager.getAnalysisStatusFlow().collect { status ->
                _isAnalyzing.value = status.isRunning
                
                // Auto-continue if batch completed but more photos remain
                if (status.shouldContinue && status.totalRemaining > 0) {
                    aiAnalysisManager.continueAnalysis()
                }
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
}
