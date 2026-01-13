package com.example.photozen.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.PhotoWithTags
import com.example.photozen.data.model.CropState
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.source.MediaStoreDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PhotoRepository.
 * Coordinates between Room database and MediaStore data source.
 */
@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val dataStore: DataStore<Preferences>
) : PhotoRepository {
    
    companion object {
        private val KEY_INITIAL_SYNC_DONE = booleanPreferencesKey("initial_sync_done")
        
        /**
         * Page size for Paging3.
         */
        private const val PAGE_SIZE = 30
    }
    
    // ==================== SYNC ====================
    
    override suspend fun syncFromMediaStore(): Int {
        // Get existing URIs from database
        val existingUris = photoDao.getAllSystemUris().toSet()
        
        // Fetch all photos from MediaStore
        val mediaStorePhotos = mediaStoreDataSource.fetchAllPhotos()
        
        // Filter only new photos (not already in DB)
        val newPhotos = mediaStorePhotos.filter { photo ->
            photo.systemUri !in existingUris
        }
        
        // Insert new photos
        if (newPhotos.isNotEmpty()) {
            photoDao.insertAll(newPhotos)
        }
        
        // Mark initial sync as done
        dataStore.edit { prefs ->
            prefs[KEY_INITIAL_SYNC_DONE] = true
        }
        
        return newPhotos.size
    }
    
    override suspend fun hasPerformedInitialSync(): Boolean {
        return dataStore.data.first()[KEY_INITIAL_SYNC_DONE] ?: false
    }
    
    // ==================== READ - Single ====================
    
    override suspend fun getPhotoById(photoId: String): PhotoEntity? {
        return photoDao.getById(photoId)
    }
    
    override fun getPhotoByIdFlow(photoId: String): Flow<PhotoEntity?> {
        return photoDao.getByIdFlow(photoId)
    }
    
    override suspend fun getPhotoWithTags(photoId: String): PhotoWithTags? {
        return photoDao.getWithTagsById(photoId)
    }
    
    // ==================== READ - Lists ====================
    
    override fun getAllPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getAllPhotos()
    }
    
    override fun getPhotosByStatus(status: PhotoStatus): Flow<List<PhotoEntity>> {
        return photoDao.getPhotosByStatus(status)
    }
    
    override fun getUnsortedPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getUnsortedPhotos()
    }
    
    override fun getMaybePhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getMaybePhotos()
    }
    
    override fun getKeepPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getKeepPhotos()
    }
    
    override fun getTrashPhotos(): Flow<List<PhotoEntity>> {
        return photoDao.getTrashPhotos()
    }
    
    override fun getVirtualCopies(parentId: String): Flow<List<PhotoEntity>> {
        return photoDao.getVirtualCopies(parentId)
    }
    
    // ==================== READ - Paging ====================
    
    override fun getAllPhotosPaged(): Flow<PagingData<PhotoEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PAGE_SIZE / 2
            ),
            pagingSourceFactory = { photoDao.getAllPhotosPaged() }
        ).flow
    }
    
    override fun getUnsortedPhotosPaged(): Flow<PagingData<PhotoEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PAGE_SIZE / 2
            ),
            pagingSourceFactory = { photoDao.getUnsortedPhotosPaged() }
        ).flow
    }
    
    override fun getPhotosByStatusPaged(status: PhotoStatus): Flow<PagingData<PhotoEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PAGE_SIZE / 2
            ),
            pagingSourceFactory = { photoDao.getPhotosByStatusPaged(status) }
        ).flow
    }
    
    // ==================== READ - Counts ====================
    
    override fun getTotalCount(): Flow<Int> {
        return photoDao.getTotalCount()
    }
    
    override fun getCountByStatus(status: PhotoStatus): Flow<Int> {
        return photoDao.getCountByStatus(status)
    }
    
    override fun getUnsortedCount(): Flow<Int> {
        return photoDao.getUnsortedCount()
    }
    
    // ==================== WRITE - Status ====================
    
    override suspend fun updatePhotoStatus(photoId: String, status: PhotoStatus) {
        photoDao.updateStatus(photoId, status)
    }
    
    override suspend fun updatePhotoStatusBatch(photoIds: List<String>, status: PhotoStatus) {
        photoDao.updateStatusBatch(photoIds, status)
    }
    
    override suspend fun keepPhoto(photoId: String) {
        photoDao.updateStatus(photoId, PhotoStatus.KEEP)
    }
    
    override suspend fun trashPhoto(photoId: String) {
        photoDao.updateStatus(photoId, PhotoStatus.TRASH)
    }
    
    override suspend fun maybePhoto(photoId: String) {
        photoDao.updateStatus(photoId, PhotoStatus.MAYBE)
    }
    
    override suspend fun resetPhotoStatus(photoId: String) {
        photoDao.updateStatus(photoId, PhotoStatus.UNSORTED)
    }
    
    // ==================== WRITE - Virtual Copies ====================
    
    override suspend fun createVirtualCopy(photoId: String, cropState: CropState?): String {
        val originalPhoto = photoDao.getById(photoId)
            ?: throw IllegalArgumentException("Photo not found: $photoId")
        
        val virtualCopyId = MediaStoreDataSource.generateVirtualCopyId()
        
        val virtualCopy = originalPhoto.copy(
            id = virtualCopyId,
            isVirtualCopy = true,
            parentId = originalPhoto.id,
            status = PhotoStatus.UNSORTED,
            cropState = cropState ?: originalPhoto.cropState,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        photoDao.insert(virtualCopy)
        
        return virtualCopyId
    }
    
    override suspend fun deleteVirtualCopy(virtualCopyId: String) {
        val photo = photoDao.getById(virtualCopyId)
        if (photo?.isVirtualCopy == true) {
            photoDao.deleteById(virtualCopyId)
        }
    }
    
    // ==================== WRITE - Crop State ====================
    
    override suspend fun updateCropState(
        photoId: String,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float,
        aspectRatioId: String,
        cropFrameWidth: Float,
        cropFrameHeight: Float
    ) {
        val photo = photoDao.getById(photoId) ?: return
        val updatedPhoto = photo.copy(
            cropState = CropState(
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                rotation = rotation,
                aspectRatioId = aspectRatioId,
                cropFrameWidth = cropFrameWidth,
                cropFrameHeight = cropFrameHeight
            ),
            updatedAt = System.currentTimeMillis()
        )
        photoDao.update(updatedPhoto)
    }
    
    override suspend fun resetCropState(photoId: String) {
        val photo = photoDao.getById(photoId) ?: return
        val updatedPhoto = photo.copy(
            cropState = CropState.DEFAULT,
            updatedAt = System.currentTimeMillis()
        )
        photoDao.update(updatedPhoto)
    }
    
    // ==================== DELETE ====================
    
    override suspend fun emptyTrash() {
        photoDao.deleteAllTrashed()
    }
    
    override fun getTrashCount(): Flow<Int> {
        return photoDao.getCountByStatus(PhotoStatus.TRASH)
    }
    
    // ==================== CAMERA COLLECTION ====================
    
    override fun getDistinctCameraModels(): Flow<List<String>> {
        return photoDao.getDistinctCameraModels()
    }
    
    override fun getPhotosByCameraModel(cameraModel: String): Flow<List<PhotoEntity>> {
        return photoDao.getPhotosByCameraModel(cameraModel)
    }
}
