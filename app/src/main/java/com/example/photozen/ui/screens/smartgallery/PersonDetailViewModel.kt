package com.example.photozen.ui.screens.smartgallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.photozen.ai.FaceClusteringService
import com.example.photozen.data.local.dao.FaceDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.FaceEntity
import com.example.photozen.data.local.entity.PersonEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Face with its photo information.
 */
data class FaceWithPhoto(
    val face: FaceEntity,
    val photo: PhotoEntity?
)

/**
 * Similar person suggestion for merge.
 */
data class SimilarPersonSuggestion(
    val person: PersonEntity,
    val similarity: Float,
    val coverPhotoUri: String?
)

/**
 * UI State for Person Detail screen.
 */
data class PersonDetailUiState(
    val isLoading: Boolean = true,
    val person: PersonEntity? = null,
    val coverPhotoUri: String? = null,
    val photos: List<PhotoEntity> = emptyList(),
    val faces: List<FaceWithPhoto> = emptyList(),
    val similarPersons: List<SimilarPersonSuggestion> = emptyList(),
    val allPersons: List<PersonEntity> = emptyList(), // For move face dialog
    val showFaceManagement: Boolean = false,
    val isRenaming: Boolean = false,
    val isMerging: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val navigateBack: Boolean = false
) {
    val displayName: String
        get() = person?.name ?: "未命名"
    
    val photoCount: Int
        get() = photos.size
    
    val faceCount: Int
        get() = faces.size
}

/**
 * ViewModel for Person Detail screen.
 * Manages person information, photos, faces, and editing operations.
 */
@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val faceDao: FaceDao,
    private val photoDao: PhotoDao,
    private val faceClusteringService: FaceClusteringService
) : ViewModel() {
    
    private val personId: String = savedStateHandle.toRoute<Screen.PersonDetail>().personId
    
    private val _isLoading = MutableStateFlow(true)
    private val _showFaceManagement = MutableStateFlow(false)
    private val _isRenaming = MutableStateFlow(false)
    private val _isMerging = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)
    private val _navigateBack = MutableStateFlow(false)
    private val _similarPersons = MutableStateFlow<List<SimilarPersonSuggestion>>(emptyList())
    
    val uiState: StateFlow<PersonDetailUiState> = combine(
        faceDao.getPersonFlow(personId),
        faceDao.getPhotoIdsForPerson(personId).flatMapLatest { photoIds ->
            if (photoIds.isEmpty()) flowOf(emptyList())
            else flowOf(photoIds.mapNotNull { photoDao.getById(it) })
        },
        faceDao.getFacesByPersonId(personId),
        faceDao.getAllPersons(),
        _isLoading,
        _showFaceManagement,
        _isRenaming,
        _isMerging,
        _error,
        _message,
        _navigateBack,
        _similarPersons
    ) { values ->
        val person = values[0] as PersonEntity?
        val photos = values[1] as List<PhotoEntity>
        val faces = values[2] as List<FaceEntity>
        val allPersons = values[3] as List<PersonEntity>
        val isLoading = values[4] as Boolean
        val showFaceManagement = values[5] as Boolean
        val isRenaming = values[6] as Boolean
        val isMerging = values[7] as Boolean
        val error = values[8] as String?
        val message = values[9] as String?
        val navigateBack = values[10] as Boolean
        val similarPersons = values[11] as List<SimilarPersonSuggestion>
        
        // Get cover photo URI
        val coverPhotoUri = person?.coverFaceId?.let { coverFaceId ->
            val coverFace = faces.find { it.id == coverFaceId }
            coverFace?.photoId?.let { photoId ->
                photos.find { it.id == photoId }?.systemUri
            }
        }
        
        // Map faces with their photos
        val facesWithPhotos = faces.map { face ->
            FaceWithPhoto(face, photos.find { it.id == face.photoId })
        }
        
        PersonDetailUiState(
            isLoading = isLoading,
            person = person,
            coverPhotoUri = coverPhotoUri,
            photos = photos,
            faces = facesWithPhotos,
            similarPersons = similarPersons,
            allPersons = allPersons.filter { it.id != personId },
            showFaceManagement = showFaceManagement,
            isRenaming = isRenaming,
            isMerging = isMerging,
            error = error,
            message = message,
            navigateBack = navigateBack
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PersonDetailUiState()
    )
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = false
            
            // Load similar persons
            loadSimilarPersons()
        }
    }
    
    private suspend fun loadSimilarPersons() {
        try {
            val similar = faceClusteringService.findSimilarPersons(personId)
            val suggestions = similar.map { (person, similarity) ->
                val coverFace = faceDao.getFace(person.coverFaceId)
                val coverPhotoUri = coverFace?.photoId?.let { photoId ->
                    photoDao.getById(photoId)?.systemUri
                }
                SimilarPersonSuggestion(person, similarity, coverPhotoUri)
            }
            _similarPersons.value = suggestions
        } catch (e: Exception) {
            // Ignore errors loading similar persons
        }
    }
    
    /**
     * Rename the person.
     */
    fun renamePerson(newName: String) {
        viewModelScope.launch {
            try {
                val trimmedName = newName.trim().ifEmpty { null }
                faceDao.updatePersonName(personId, trimmedName)
                _message.value = if (trimmedName != null) "已重命名为「$trimmedName」" else "已清除名称"
                _isRenaming.value = false
            } catch (e: Exception) {
                _error.value = "重命名失败: ${e.message}"
            }
        }
    }
    
    /**
     * Toggle favorite status.
     */
    fun toggleFavorite() {
        viewModelScope.launch {
            val currentPerson = uiState.value.person ?: return@launch
            faceDao.updatePersonFavorite(personId, !currentPerson.isFavorite)
            _message.value = if (currentPerson.isFavorite) "已取消收藏" else "已收藏"
        }
    }
    
    /**
     * Hide the person.
     */
    fun hidePerson() {
        viewModelScope.launch {
            faceDao.updatePersonHidden(personId, true)
            _message.value = "人物已隐藏"
            _navigateBack.value = true
        }
    }
    
    /**
     * Delete the person and unassign all faces.
     */
    fun deletePerson() {
        viewModelScope.launch {
            try {
                faceDao.unassignAllFacesFromPerson(personId)
                faceDao.deletePerson(personId)
                _message.value = "人物已删除"
                _navigateBack.value = true
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            }
        }
    }
    
    /**
     * Set a face as the cover image.
     */
    fun setCoverFace(faceId: String) {
        viewModelScope.launch {
            try {
                faceDao.updatePersonCover(personId, faceId)
                _message.value = "已更新封面"
            } catch (e: Exception) {
                _error.value = "更新封面失败: ${e.message}"
            }
        }
    }
    
    /**
     * Remove a face from this person (unassign).
     */
    fun removeFace(faceId: String) {
        viewModelScope.launch {
            try {
                faceClusteringService.removeFaceFromPerson(faceId)
                _message.value = "已移除人脸"
                
                // Check if person has no faces left
                val remainingFaces = faceDao.getFaceCountForPerson(personId)
                if (remainingFaces == 0) {
                    _navigateBack.value = true
                }
            } catch (e: Exception) {
                _error.value = "移除失败: ${e.message}"
            }
        }
    }
    
    /**
     * Move a face to another person.
     */
    fun moveFaceToPerson(faceId: String, targetPersonId: String) {
        viewModelScope.launch {
            try {
                faceClusteringService.removeFaceFromPerson(faceId)
                faceClusteringService.assignFaceToPerson(faceId, targetPersonId)
                _message.value = "已移动人脸"
                
                // Check if person has no faces left
                val remainingFaces = faceDao.getFaceCountForPerson(personId)
                if (remainingFaces == 0) {
                    _navigateBack.value = true
                }
            } catch (e: Exception) {
                _error.value = "移动失败: ${e.message}"
            }
        }
    }
    
    /**
     * Split a face to create a new person.
     */
    fun splitFaceToNewPerson(faceId: String) {
        viewModelScope.launch {
            try {
                val newPersonId = faceClusteringService.splitFaceToNewPerson(faceId)
                if (newPersonId != null) {
                    _message.value = "已创建新人物"
                    
                    // Check if person has no faces left
                    val remainingFaces = faceDao.getFaceCountForPerson(personId)
                    if (remainingFaces == 0) {
                        _navigateBack.value = true
                    }
                } else {
                    _error.value = "分离失败"
                }
            } catch (e: Exception) {
                _error.value = "分离失败: ${e.message}"
            }
        }
    }
    
    /**
     * Merge this person with another.
     */
    fun mergeWithPerson(targetPersonId: String) {
        viewModelScope.launch {
            try {
                _isMerging.value = true
                faceClusteringService.mergePersons(targetPersonId, personId)
                _message.value = "已合并人物"
                _navigateBack.value = true
            } catch (e: Exception) {
                _error.value = "合并失败: ${e.message}"
            } finally {
                _isMerging.value = false
            }
        }
    }
    
    /**
     * Toggle face management panel visibility.
     */
    fun toggleFaceManagement() {
        _showFaceManagement.update { !it }
    }
    
    /**
     * Show rename dialog.
     */
    fun showRenameDialog() {
        _isRenaming.value = true
    }
    
    /**
     * Hide rename dialog.
     */
    fun hideRenameDialog() {
        _isRenaming.value = false
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
    
    /**
     * Reset navigation flag.
     */
    fun onNavigationHandled() {
        _navigateBack.value = false
    }
}
