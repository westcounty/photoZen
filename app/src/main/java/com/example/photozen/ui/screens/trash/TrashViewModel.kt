package com.example.photozen.ui.screens.trash

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.ManageTrashUseCase
import com.example.photozen.domain.usecase.SortPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val deleteIntentSender: IntentSender? = null,
    val message: String? = null
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
    val selectedCount: Int get() = selectedIds.size
    val allSelected: Boolean get() = photos.isNotEmpty() && selectedIds.size == photos.size
}

private data class InternalState(
    val selectedIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val deleteIntentSender: IntentSender? = null,
    val message: String? = null
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val manageTrashUseCase: ManageTrashUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase
) : ViewModel() {
    
    private val _internalState = MutableStateFlow(InternalState())
    
    val uiState: StateFlow<TrashUiState> = combine(
        getPhotosUseCase.getPhotosByStatus(PhotoStatus.TRASH),
        _internalState
    ) { photos, internal ->
        TrashUiState(
            photos = photos,
            selectedIds = internal.selectedIds,
            isLoading = internal.isLoading && photos.isEmpty(),
            isDeleting = internal.isDeleting,
            deleteIntentSender = internal.deleteIntentSender,
            message = internal.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrashUiState()
    )
    
    init {
        viewModelScope.launch {
            _internalState.update { it.copy(isLoading = false) }
        }
    }
    
    fun toggleSelection(photoId: String) {
        _internalState.update { state ->
            val newSelection = if (photoId in state.selectedIds) {
                state.selectedIds - photoId
            } else {
                state.selectedIds + photoId
            }
            state.copy(selectedIds = newSelection)
        }
    }
    
    fun selectAll() {
        val allIds = uiState.value.photos.map { it.id }.toSet()
        _internalState.update { it.copy(selectedIds = allIds) }
    }
    
    fun clearSelection() {
        _internalState.update { it.copy(selectedIds = emptySet()) }
    }
    
    fun restoreSelected() {
        val selected = _internalState.value.selectedIds.toList()
        viewModelScope.launch {
            try {
                sortPhotoUseCase.batchUpdateStatus(selected, PhotoStatus.UNSORTED)
                _internalState.update { 
                    it.copy(selectedIds = emptySet(), message = "已恢复 ${selected.size} 张照片")
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "恢复失败") }
            }
        }
    }
    
    /**
     * Request permanent deletion via system API.
     * For Android 11+, this creates an IntentSender for user confirmation.
     * For older versions, we just remove from our database (actual file deletion 
     * would require WRITE_EXTERNAL_STORAGE which is restricted).
     */
    fun requestPermanentDelete() {
        val selectedPhotos = uiState.value.photos.filter { it.id in _internalState.value.selectedIds }
        if (selectedPhotos.isEmpty()) return
        
        viewModelScope.launch {
            _internalState.update { it.copy(isDeleting = true) }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+: Use MediaStore.createDeleteRequest
                    val uris = selectedPhotos.mapNotNull { photo ->
                        try {
                            Uri.parse(photo.systemUri)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (uris.isNotEmpty()) {
                        val intentSender = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            uris
                        ).intentSender
                        
                        _internalState.update { 
                            it.copy(deleteIntentSender = intentSender, isDeleting = false)
                        }
                    }
                } else {
                    // Older versions: Just remove selected photos from our database
                    // (actual file deletion not supported without special permissions)
                    val selectedIds = _internalState.value.selectedIds.toList()
                    val count = selectedIds.size
                    manageTrashUseCase.deletePhotos(selectedIds)
                    _internalState.update { 
                        it.copy(
                            selectedIds = emptySet(),
                            isDeleting = false,
                            message = "已从整理记录中移除 $count 张照片"
                        )
                    }
                }
            } catch (e: Exception) {
                _internalState.update { 
                    it.copy(isDeleting = false, message = "删除失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Called after system delete dialog completes.
     */
    fun onDeleteComplete(success: Boolean) {
        viewModelScope.launch {
            if (success) {
                // Remove only the selected photos from our database
                val selectedIds = _internalState.value.selectedIds.toList()
                val count = selectedIds.size
                manageTrashUseCase.deletePhotos(selectedIds)
                _internalState.update { 
                    it.copy(
                        selectedIds = emptySet(),
                        deleteIntentSender = null,
                        message = "已彻底删除 $count 张照片"
                    )
                }
            } else {
                _internalState.update { 
                    it.copy(deleteIntentSender = null, message = "删除已取消")
                }
            }
        }
    }
    
    fun clearIntentSender() {
        _internalState.update { it.copy(deleteIntentSender = null) }
    }
    
    fun clearMessage() {
        _internalState.update { it.copy(message = null) }
    }
}
