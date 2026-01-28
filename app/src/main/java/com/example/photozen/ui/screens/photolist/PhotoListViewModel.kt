package com.example.photozen.ui.screens.photolist

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import androidx.lifecycle.viewModelScope
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.domain.usecase.AlbumOperationsUseCase
import com.example.photozen.domain.usecase.GetPhotosUseCase
import com.example.photozen.domain.usecase.MovePhotoResult
import com.example.photozen.domain.usecase.SortPhotoUseCase
import com.example.photozen.data.repository.GuideRepository
import com.example.photozen.ui.components.PhotoGridMode
import com.example.photozen.ui.state.UiEvent
import com.example.photozen.ui.state.PhotoSelectionStateHolder
import com.example.photozen.domain.usecase.PhotoBatchOperationUseCase
import com.example.photozen.util.StoragePermissionHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 照片列表排序选项 (REQ-028, REQ-033, REQ-038)
 */
enum class PhotoListSortOrder(val displayName: String) {
    DATE_DESC("时间倒序"),      // 照片真实时间倒序 (默认)
    DATE_ASC("时间正序"),       // 照片真实时间正序
    ADDED_DESC("添加时间倒序"), // 添加到当前状态的时间倒序 (REQ-028, REQ-033)
    ADDED_ASC("添加时间正序"),  // 添加到当前状态的时间正序 (REQ-028, REQ-033)
    RANDOM("随机排序")          // 随机排序
}

/**
 * 各列表的排序选项配置 (REQ-028, REQ-033, REQ-038)
 */
object ListSortConfigs {
    // 待定列表: 照片真实时间正序/倒序、添加至待定列表时间正序/倒序
    val maybeList = listOf(
        PhotoListSortOrder.DATE_DESC,
        PhotoListSortOrder.DATE_ASC,
        PhotoListSortOrder.ADDED_DESC,
        PhotoListSortOrder.ADDED_ASC
    )

    // 回收站列表: 照片真实时间正序/倒序、添加至回收站时间正序/倒序
    val trashList = listOf(
        PhotoListSortOrder.DATE_DESC,
        PhotoListSortOrder.DATE_ASC,
        PhotoListSortOrder.ADDED_DESC,
        PhotoListSortOrder.ADDED_ASC
    )

    // 已保留列表: 照片真实时间正序/倒序、添加时间正序/倒序、随机排序
    val keepList = listOf(
        PhotoListSortOrder.DATE_DESC,
        PhotoListSortOrder.DATE_ASC,
        PhotoListSortOrder.ADDED_DESC,
        PhotoListSortOrder.ADDED_ASC,
        PhotoListSortOrder.RANDOM
    )

    // 相册/时间线列表: 照片真实时间正序/倒序、随机排序
    val albumList = listOf(
        PhotoListSortOrder.DATE_DESC,
        PhotoListSortOrder.DATE_ASC,
        PhotoListSortOrder.RANDOM
    )

    // 筛选列表: 照片真实时间正序/倒序、随机排序
    val filterList = listOf(
        PhotoListSortOrder.DATE_DESC,
        PhotoListSortOrder.DATE_ASC,
        PhotoListSortOrder.RANDOM
    )

    /** 根据状态获取排序选项列表 */
    fun forStatus(status: PhotoStatus): List<PhotoListSortOrder> = when (status) {
        PhotoStatus.MAYBE -> maybeList
        PhotoStatus.TRASH -> trashList
        PhotoStatus.KEEP -> keepList
        PhotoStatus.UNSORTED -> listOf(PhotoListSortOrder.DATE_DESC, PhotoListSortOrder.DATE_ASC)
    }
}

/**
 * 各列表的默认排序配置
 */
object DefaultSortOrders {
    val maybeList = PhotoListSortOrder.DATE_DESC      // 待定: 照片时间倒序
    val trashList = PhotoListSortOrder.ADDED_DESC     // 回收站: 添加时间倒序 (最近删除的在前)
    val keepList = PhotoListSortOrder.DATE_DESC       // 已保留: 照片时间倒序
    val albumList = PhotoListSortOrder.DATE_DESC      // 相册: 照片时间倒序

    /** 根据状态获取默认排序 */
    fun forStatus(status: PhotoStatus): PhotoListSortOrder = when (status) {
        PhotoStatus.MAYBE -> maybeList
        PhotoStatus.TRASH -> trashList
        PhotoStatus.KEEP -> keepList
        PhotoStatus.UNSORTED -> PhotoListSortOrder.DATE_DESC
    }
}

/** 待定列表选择数量限制 (REQ-029, REQ-030) */
const val MAYBE_LIST_SELECTION_LIMIT = 6

data class PhotoListUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val status: PhotoStatus = PhotoStatus.UNSORTED,
    val isLoading: Boolean = true,
    val message: String? = null,
    val defaultExternalApp: String? = null,
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC,
    val availableSortOptions: List<PhotoListSortOrder> = listOf(PhotoListSortOrder.DATE_DESC, PhotoListSortOrder.DATE_ASC), // REQ-028, REQ-033
    val isSelectionMode: Boolean = false,
    val selectedPhotoIds: Set<String> = emptySet(),
    val gridColumns: Int = 3,  // REQ-002, REQ-007: 默认3列
    val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
    // Album mode support
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val showAlbumDialog: Boolean = false,
    // Phase 6: Keep list album filter
    val showPhotosInAlbum: Boolean = true,  // When false, only show photos not in "my albums"
    val notInAlbumCount: Int = 0,           // Count of photos not in any "my album"
    val myAlbumBucketIds: Set<String> = emptySet(),  // Bucket IDs of "my albums"
    // Album classify mode
    val isClassifyMode: Boolean = false,
    val classifyModePhotos: List<PhotoEntity> = emptyList(),
    val classifyModeIndex: Int = 0,
    // REQ-029: 待定列表选择数量限制
    val selectionLimit: Int? = null,
    // Permanent delete support
    val deleteIntentSender: IntentSender? = null,
    val pendingDeletePhotoId: String? = null,
    // Permission dialog for move operations
    val showPermissionDialog: Boolean = false,
    val permissionRetryError: Boolean = false
) {
    val selectedCount: Int get() = selectedPhotoIds.size
    val allSelected: Boolean get() = photos.isNotEmpty() && selectedPhotoIds.size == photos.size
    // Batch management is available for KEEP, MAYBE, and TRASH
    val canBatchManage: Boolean get() = status in listOf(PhotoStatus.KEEP, PhotoStatus.MAYBE, PhotoStatus.TRASH)
    // Album operations for KEEP status
    val canBatchAlbum: Boolean get() = status == PhotoStatus.KEEP
    // Current photo in classify mode
    val currentClassifyPhoto: PhotoEntity? get() = classifyModePhotos.getOrNull(classifyModeIndex)
    // REQ-031: 对比按钮启用条件
    val canCompare: Boolean get() = status == PhotoStatus.MAYBE && selectedCount in 2..6
}

// Phase 4 清理：isSelectionMode 和 selectedPhotoIds 已迁移到 PhotoSelectionStateHolder
private data class InternalState(
    val isLoading: Boolean = true,
    val message: String? = null,
    val defaultExternalApp: String? = null,
    val sortOrder: PhotoListSortOrder = PhotoListSortOrder.DATE_DESC,
    val gridColumns: Int = 3,  // REQ-002, REQ-007: 默认3列
    val gridMode: PhotoGridMode = PhotoGridMode.WATERFALL,
    val showAlbumDialog: Boolean = false,
    // Phase 6: Keep list album filter
    val showPhotosInAlbum: Boolean = true,
    // Album classify mode
    val isClassifyMode: Boolean = false,
    val classifyModeIndex: Int = 0,
    val classifyModePhotosSnapshot: List<PhotoEntity> = emptyList(),  // 快照，进入分类模式时固定
    // Permanent delete support
    val deleteIntentSender: IntentSender? = null,
    val pendingDeletePhotoId: String? = null,
    val pendingDeletePhotoIds: List<String> = emptyList(),
    // Permission dialog for move operations
    val showPermissionDialog: Boolean = false,
    val permissionRetryError: Boolean = false,
    val pendingMoveAlbumBucketId: String? = null,
    val pendingMovePhotoIds: List<String> = emptyList(),
    // Permission dialog for classify mode
    val pendingClassifyAlbumBucketId: String? = null
)

private data class AlbumState(
    val albumBubbleList: List<AlbumBubbleEntity> = emptyList(),
    val albumAddAction: AlbumAddAction = AlbumAddAction.MOVE,
    val myAlbumBucketIds: Set<String> = emptySet()
)

@HiltViewModel
class PhotoListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val sortPhotoUseCase: SortPhotoUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val photoDao: PhotoDao,
    private val albumBubbleDao: AlbumBubbleDao,
    private val albumOperationsUseCase: AlbumOperationsUseCase,
    val guideRepository: GuideRepository,
    // Phase 4: 共享状态和批量操作
    private val selectionStateHolder: PhotoSelectionStateHolder,
    private val batchOperationUseCase: PhotoBatchOperationUseCase,
    private val storagePermissionHelper: StoragePermissionHelper
) : ViewModel() {
    
    // UI 事件流
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private val statusName: String = savedStateHandle.get<String>("statusName") ?: "UNSORTED"
    private val status: PhotoStatus = try {
        PhotoStatus.valueOf(statusName)
    } catch (e: Exception) {
        PhotoStatus.UNSORTED
    }
    
    private val _internalState = MutableStateFlow(InternalState())
    
    // Random seed for consistent random sorting
    // Use currentTimeMillis % Int.MAX_VALUE to prevent SQL integer overflow
    private var randomSeed = (System.currentTimeMillis() % Int.MAX_VALUE)
    private var randomSeedCounter = 0L
    
    // Album mode state
    private val _albumState = MutableStateFlow(AlbumState())
    
    // Phase 4: 使用 selectionStateHolder 的状态
    val uiState: StateFlow<PhotoListUiState> = combine(
        getPhotosUseCase.getPhotosByStatus(status),
        _internalState,
        _albumState,
        selectionStateHolder.selectedIds,
        selectionStateHolder.isSelectionMode
    ) { photos, internal, albumState, selectedIds, isSelectionMode ->
        // Apply sorting based on dateAdded (creation time)
        val sortedPhotos = applySortOrder(photos, internal.sortOrder)
        
        // Phase 6: Calculate photos not in "my albums" (only for KEEP status)
        val myAlbumBucketIds = albumState.myAlbumBucketIds
        val photosNotInAlbum = if (status == PhotoStatus.KEEP && myAlbumBucketIds.isNotEmpty()) {
            sortedPhotos.filter { photo -> photo.bucketId == null || photo.bucketId !in myAlbumBucketIds }
        } else {
            emptyList()
        }
        val notInAlbumCount = photosNotInAlbum.size
        
        // Apply album filter for KEEP status (Phase 6.2)
        val filteredPhotos = if (status == PhotoStatus.KEEP && !internal.showPhotosInAlbum && myAlbumBucketIds.isNotEmpty()) {
            photosNotInAlbum
        } else {
            sortedPhotos
        }
        
        // Filter out selected photos that no longer exist (Phase 4: 使用 StateHolder 的 selectedIds)
        val validSelectedIds = selectedIds.filter { id ->
            filteredPhotos.any { it.id == id }
        }.toSet()
        
        PhotoListUiState(
            photos = filteredPhotos,
            status = status,
            isLoading = internal.isLoading && photos.isEmpty(),
            message = internal.message,
            defaultExternalApp = internal.defaultExternalApp,
            sortOrder = internal.sortOrder,
            availableSortOptions = ListSortConfigs.forStatus(status), // REQ-028, REQ-033
            isSelectionMode = isSelectionMode || validSelectedIds.isNotEmpty(), // Phase 4: 使用 StateHolder
            selectedPhotoIds = validSelectedIds,
            gridColumns = internal.gridColumns,
            gridMode = internal.gridMode,
            albumBubbleList = albumState.albumBubbleList,
            albumAddAction = albumState.albumAddAction,
            showAlbumDialog = internal.showAlbumDialog,
            // Phase 6
            showPhotosInAlbum = internal.showPhotosInAlbum,
            notInAlbumCount = notInAlbumCount,
            myAlbumBucketIds = myAlbumBucketIds,
            isClassifyMode = internal.isClassifyMode,
            // 使用快照列表，避免分类过程中列表动态变化导致跳过照片
            classifyModePhotos = if (internal.isClassifyMode && internal.classifyModePhotosSnapshot.isNotEmpty()) {
                internal.classifyModePhotosSnapshot
            } else {
                photosNotInAlbum
            },
            classifyModeIndex = internal.classifyModeIndex,
            // REQ-029, REQ-030: 待定列表选择限制
            selectionLimit = if (status == PhotoStatus.MAYBE) MAYBE_LIST_SELECTION_LIMIT else null,
            // Permanent delete
            deleteIntentSender = internal.deleteIntentSender,
            pendingDeletePhotoId = internal.pendingDeletePhotoId,
            // Permission dialog
            showPermissionDialog = internal.showPermissionDialog,
            permissionRetryError = internal.permissionRetryError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PhotoListUiState(status = status)
    )
    
    init {
        // Phase 4: 进入页面时清空选择状态，避免与其他页面冲突
        selectionStateHolder.clear()

        viewModelScope.launch {
            _internalState.update { it.copy(isLoading = false) }
        }
        // Restore saved sort order from preferences (B4 fix)
        viewModelScope.launch {
            val screen = when (status) {
                PhotoStatus.KEEP -> PreferencesRepository.SortScreen.KEEP
                PhotoStatus.MAYBE -> PreferencesRepository.SortScreen.MAYBE
                PhotoStatus.TRASH -> PreferencesRepository.SortScreen.TRASH
                else -> PreferencesRepository.SortScreen.FILTER
            }
            val savedSortId = preferencesRepository.getSortOrderSync(screen)
            val availableOptions = ListSortConfigs.forStatus(status)
            val restoredOrder = when (savedSortId) {
                "photo_time_asc" -> PhotoListSortOrder.DATE_ASC
                "added_time_desc" -> PhotoListSortOrder.ADDED_DESC
                "added_time_asc" -> PhotoListSortOrder.ADDED_ASC
                "random" -> {
                    randomSeedCounter++
                    randomSeed = ((System.currentTimeMillis() xor randomSeedCounter) % Int.MAX_VALUE).coerceAtLeast(1)
                    PhotoListSortOrder.RANDOM
                }
                else -> PhotoListSortOrder.DATE_DESC // "photo_time_desc" or default
            }
            // Only apply if it's a valid option for this list
            if (restoredOrder in availableOptions) {
                _internalState.update { it.copy(sortOrder = restoredOrder) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.getDefaultExternalApp().collect { app ->
                _internalState.update { it.copy(defaultExternalApp = app) }
            }
        }
        // Load grid columns preference
        viewModelScope.launch {
            val gridScreen = when (status) {
                PhotoStatus.KEEP -> PreferencesRepository.GridScreen.KEEP
                PhotoStatus.MAYBE -> PreferencesRepository.GridScreen.MAYBE
                PhotoStatus.TRASH -> PreferencesRepository.GridScreen.TRASH
                PhotoStatus.UNSORTED -> PreferencesRepository.GridScreen.KEEP // Default
            }
            preferencesRepository.getGridColumns(gridScreen).collect { columns ->
                _internalState.update { it.copy(gridColumns = columns) }
            }
        }
        // Load album settings for KEEP status
        if (status == PhotoStatus.KEEP) {
            viewModelScope.launch {
                preferencesRepository.getAlbumAddAction().collect { action ->
                    _albumState.update { it.copy(albumAddAction = action) }
                }
            }
            viewModelScope.launch {
                albumBubbleDao.getAll().collect { albums ->
                    _albumState.update { 
                        it.copy(
                            albumBubbleList = albums,
                            myAlbumBucketIds = albums.map { album -> album.bucketId }.toSet()
                        )
                    }
                }
            }
            // Phase 6.2: Load show photos in album preference
            viewModelScope.launch {
                preferencesRepository.getShowPhotosInAlbumKeepList().collect { show ->
                    _internalState.update { it.copy(showPhotosInAlbum = show) }
                }
            }
        }
    }
    
    /**
     * Apply sort order to photos list.
     * - DATE_*: Uses effective time (dateTaken if available, else dateAdded * 1000)
     * - ADDED_*: Uses updatedAt (when status was changed / added to this list)
     */
    private fun applySortOrder(photos: List<PhotoEntity>, sortOrder: PhotoListSortOrder): List<PhotoEntity> {
        return when (sortOrder) {
            // Sort by effective time: prefers dateTaken, falls back to dateAdded * 1000
            PhotoListSortOrder.DATE_DESC -> photos.sortedByDescending { it.getEffectiveTime() }
            PhotoListSortOrder.DATE_ASC -> photos.sortedBy { it.getEffectiveTime() }
            // Sort by updatedAt (when added to current status/list) - REQ-028, REQ-033
            PhotoListSortOrder.ADDED_DESC -> photos.sortedByDescending { it.updatedAt }
            PhotoListSortOrder.ADDED_ASC -> photos.sortedBy { it.updatedAt }
            PhotoListSortOrder.RANDOM -> photos.shuffled(kotlin.random.Random(randomSeed))
        }
    }

    /**
     * Get effective time for sorting: prefers dateTaken, falls back to dateAdded * 1000
     * Matches the database COALESCE logic in PhotoDao
     */
    private fun PhotoEntity.getEffectiveTime(): Long {
        return if (dateTaken > 0) dateTaken else dateAdded * 1000
    }
    
    /**
     * Set sort order.
     */
    fun setSortOrder(order: PhotoListSortOrder) {
        if (order == PhotoListSortOrder.RANDOM) {
            randomSeedCounter++
            randomSeed = ((System.currentTimeMillis() xor randomSeedCounter) % Int.MAX_VALUE).coerceAtLeast(1)
        }
        _internalState.update { it.copy(sortOrder = order) }

        // Persist sort order to preferences (B4 fix)
        viewModelScope.launch {
            val sortId = when (order) {
                PhotoListSortOrder.DATE_DESC -> "photo_time_desc"
                PhotoListSortOrder.DATE_ASC -> "photo_time_asc"
                PhotoListSortOrder.ADDED_DESC -> "added_time_desc"
                PhotoListSortOrder.ADDED_ASC -> "added_time_asc"
                PhotoListSortOrder.RANDOM -> "random"
            }
            val screen = when (status) {
                PhotoStatus.KEEP -> PreferencesRepository.SortScreen.KEEP
                PhotoStatus.MAYBE -> PreferencesRepository.SortScreen.MAYBE
                PhotoStatus.TRASH -> PreferencesRepository.SortScreen.TRASH
                else -> PreferencesRepository.SortScreen.FILTER
            }
            preferencesRepository.setSortOrder(screen, sortId)
        }
    }
    
    /**
     * Cycle through sort orders.
     * Note: This cycles through available options based on current status.
     */
    fun cycleSortOrder() {
        val availableOptions = ListSortConfigs.forStatus(status)
        val currentOrder = _internalState.value.sortOrder
        val currentIndex = availableOptions.indexOf(currentOrder)
        val nextIndex = if (currentIndex >= 0) {
            (currentIndex + 1) % availableOptions.size
        } else {
            0
        }
        val nextOrder = availableOptions.getOrElse(nextIndex) { PhotoListSortOrder.DATE_DESC }
        setSortOrder(nextOrder)
    }
    
    /**
     * Set default external app for opening photos.
     */
    suspend fun setDefaultExternalApp(packageName: String?) {
        preferencesRepository.setDefaultExternalApp(packageName)
    }
    
    fun moveToKeep(photoId: String) {
        viewModelScope.launch {
            try {
                sortPhotoUseCase.keepPhoto(photoId)
                _internalState.update { it.copy(message = "已移至保留") }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    fun moveToTrash(photoId: String) {
        viewModelScope.launch {
            try {
                sortPhotoUseCase.trashPhoto(photoId)
                _internalState.update { it.copy(message = "已移至回收站") }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
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

                        _internalState.update {
                            it.copy(
                                deleteIntentSender = intentSender,
                                pendingDeletePhotoId = photoId
                            )
                        }
                    }
                } else {
                    // Older versions: Just remove from database
                    photoDao.deleteById(photoId)
                    _internalState.update { it.copy(message = "已删除") }
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "删除失败: ${e.message}") }
            }
        }
    }

    /**
     * Called after system delete dialog completes.
     */
    fun onDeleteComplete(success: Boolean) {
        viewModelScope.launch {
            val pendingId = _internalState.value.pendingDeletePhotoId
            val pendingIds = _internalState.value.pendingDeletePhotoIds

            if (success) {
                // Handle single photo deletion
                if (pendingId != null) {
                    photoDao.deleteById(pendingId)
                    _internalState.update {
                        it.copy(
                            deleteIntentSender = null,
                            pendingDeletePhotoId = null,
                            pendingDeletePhotoIds = emptyList(),
                            message = "已永久删除"
                        )
                    }
                }
                // Handle multiple photo deletion
                else if (pendingIds.isNotEmpty()) {
                    pendingIds.forEach { photoDao.deleteById(it) }
                    selectionStateHolder.clear()
                    _internalState.update {
                        it.copy(
                            deleteIntentSender = null,
                            pendingDeletePhotoId = null,
                            pendingDeletePhotoIds = emptyList(),
                            message = "已永久删除 ${pendingIds.size} 张照片"
                        )
                    }
                }
            } else {
                _internalState.update {
                    it.copy(
                        deleteIntentSender = null,
                        pendingDeletePhotoId = null,
                        pendingDeletePhotoIds = emptyList()
                    )
                }
            }
        }
    }

    /**
     * Clear delete intent sender (e.g., if dialog was dismissed).
     */
    fun clearDeleteIntent() {
        _internalState.update {
            it.copy(
                deleteIntentSender = null,
                pendingDeletePhotoId = null,
                pendingDeletePhotoIds = emptyList()
            )
        }
    }

    /**
     * Request permanent deletion of selected photos via system API.
     * For Android 11+, this creates an IntentSender for user confirmation.
     */
    fun requestPermanentDeleteSelected() {
        val selectedIds = uiState.value.selectedPhotoIds.toList()
        if (selectedIds.isEmpty()) return

        val selectedPhotos = uiState.value.photos.filter { it.id in selectedIds }
        if (selectedPhotos.isEmpty()) return

        viewModelScope.launch {
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
                            it.copy(
                                deleteIntentSender = intentSender,
                                pendingDeletePhotoIds = selectedIds
                            )
                        }
                    }
                } else {
                    // Older versions: Just remove from database
                    selectedIds.forEach { photoDao.deleteById(it) }
                    selectionStateHolder.clear()
                    _internalState.update { it.copy(message = "已删除 ${selectedIds.size} 张照片") }
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "删除失败: ${e.message}") }
            }
        }
    }

    fun moveToMaybe(photoId: String) {
        viewModelScope.launch {
            try {
                sortPhotoUseCase.maybePhoto(photoId)
                _internalState.update { it.copy(message = "已标记为待定") }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    fun resetToUnsorted(photoId: String) {
        viewModelScope.launch {
            try {
                sortPhotoUseCase.resetPhoto(photoId)
                _internalState.update { it.copy(message = "已恢复未整理") }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败") }
            }
        }
    }
    
    fun clearMessage() {
        _internalState.update { it.copy(message = null) }
    }
    
    // ==================== Selection Mode (Phase 4: 使用 StateHolder) ====================
    
    /**
     * Toggle selection mode on/off.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun toggleSelectionMode() {
        if (selectionStateHolder.hasSelection()) {
            selectionStateHolder.clear()
        }
    }
    
    /**
     * Exit selection mode and clear selection.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun exitSelectionMode() {
        selectionStateHolder.clear()
    }
    
    /**
     * Toggle selection of a single photo.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun togglePhotoSelection(photoId: String) {
        selectionStateHolder.toggle(photoId)
    }
    
    /**
     * Update selection with a new set of IDs (for drag-select).
     * Phase 4: 委托给 PhotoSelectionStateHolder
     * REQ-029, REQ-030: 待定列表限制最多6张
     *
     * @return true if selection was applied as-is, false if it was limited
     */
    fun updateSelection(newSelection: Set<String>): Boolean {
        val limit = if (status == PhotoStatus.MAYBE) MAYBE_LIST_SELECTION_LIMIT else null
        if (limit != null && newSelection.size > limit) {
            // 只保留前 limit 张 (按当前照片列表顺序)
            val currentPhotos = uiState.value.photos
            val ordered = newSelection.filter { id -> currentPhotos.any { it.id == id } }
                .take(limit)
                .toSet()
            selectionStateHolder.setSelection(ordered)
            return false // 表示被限制了
        } else {
            selectionStateHolder.setSelection(newSelection)
            return true
        }
    }

    /**
     * 切换单张照片选中状态 (REQ-029)
     * 待定列表限制最多6张
     *
     * @return true if toggled successfully, false if hit limit
     */
    fun togglePhotoSelectionWithLimit(photoId: String): Boolean {
        val currentSelection = selectionStateHolder.getSelectedSet()
        val limit = if (status == PhotoStatus.MAYBE) MAYBE_LIST_SELECTION_LIMIT else null

        if (photoId in currentSelection) {
            // 取消选中始终允许
            selectionStateHolder.toggle(photoId)
            return true
        } else {
            // 选中 - 检查限制
            if (limit != null && currentSelection.size >= limit) {
                return false // 达到上限
            }
            selectionStateHolder.toggle(photoId)
            return true
        }
    }
    
    /**
     * Select all photos.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun selectAll() {
        val allIds = uiState.value.photos.map { it.id }
        selectionStateHolder.selectAll(allIds)
    }
    
    /**
     * Deselect all photos.
     * Phase 4: 委托给 PhotoSelectionStateHolder
     */
    fun deselectAll() {
        selectionStateHolder.clear()
    }
    
    /**
     * 批量更新选中照片状态（Phase 4: 使用 BatchUseCase）
     * 
     * 新版本使用 PhotoBatchOperationUseCase 统一处理：
     * - 状态批量更新
     * - 撤销支持（通过 UndoManager）
     * - Snackbar 反馈（通过 SnackbarManager）
     * - 统计记录
     */
    private fun batchUpdateStatus(newStatus: PhotoStatus) {
        val selectedIds = selectionStateHolder.getSelectedList()
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            // Phase 4: 使用 BatchUseCase 执行批量操作
            // UseCase 内部已处理撤销、Snackbar、统计
            batchOperationUseCase.batchUpdateStatus(selectedIds, newStatus)
            
            // 清空选择
            selectionStateHolder.clear()
        }
    }
    
    /**
     * Move selected photos to Keep.
     * Phase 4: 使用 BatchUseCase
     */
    fun moveSelectedToKeep() = batchUpdateStatus(PhotoStatus.KEEP)
    
    /**
     * Move selected photos to Trash.
     * Phase 4: 使用 BatchUseCase
     */
    fun moveSelectedToTrash() = batchUpdateStatus(PhotoStatus.TRASH)
    
    /**
     * Move selected photos to Maybe.
     * Phase 4: 使用 BatchUseCase
     */
    fun moveSelectedToMaybe() = batchUpdateStatus(PhotoStatus.MAYBE)
    
    /**
     * Reset selected photos to Unsorted.
     * Phase 4: 使用 BatchUseCase
     */
    fun resetSelectedToUnsorted() = batchUpdateStatus(PhotoStatus.UNSORTED)
    
    /**
     * Duplicate a photo, preserving all EXIF metadata and timestamps.
     * The copy will have the same status as the original photo.
     */
    fun duplicatePhoto(photoId: String) {
        viewModelScope.launch {
            try {
                // Get the original photo
                val originalPhoto = photoDao.getById(photoId)
                if (originalPhoto == null) {
                    _internalState.update { it.copy(message = "找不到照片") }
                    return@launch
                }
                
                // Duplicate the photo in MediaStore
                val sourceUri = Uri.parse(originalPhoto.systemUri)
                val newPhoto = mediaStoreDataSource.duplicatePhoto(sourceUri)
                
                if (newPhoto != null) {
                    // Save the new photo to our database with the same status
                    val photoWithStatus = newPhoto.copy(status = originalPhoto.status)
                    photoDao.insert(photoWithStatus)
                    _internalState.update { it.copy(message = "照片已复制") }
                } else {
                    _internalState.update { it.copy(message = "复制照片失败") }
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "复制照片失败: ${e.message}") }
            }
        }
    }
    
    // ==================== Album Operations ====================
    
    /**
     * Show album selection dialog.
     */
    fun showAlbumDialog() {
        _internalState.update { it.copy(showAlbumDialog = true) }
    }
    
    /**
     * Hide album selection dialog.
     */
    fun hideAlbumDialog() {
        _internalState.update { it.copy(showAlbumDialog = false) }
    }
    
    /**
     * Refresh albums list. Since albums are loaded via Flow from DAO,
     * this is mostly a no-op as the Flow will automatically provide updates.
     * Kept for explicit refresh triggers from UI.
     */
    fun refreshAlbums() {
        // Albums are automatically refreshed via Flow from albumBubbleDao.getAll()
        // This method exists for explicit triggers but the data should already be current
    }
    
    /**
     * Add selected photos to album using default action (copy or move).
     * Phase 4: 使用 selectionStateHolder
     */
    fun addSelectedToAlbum(bucketId: String) {
        val selectedIds = selectionStateHolder.getSelectedList()
        if (selectedIds.isEmpty()) return
        
        val action = _albumState.value.albumAddAction
        if (action == AlbumAddAction.MOVE) {
            moveSelectedToAlbum(bucketId)
        } else {
            copySelectedToAlbum(bucketId)
        }
    }
    
    /**
     * Move selected photos to an album.
     * Phase 4: 使用 selectionStateHolder
     */
    fun moveSelectedToAlbum(bucketId: String) {
        val selectedIds = selectionStateHolder.getSelectedList()
        if (selectedIds.isEmpty()) return

        // Check permission for move operation
        if (storagePermissionHelper.isManageStoragePermissionApplicable() &&
            !storagePermissionHelper.hasManageStoragePermission()) {
            // Save pending operation and show permission dialog
            _internalState.update {
                it.copy(
                    pendingMoveAlbumBucketId = bucketId,
                    pendingMovePhotoIds = selectedIds,
                    permissionRetryError = false,
                    showPermissionDialog = true
                )
            }
            return
        }

        executeMoveSelectedToAlbum(bucketId, selectedIds)
    }

    /**
     * Execute the move operation after permission check.
     */
    private fun executeMoveSelectedToAlbum(bucketId: String, photoIds: List<String>) {
        viewModelScope.launch {
            try {
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                var successCount = 0

                for (photoId in photoIds) {
                    val photo = uiState.value.photos.find { it.id == photoId } ?: continue
                    val photoUri = Uri.parse(photo.systemUri)

                    when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                        is MovePhotoResult.Success -> successCount++
                        is MovePhotoResult.NeedsConfirmation -> {
                            // Handle one at a time for confirmation
                        }
                        is MovePhotoResult.Error -> {
                            // Continue with other photos
                        }
                    }
                }

                _internalState.update {
                    it.copy(
                        showAlbumDialog = false,
                        message = "已移动 $successCount 张照片到「${album?.displayName}」"
                    )
                }
                selectionStateHolder.clear()
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "移动失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Copy selected photos to an album.
     * Phase 4: 使用 selectionStateHolder
     */
    fun copySelectedToAlbum(bucketId: String) {
        val selectedIds = selectionStateHolder.getSelectedList()
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                var successCount = 0
                
                for (photoId in selectedIds) {
                    val photo = uiState.value.photos.find { it.id == photoId } ?: continue
                    val photoUri = Uri.parse(photo.systemUri)
                    
                    val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                    if (result.isSuccess) {
                        successCount++
                    }
                }
                
                _internalState.update { 
                    it.copy(
                        showAlbumDialog = false,
                        message = "已复制 $successCount 张照片到「${album?.displayName}」"
                    )
                }
                selectionStateHolder.clear()
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "复制失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Add a single photo to album (for long-press menu).
     */
    fun addPhotoToAlbum(photoId: String, bucketId: String, copy: Boolean = false) {
        viewModelScope.launch {
            try {
                val photo = uiState.value.photos.find { it.id == photoId } ?: return@launch
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                // Use getAlbumPath to get the actual album path (e.g., "DCIM/Camera" for system Camera album)
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                val photoUri = Uri.parse(photo.systemUri)
                
                if (copy) {
                    val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                    if (result.isSuccess) {
                        _internalState.update { it.copy(message = "已复制到「${album?.displayName}」") }
                    } else {
                        _internalState.update { it.copy(message = "复制失败") }
                    }
                } else {
                    when (val result = albumOperationsUseCase.movePhotoToAlbum(photoUri, targetPath)) {
                        is MovePhotoResult.Success -> {
                            _internalState.update { it.copy(message = "已移动到「${album?.displayName}」") }
                        }
                        is MovePhotoResult.NeedsConfirmation -> {
                            _internalState.update { it.copy(message = "需要权限确认才能移动") }
                        }
                        is MovePhotoResult.Error -> {
                            _internalState.update { it.copy(message = "移动失败: ${result.message}") }
                        }
                    }
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败: ${e.message}") }
            }
        }
    }
    
    // ==================== Grid Columns (REQ-002, REQ-003, REQ-007, REQ-008) ====================

    /**
     * Cycle grid columns based on current mode.
     * REQ-002: 网格视图 2-5 列
     * REQ-007: 瀑布流视图 1-5 列
     */
    fun cycleGridColumns() {
        viewModelScope.launch {
            val gridScreen = when (status) {
                PhotoStatus.KEEP -> PreferencesRepository.GridScreen.KEEP
                PhotoStatus.MAYBE -> PreferencesRepository.GridScreen.MAYBE
                PhotoStatus.TRASH -> PreferencesRepository.GridScreen.TRASH
                PhotoStatus.UNSORTED -> PreferencesRepository.GridScreen.KEEP
            }
            val minColumns = if (_internalState.value.gridMode == PhotoGridMode.SQUARE) 2 else 1
            val newColumns = preferencesRepository.cycleGridColumns(gridScreen, minColumns)
            _internalState.update { it.copy(gridColumns = newColumns) }
        }
    }

    /**
     * Set grid columns directly (for pinch-zoom gesture).
     * REQ-003, REQ-008: 双指缩放切换列数
     *
     * @param columns Target column count
     */
    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            val gridScreen = when (status) {
                PhotoStatus.KEEP -> PreferencesRepository.GridScreen.KEEP
                PhotoStatus.MAYBE -> PreferencesRepository.GridScreen.MAYBE
                PhotoStatus.TRASH -> PreferencesRepository.GridScreen.TRASH
                PhotoStatus.UNSORTED -> PreferencesRepository.GridScreen.KEEP
            }
            // Apply limits based on current grid mode
            val minColumns = if (_internalState.value.gridMode == PhotoGridMode.SQUARE) 2 else 1
            val validColumns = columns.coerceIn(minColumns, 5)
            preferencesRepository.setGridColumns(gridScreen, validColumns)
            _internalState.update { it.copy(gridColumns = validColumns) }
        }
    }

    /**
     * Toggle between SQUARE (grid) and WATERFALL (staggered) modes.
     * SQUARE mode supports drag-to-select, WATERFALL mode does not.
     * REQ-027: 视图模式切换按钮
     */
    fun toggleGridMode() {
        _internalState.update { state ->
            val newMode = when (state.gridMode) {
                PhotoGridMode.SQUARE -> PhotoGridMode.WATERFALL
                PhotoGridMode.WATERFALL -> PhotoGridMode.SQUARE
            }
            // Adjust columns if switching to SQUARE mode and current columns < 2
            val newColumns = if (newMode == PhotoGridMode.SQUARE && state.gridColumns < 2) {
                2
            } else {
                state.gridColumns
            }
            state.copy(gridMode = newMode, gridColumns = newColumns)
        }
    }

    /**
     * Set grid mode directly.
     * @param mode The new grid mode
     */
    fun setGridMode(mode: PhotoGridMode) {
        _internalState.update { state ->
            // Adjust columns if switching to SQUARE mode and current columns < 2
            val newColumns = if (mode == PhotoGridMode.SQUARE && state.gridColumns < 2) {
                2
            } else {
                state.gridColumns
            }
            state.copy(gridMode = mode, gridColumns = newColumns)
        }
    }

    // ==================== Phase 6: Keep List Album Features ====================
    
    /**
     * Toggle whether to show photos that are already in albums.
     */
    fun toggleShowPhotosInAlbum() {
        viewModelScope.launch {
            val newValue = !_internalState.value.showPhotosInAlbum
            preferencesRepository.setShowPhotosInAlbumKeepList(newValue)
            _internalState.update { it.copy(showPhotosInAlbum = newValue) }
        }
    }
    
    /**
     * Enter album classify mode - for quickly adding photos to albums.
     * Only available for KEEP status photos not in any "my album".
     */
    fun enterClassifyMode() {
        if (status != PhotoStatus.KEEP) return
        // 保存当前待分类照片的快照，避免分类过程中列表动态变化
        val currentPhotosNotInAlbum = uiState.value.classifyModePhotos
        _internalState.update {
            it.copy(
                isClassifyMode = true,
                classifyModeIndex = 0,
                classifyModePhotosSnapshot = currentPhotosNotInAlbum
            )
        }
    }
    
    /**
     * Exit album classify mode.
     */
    fun exitClassifyMode() {
        _internalState.update {
            it.copy(
                isClassifyMode = false,
                classifyModeIndex = 0,
                classifyModePhotosSnapshot = emptyList()  // 清除快照
            )
        }
    }
    
    /**
     * Add current photo in classify mode to an album, then advance to next.
     */
    fun classifyPhotoToAlbum(bucketId: String) {
        val currentPhoto = uiState.value.currentClassifyPhoto ?: return

        // Check permission for copy/move operation
        if (storagePermissionHelper.isManageStoragePermissionApplicable() &&
            !storagePermissionHelper.hasManageStoragePermission()) {
            // Save pending operation and show permission dialog
            _internalState.update {
                it.copy(
                    pendingClassifyAlbumBucketId = bucketId,
                    permissionRetryError = false,
                    showPermissionDialog = true
                )
            }
            return
        }

        executeClassifyPhotoToAlbum(bucketId, currentPhoto)
    }

    /**
     * Execute the actual classify photo operation (after permission check).
     */
    private fun executeClassifyPhotoToAlbum(bucketId: String, currentPhoto: PhotoEntity) {
        viewModelScope.launch {
            try {
                val album = _albumState.value.albumBubbleList.find { it.bucketId == bucketId }
                val targetPath = mediaStoreDataSource.getAlbumPath(bucketId)
                    ?: "Pictures/${album?.displayName ?: "PhotoZen"}"
                val photoUri = Uri.parse(currentPhoto.systemUri)

                // Copy photo to album (don't move, keep original location)
                val result = albumOperationsUseCase.copyPhotoToAlbum(photoUri, targetPath)
                if (result.isSuccess) {
                    // Update photo's bucketId in database
                    photoDao.updateBucketId(currentPhoto.id, bucketId)
                    _internalState.update { it.copy(message = "已添加到「${album?.displayName}」") }
                }

                // Advance to next photo
                advanceClassifyMode()
            } catch (e: Exception) {
                _internalState.update { it.copy(message = "操作失败: ${e.message}") }
            }
        }
    }
    
    /**
     * Skip current photo in classify mode (don't add to any album).
     */
    fun skipClassifyPhoto() {
        advanceClassifyMode()
    }
    
    /**
     * Advance to next photo in classify mode.
     */
    private fun advanceClassifyMode() {
        val currentIndex = _internalState.value.classifyModeIndex
        val totalCount = uiState.value.classifyModePhotos.size

        if (currentIndex + 1 >= totalCount) {
            // All photos processed, exit classify mode
            _internalState.update {
                it.copy(
                    isClassifyMode = false,
                    classifyModeIndex = 0,
                    classifyModePhotosSnapshot = emptyList(),  // 清除快照
                    message = "全部照片已分类完成"
                )
            }
        } else {
            // Move to next photo
            _internalState.update { it.copy(classifyModeIndex = currentIndex + 1) }
        }
    }
    
    // ==================== Permission Dialog ====================

    /**
     * Called when user returns from settings after granting permission.
     */
    fun onPermissionGranted() {
        if (storagePermissionHelper.hasManageStoragePermission()) {
            // Permission granted, execute pending operation
            val moveBucketId = _internalState.value.pendingMoveAlbumBucketId
            val movePhotoIds = _internalState.value.pendingMovePhotoIds
            val classifyBucketId = _internalState.value.pendingClassifyAlbumBucketId
            val currentClassifyPhoto = uiState.value.currentClassifyPhoto

            // Close dialog and clear pending state
            _internalState.update {
                it.copy(
                    showPermissionDialog = false,
                    permissionRetryError = false,
                    pendingMoveAlbumBucketId = null,
                    pendingMovePhotoIds = emptyList(),
                    pendingClassifyAlbumBucketId = null
                )
            }

            // Execute the pending move operation
            if (moveBucketId != null && movePhotoIds.isNotEmpty()) {
                executeMoveSelectedToAlbum(moveBucketId, movePhotoIds)
            }

            // Execute the pending classify operation
            if (classifyBucketId != null && currentClassifyPhoto != null) {
                executeClassifyPhotoToAlbum(classifyBucketId, currentClassifyPhoto)
            }
        } else {
            // Permission still not granted, show error
            _internalState.update { it.copy(permissionRetryError = true) }
        }
    }

    /**
     * Dismiss permission dialog without granting permission.
     */
    fun dismissPermissionDialog() {
        _internalState.update {
            it.copy(
                showPermissionDialog = false,
                permissionRetryError = false,
                pendingMoveAlbumBucketId = null,
                pendingMovePhotoIds = emptyList(),
                pendingClassifyAlbumBucketId = null
            )
        }
    }

    /**
     * Phase 4: 页面销毁时清理选择状态
     */
    override fun onCleared() {
        super.onCleared()
        selectionStateHolder.clear()
    }
}
