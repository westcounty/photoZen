package com.example.photozen.ui.screens.widgetpreview

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.repository.PhotoRepository
import com.example.photozen.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for widget photo preview.
 */
data class WidgetPhotoPreviewUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val initialIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val deleteIntentSender: IntentSender? = null,
    val pendingDeletePhotoId: String? = null,
    val message: String? = null
)

/**
 * One-time events for the UI.
 */
sealed class WidgetPhotoPreviewEvent {
    data class ShowToast(val message: String) : WidgetPhotoPreviewEvent()
}

/**
 * ViewModel for the widget photo preview screen.
 * Loads photos based on the widget's configured photo source.
 */
@HiltViewModel
class WidgetPhotoPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val photoRepository: PhotoRepository,
    private val preferencesRepository: PreferencesRepository,
    private val photoDao: PhotoDao
) : ViewModel() {

    private val photoId: String = savedStateHandle["photoId"] ?: ""
    private val widgetId: Int = savedStateHandle["widgetId"] ?: -1

    private val _uiState = MutableStateFlow(WidgetPhotoPreviewUiState())
    val uiState: StateFlow<WidgetPhotoPreviewUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WidgetPhotoPreviewEvent>()
    val events: SharedFlow<WidgetPhotoPreviewEvent> = _events.asSharedFlow()

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            try {
                // Get widget photo source configuration
                val photoSource = preferencesRepository.getWidgetPhotoSource(widgetId)
                val selectedAlbums = preferencesRepository.getWidgetSelectedAlbums(widgetId)
                val excludedAlbums = preferencesRepository.getWidgetExcludedAlbums(widgetId)

                // Load photos based on widget configuration
                val photos = photoRepository.getWidgetPhotos(
                    photoSource = photoSource,
                    selectedAlbums = selectedAlbums.toList(),
                    excludedAlbums = excludedAlbums.toList()
                )

                // Find the initial index for the clicked photo
                val initialIndex = photos.indexOfFirst { it.id == photoId }.coerceAtLeast(0)

                _uiState.value = WidgetPhotoPreviewUiState(
                    photos = photos,
                    initialIndex = initialIndex,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = WidgetPhotoPreviewUiState(
                    isLoading = false,
                    error = e.message ?: "加载照片失败"
                )
            }
        }
    }

    /**
     * Duplicate (create virtual copy of) a photo.
     */
    fun duplicatePhoto(photoId: String) {
        viewModelScope.launch {
            try {
                photoRepository.createVirtualCopy(photoId)
                _events.emit(WidgetPhotoPreviewEvent.ShowToast("已创建副本"))
            } catch (e: Exception) {
                _events.emit(WidgetPhotoPreviewEvent.ShowToast("创建副本失败"))
            }
        }
    }

    /**
     * Request permanent deletion via system API.
     * For Android 11+, this creates an IntentSender for user confirmation.
     */
    fun requestPermanentDelete(photoId: String) {
        val photo = uiState.value.photos.find { it.id == photoId } ?: return

        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+: Use MediaStore.createDeleteRequest
                    val uri = try {
                        Uri.parse(photo.systemUri)
                    } catch (e: Exception) {
                        null
                    }

                    if (uri != null) {
                        val intentSender = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            listOf(uri)
                        ).intentSender

                        _uiState.update {
                            it.copy(
                                deleteIntentSender = intentSender,
                                pendingDeletePhotoId = photoId
                            )
                        }
                    }
                } else {
                    // Older versions: Just remove from database
                    photoDao.deleteById(photoId)
                    // Remove from current list
                    _uiState.update { state ->
                        state.copy(
                            photos = state.photos.filter { it.id != photoId }
                        )
                    }
                    _events.emit(WidgetPhotoPreviewEvent.ShowToast("已永久删除"))
                }
            } catch (e: Exception) {
                _events.emit(WidgetPhotoPreviewEvent.ShowToast("删除失败: ${e.message}"))
            }
        }
    }

    /**
     * Called after system delete dialog completes.
     */
    fun onDeleteComplete(success: Boolean) {
        viewModelScope.launch {
            val pendingId = _uiState.value.pendingDeletePhotoId

            if (success && pendingId != null) {
                // Remove from database
                photoDao.deleteById(pendingId)
                // Remove from current list
                _uiState.update { state ->
                    state.copy(
                        photos = state.photos.filter { it.id != pendingId },
                        deleteIntentSender = null,
                        pendingDeletePhotoId = null
                    )
                }
                _events.emit(WidgetPhotoPreviewEvent.ShowToast("已永久删除"))
            } else {
                _uiState.update {
                    it.copy(
                        deleteIntentSender = null,
                        pendingDeletePhotoId = null
                    )
                }
            }
        }
    }

    /**
     * Clear delete intent sender (e.g., if dialog was dismissed).
     */
    fun clearDeleteIntent() {
        _uiState.update {
            it.copy(
                deleteIntentSender = null,
                pendingDeletePhotoId = null
            )
        }
    }

    /**
     * Clear message after showing.
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
