package com.example.photozen.ui.screens.smartgallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.ai.AIAnalysisManager
import com.example.photozen.ai.FaceClusteringService
import com.example.photozen.ai.FaceEmbedding
import com.example.photozen.data.local.dao.FaceDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.PersonEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Person with cover photo URI for display.
 */
data class PersonWithCover(
    val person: PersonEntity,
    val coverPhotoUri: String?
)

/**
 * UI State for Person List screen.
 */
data class PersonListUiState(
    val isLoading: Boolean = true,
    val persons: List<PersonWithCover> = emptyList(),
    val totalFaces: Int = 0,
    val unassignedFaces: Int = 0,
    val isClusteringRunning: Boolean = false,
    val clusteringProgress: Int = 0,
    val clusteringMessage: String = "",
    val isEmbeddingRunning: Boolean = false,
    val facesWithoutEmbedding: Int = 0,
    val showHiddenPersons: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null,
    val message: String? = null,
    // 人脸嵌入模型可用性
    val isFaceEmbeddingAvailable: Boolean = true,
    val faceEmbeddingStatus: String? = null
) {
    val filteredPersons: List<PersonWithCover>
        get() = persons.filter { personWithCover ->
            val person = personWithCover.person
            // Filter by hidden status
            val showByHidden = showHiddenPersons || !person.isHidden
            // Filter by search query
            val showBySearch = searchQuery.isEmpty() || 
                person.name?.contains(searchQuery, ignoreCase = true) == true
            showByHidden && showBySearch
        }
    
    val visiblePersonCount: Int
        get() = filteredPersons.size
    
    val hasPersons: Boolean
        get() = persons.isNotEmpty()
}

/**
 * ViewModel for Person List screen.
 * Manages person data, clustering, and face embedding operations.
 */
@HiltViewModel
class PersonListViewModel @Inject constructor(
    private val faceDao: FaceDao,
    private val photoDao: PhotoDao,
    private val faceClusteringService: FaceClusteringService,
    private val aiAnalysisManager: AIAnalysisManager,
    private val faceEmbedding: FaceEmbedding
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(true)
    private val _isClusteringRunning = MutableStateFlow(false)
    private val _clusteringProgress = MutableStateFlow(0)
    private val _clusteringMessage = MutableStateFlow("")
    private val _showHiddenPersons = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _error = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)
    private val _isFaceEmbeddingAvailable = MutableStateFlow(true)
    private val _faceEmbeddingStatus = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<PersonListUiState> = combine(
        faceDao.getAllPersons(),
        faceDao.getTotalFaceCountFlow(),
        faceDao.getPersonCountFlow(),
        _isLoading,
        _isClusteringRunning,
        _clusteringProgress,
        _clusteringMessage,
        _showHiddenPersons,
        _searchQuery,
        _error,
        _message,
        _isFaceEmbeddingAvailable,
        _faceEmbeddingStatus
    ) { values ->
        val persons = values[0] as List<PersonEntity>
        val totalFaces = values[1] as Int
        @Suppress("UNUSED_VARIABLE")
        val personCount = values[2] as Int
        val isLoading = values[3] as Boolean
        val isClusteringRunning = values[4] as Boolean
        val clusteringProgress = values[5] as Int
        val clusteringMessage = values[6] as String
        val showHiddenPersons = values[7] as Boolean
        val searchQuery = values[8] as String
        val error = values[9] as String?
        val message = values[10] as String?
        val isFaceEmbeddingAvailable = values[11] as Boolean
        val faceEmbeddingStatus = values[12] as String?
        
        // Load cover photo URIs
        val personsWithCovers = persons.map { person ->
            val coverPhotoUri = getCoverPhotoUri(person)
            PersonWithCover(person, coverPhotoUri)
        }
        
        PersonListUiState(
            isLoading = isLoading,
            persons = personsWithCovers,
            totalFaces = totalFaces,
            unassignedFaces = 0, // Will be calculated separately if needed
            isClusteringRunning = isClusteringRunning,
            clusteringProgress = clusteringProgress,
            clusteringMessage = clusteringMessage,
            showHiddenPersons = showHiddenPersons,
            searchQuery = searchQuery,
            error = error,
            message = message,
            isFaceEmbeddingAvailable = isFaceEmbeddingAvailable,
            faceEmbeddingStatus = faceEmbeddingStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PersonListUiState()
    )
    
    init {
        // 检查人脸嵌入模型可用性
        checkFaceEmbeddingAvailability()
        
        viewModelScope.launch {
            _isLoading.value = false
        }
        
        // Monitor embedding status
        viewModelScope.launch {
            aiAnalysisManager.getFaceEmbeddingStatusFlow().collect { status ->
                // Could update UI based on embedding progress if needed
            }
        }
    }
    
    /**
     * 检查人脸嵌入模型是否可用。
     * 如果模型不可用，自动聚类功能将被禁用。
     */
    private fun checkFaceEmbeddingAvailability() {
        val available = faceEmbedding.isModelAvailable()
        val status = when (val modelStatus = faceEmbedding.getModelStatus()) {
            is FaceEmbedding.ModelStatus.Missing -> "人脸嵌入模型未找到，请下载 arcface.tflite"
            is FaceEmbedding.ModelStatus.Error -> modelStatus.message
            is FaceEmbedding.ModelStatus.NotInitialized -> if (!available) "人脸嵌入模型未初始化" else null
            is FaceEmbedding.ModelStatus.Ready -> null
        }
        _isFaceEmbeddingAvailable.value = available
        _faceEmbeddingStatus.value = status
    }
    
    /**
     * Get cover photo URI for a person.
     */
    private suspend fun getCoverPhotoUri(person: PersonEntity): String? {
        val coverFace = faceDao.getFace(person.coverFaceId) ?: return null
        val photo = photoDao.getById(coverFace.photoId) ?: return null
        return photo.systemUri
    }
    
    /**
     * Start face clustering.
     */
    fun startClustering() {
        if (_isClusteringRunning.value) return
        
        // 检查模型是否可用
        if (!_isFaceEmbeddingAvailable.value) {
            _error.value = "人脸嵌入模型不可用，无法进行自动聚类。请下载 arcface.tflite 模型文件。"
            return
        }
        
        viewModelScope.launch {
            _isClusteringRunning.value = true
            _clusteringProgress.value = 0
            _clusteringMessage.value = "准备聚类..."
            
            try {
                faceClusteringService.runClustering { current, total, message ->
                    _clusteringProgress.value = if (total > 0) (current * 100) / total else 0
                    _clusteringMessage.value = message
                }
                _message.value = "聚类完成"
            } catch (e: Exception) {
                _error.value = "聚类失败: ${e.message}"
            } finally {
                _isClusteringRunning.value = false
                _clusteringProgress.value = 0
                _clusteringMessage.value = ""
            }
        }
    }
    
    /**
     * Start face embedding generation.
     */
    fun startFaceEmbedding() {
        aiAnalysisManager.startFaceEmbedding()
        _message.value = "开始生成人脸特征..."
    }
    
    /**
     * Toggle showing hidden persons.
     */
    fun toggleShowHiddenPersons() {
        _showHiddenPersons.update { !it }
    }
    
    /**
     * Update search query.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Hide a person.
     */
    fun hidePerson(personId: String) {
        viewModelScope.launch {
            faceDao.updatePersonHidden(personId, true)
            _message.value = "人物已隐藏"
        }
    }
    
    /**
     * Unhide a person.
     */
    fun unhidePerson(personId: String) {
        viewModelScope.launch {
            faceDao.updatePersonHidden(personId, false)
            _message.value = "人物已显示"
        }
    }
    
    /**
     * Delete a person and unassign all their faces.
     */
    fun deletePerson(personId: String) {
        viewModelScope.launch {
            try {
                faceDao.unassignAllFacesFromPerson(personId)
                faceDao.deletePerson(personId)
                _message.value = "人物已删除"
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Clear message.
     */
    fun clearMessage() {
        _message.value = null
    }
}
