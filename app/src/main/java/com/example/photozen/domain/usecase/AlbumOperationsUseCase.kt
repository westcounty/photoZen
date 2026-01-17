package com.example.photozen.domain.usecase

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.photozen.data.local.dao.AlbumBubbleDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.AlbumBubbleEntity
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.AlbumAddAction
import com.example.photozen.data.repository.PreferencesRepository
import com.example.photozen.data.source.Album
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.util.StoragePermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a photo move operation with dual-layer permission handling.
 */
sealed class MovePhotoResult {
    /** Move succeeded without user confirmation */
    data class Success(val newPhoto: PhotoEntity) : MovePhotoResult()
    
    /** Move requires user confirmation (MediaStore createWriteRequest fallback) */
    data class NeedsConfirmation(
        val intentSender: IntentSender,
        val photoUri: Uri,
        val targetAlbumPath: String
    ) : MovePhotoResult()
    
    /** Move failed */
    data class Error(val message: String) : MovePhotoResult()
}

/**
 * Statistics for an album.
 */
data class AlbumStats(
    val totalCount: Int,
    val sortedCount: Int,  // Photos with status != UNSORTED
    val unsortedCount: Int
) {
    val sortedPercentage: Float
        get() = if (totalCount > 0) sortedCount.toFloat() / totalCount else 0f
}

/**
 * Use case for album-related operations.
 * 
 * Implements dual-layer permission strategy:
 * 1. Primary: MANAGE_EXTERNAL_STORAGE for seamless operations
 * 2. Fallback: MediaStore createWriteRequest for per-file confirmation
 * 
 * This ensures photo operations never fail - they either work seamlessly
 * or prompt the user for confirmation.
 */
@Singleton
class AlbumOperationsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val albumBubbleDao: AlbumBubbleDao,
    private val photoDao: PhotoDao,
    private val preferencesRepository: PreferencesRepository,
    private val storagePermissionHelper: StoragePermissionHelper
) {
    
    // ==================== ALBUM BUBBLE LIST OPERATIONS ====================
    
    /**
     * Get all albums in the user's bubble list.
     */
    fun getAlbumBubbleList(): Flow<List<AlbumBubbleEntity>> {
        return albumBubbleDao.getAll()
    }
    
    /**
     * Add an album to the bubble list.
     */
    suspend fun addAlbumToBubbleList(album: Album) {
        val maxSortOrder = albumBubbleDao.getMaxSortOrder() ?: -1
        val entity = AlbumBubbleEntity(
            bucketId = album.id,
            displayName = album.name,
            sortOrder = maxSortOrder + 1
        )
        albumBubbleDao.insert(entity)
    }
    
    /**
     * Add multiple albums to the bubble list.
     */
    suspend fun addAlbumsToBubbleList(albums: List<Album>) {
        val maxSortOrder = albumBubbleDao.getMaxSortOrder() ?: -1
        val entities = albums.mapIndexed { index, album ->
            AlbumBubbleEntity(
                bucketId = album.id,
                displayName = album.name,
                sortOrder = maxSortOrder + 1 + index
            )
        }
        albumBubbleDao.insertAll(entities)
    }
    
    /**
     * Remove an album from the bubble list.
     */
    suspend fun removeAlbumFromBubbleList(bucketId: String) {
        albumBubbleDao.deleteByBucketId(bucketId)
    }
    
    /**
     * Update sort order for an album.
     */
    suspend fun updateAlbumSortOrder(bucketId: String, sortOrder: Int) {
        albumBubbleDao.updateSortOrder(bucketId, sortOrder)
    }
    
    /**
     * Check if an album is in the bubble list.
     */
    suspend fun isAlbumInBubbleList(bucketId: String): Boolean {
        return albumBubbleDao.exists(bucketId)
    }
    
    // ==================== ALBUM OPERATIONS ====================
    
    /**
     * Get all system albums.
     */
    suspend fun getAllAlbums(): List<Album> {
        return mediaStoreDataSource.getAllAlbums()
    }
    
    /**
     * Create a new album.
     * 
     * @param name Name of the new album
     * @return Pair of (bucketId, albumPath), or null if failed
     */
    suspend fun createAlbum(name: String): Pair<String, String>? {
        return mediaStoreDataSource.createAlbum(name)
    }
    
    /**
     * Get statistics for an album.
     */
    suspend fun getAlbumStats(bucketId: String): AlbumStats = withContext(Dispatchers.IO) {
        // Get photos from our database that belong to this album
        val photos = photoDao.getPhotosByBucketIdSync(bucketId)
        val totalCount = photos.size
        val sortedCount = photos.count { it.status != PhotoStatus.UNSORTED }
        
        AlbumStats(
            totalCount = totalCount,
            sortedCount = sortedCount,
            unsortedCount = totalCount - sortedCount
        )
    }
    
    // ==================== PHOTO OPERATIONS ====================
    
    /**
     * Move a photo to an album.
     * 
     * Uses dual-layer permission strategy:
     * 1. If MANAGE_EXTERNAL_STORAGE is granted, moves directly
     * 2. If not, returns NeedsConfirmation with IntentSender for user to confirm
     * 
     * @param photoUri URI of the photo to move
     * @param targetAlbumPath Target album path (e.g., "Pictures/MyAlbum")
     * @return MovePhotoResult indicating success, need for confirmation, or error
     */
    suspend fun movePhotoToAlbum(
        photoUri: Uri,
        targetAlbumPath: String
    ): MovePhotoResult = withContext(Dispatchers.IO) {
        try {
            if (storagePermissionHelper.hasManageStoragePermission()) {
                // Has full permission - direct move
                val result = mediaStoreDataSource.movePhotoToAlbum(photoUri, targetAlbumPath)
                if (result != null) {
                    MovePhotoResult.Success(result)
                } else {
                    MovePhotoResult.Error("移动照片失败")
                }
            } else {
                // Need per-file permission - use createWriteRequest fallback
                val moveResult = mediaStoreDataSource.movePhotoToAlbumWithConfirmation(
                    photoUri, targetAlbumPath
                )
                
                when (moveResult) {
                    is MediaStoreDataSource.MoveResult.Success -> {
                        MovePhotoResult.Success(moveResult.newPhoto)
                    }
                    is MediaStoreDataSource.MoveResult.RequiresDeleteConfirmation -> {
                        // Return the intent sender for UI to handle
                        MovePhotoResult.NeedsConfirmation(
                            intentSender = moveResult.intentSender,
                            photoUri = photoUri,
                            targetAlbumPath = targetAlbumPath
                        )
                    }
                    is MediaStoreDataSource.MoveResult.Failed -> {
                        MovePhotoResult.Error("移动照片失败")
                    }
                }
            }
        } catch (e: Exception) {
            MovePhotoResult.Error("移动照片失败: ${e.message}")
        }
    }
    
    /**
     * Execute pending move after user grants permission.
     * Called after user confirms via the IntentSender.
     */
    suspend fun executePendingMove(
        photoUri: Uri,
        targetAlbumPath: String
    ): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val result = mediaStoreDataSource.movePhotoAfterPermission(photoUri, targetAlbumPath)
                if (result != null) {
                    Result.success(result)
                } else {
                    Result.failure(Exception("移动照片失败"))
                }
            } else {
                Result.failure(Exception("不支持的系统版本"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Copy a photo to an album.
     * 
     * @param photoUri URI of the photo to copy
     * @param targetAlbumPath Target album path (e.g., "Pictures/MyAlbum")
     * @return The new PhotoEntity if successful, null otherwise
     */
    suspend fun copyPhotoToAlbum(
        photoUri: Uri,
        targetAlbumPath: String
    ): Result<PhotoEntity> = withContext(Dispatchers.IO) {
        try {
            val result = mediaStoreDataSource.copyPhotoToAlbum(photoUri, targetAlbumPath)
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("复制照片失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add photo to album using the user's preferred method (copy or move).
     * 
     * @param photoUri URI of the photo
     * @param targetAlbumPath Target album path
     * @return Result based on the operation type and permission status
     */
    suspend fun addPhotoToAlbumWithPreference(
        photoUri: Uri,
        targetAlbumPath: String
    ): MovePhotoResult = withContext(Dispatchers.IO) {
        val action = preferencesRepository.getAlbumAddAction().first()
        
        when (action) {
            AlbumAddAction.COPY -> {
                val result = copyPhotoToAlbum(photoUri, targetAlbumPath)
                result.fold(
                    onSuccess = { MovePhotoResult.Success(it) },
                    onFailure = { MovePhotoResult.Error(it.message ?: "复制失败") }
                )
            }
            AlbumAddAction.MOVE -> {
                movePhotoToAlbum(photoUri, targetAlbumPath)
            }
        }
    }
    
    /**
     * Delete an album from the system.
     * Note: This only works if the album is empty or user has proper permissions.
     * 
     * @param bucketId The bucket ID of the album to delete
     * @return IntentSender for delete confirmation if needed, null if deleted successfully
     */
    suspend fun deleteAlbum(bucketId: String): IntentSender? = withContext(Dispatchers.IO) {
        // First, remove from bubble list
        albumBubbleDao.deleteByBucketId(bucketId)
        
        // Get all photos in the album to create delete request
        val photos = mediaStoreDataSource.getPhotosFromAlbum(bucketId)
        if (photos.isEmpty()) {
            return@withContext null // Album is already empty
        }
        
        // Create delete request for all photos in the album
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = photos.map { Uri.parse(it.systemUri) }
            try {
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
