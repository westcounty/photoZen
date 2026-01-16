package com.example.photozen.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.DailyStatsDao
import com.example.photozen.data.local.entity.DailyStats
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.local.entity.PhotoWithTags
import com.example.photozen.data.model.CropState
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.source.MediaStoreDataSource
import com.example.photozen.data.source.PhotoFilter
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
    private val dailyStatsDao: DailyStatsDao,
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
        
        // Update bucket_id for existing photos that don't have it
        updateBucketIdsForExistingPhotos(mediaStorePhotos)
        
        // Detect and remove photos that were deleted externally
        val deletedCount = removeDeletedPhotos()
        
        // Mark initial sync as done
        dataStore.edit { prefs ->
            prefs[KEY_INITIAL_SYNC_DONE] = true
        }
        
        return newPhotos.size
    }
    
    /**
     * Update bucket_id for existing photos that don't have it set.
     * This is needed for photos synced before bucket_id was added.
     */
    private suspend fun updateBucketIdsForExistingPhotos(mediaStorePhotos: List<PhotoEntity>) {
        // Create a map of system_uri to bucket_id from MediaStore photos
        val uriBucketMap = mediaStorePhotos.associate { it.systemUri to it.bucketId }
        
        // Get all photos from DB that need bucket_id update
        val photosNeedingUpdate = photoDao.getPhotosWithNullBucketId()
        
        // Update each photo with its bucket_id
        photosNeedingUpdate.forEach { photo ->
            val bucketId = uriBucketMap[photo.systemUri]
            if (bucketId != null) {
                photoDao.updateBucketId(photo.id, bucketId)
            }
        }
    }
    
    /**
     * Remove photos from database that no longer exist in MediaStore.
     * This handles the case where photos are deleted outside the app.
     */
    override suspend fun removeDeletedPhotos(): Int {
        // Get all MediaStore IDs currently in the system
        val currentMediaStoreIds = mediaStoreDataSource.getAllMediaStoreIds()
        
        // Get all photo IDs from our database (only non-virtual copies)
        val dbPhotoIds = photoDao.getAllNonVirtualPhotoIds()
        
        // Find photos in our DB that are no longer in MediaStore
        val deletedIds = dbPhotoIds.filter { id -> 
            // Only check photos that originated from MediaStore (have "ms_" prefix)
            id.startsWith("ms_") && id !in currentMediaStoreIds
        }
        
        // Remove deleted photos from database
        if (deletedIds.isNotEmpty()) {
            photoDao.deleteByIds(deletedIds)
            // Also remove any virtual copies of deleted photos
            deletedIds.forEach { parentId ->
                photoDao.deleteVirtualCopiesByParentId(parentId)
            }
        }
        
        return deletedIds.size
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
    
    override fun getUnsortedPhotosByBuckets(bucketIds: List<String>): Flow<List<PhotoEntity>> {
        return photoDao.getUnsortedPhotosByBuckets(bucketIds)
    }
    
    override fun getUnsortedPhotosExcludingBuckets(bucketIds: List<String>): Flow<List<PhotoEntity>> {
        return photoDao.getUnsortedPhotosExcludingBuckets(bucketIds)
    }
    
    override fun getUnsortedCountByBuckets(bucketIds: List<String>): Flow<Int> {
        return photoDao.getUnsortedCountByBuckets(bucketIds)
    }
    
    override fun getUnsortedCountExcludingBuckets(bucketIds: List<String>): Flow<Int> {
        return photoDao.getUnsortedCountExcludingBuckets(bucketIds)
    }
    
    override fun getTotalCountByBuckets(bucketIds: List<String>): Flow<Int> {
        return photoDao.getTotalCountByBuckets(bucketIds)
    }
    
    override fun getTotalCountExcludingBuckets(bucketIds: List<String>): Flow<Int> {
        return photoDao.getTotalCountExcludingBuckets(bucketIds)
    }
    
    override fun getSortedCountByBuckets(bucketIds: List<String>): Flow<Int> {
        return photoDao.getSortedCountByBuckets(bucketIds)
    }
    
    override fun getSortedCountExcludingBuckets(bucketIds: List<String>): Flow<Int> {
        return photoDao.getSortedCountExcludingBuckets(bucketIds)
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
    
    override suspend fun getFilteredUnsortedCount(filter: PhotoFilter): Int {
        // For filtered count, we need to consider the filter criteria
        // Get count from MediaStore based on filter, not from our DB
        return mediaStoreDataSource.getFilteredPhotoCount(filter)
    }
    
    // ==================== DAILY STATS ====================
    
    override fun getDailyStats(date: String): Flow<DailyStats?> {
        return dailyStatsDao.getStatsByDate(date)
    }
    
    override suspend fun incrementDailyStats(amount: Int) {
        val today = getTodayDateString()
        var currentStats = dailyStatsDao.getStatsByDateOneShot(today)
        
        if (currentStats == null) {
            val target = dataStore.data.first()[androidx.datastore.preferences.core.intPreferencesKey("daily_task_target")] ?: 100
            currentStats = DailyStats(date = today, count = 0, target = target)
            dailyStatsDao.insertOrUpdate(currentStats)
        }
        
        val newCount = currentStats.count + amount
        
        dailyStatsDao.incrementCount(today, amount)
        
        // Check if daily task target is reached (exactly or crossed)
        if (currentStats.count < currentStats.target && newCount >= currentStats.target) {
            dataStore.edit { prefs ->
                val currentCompleted = prefs[androidx.datastore.preferences.core.intPreferencesKey("daily_tasks_completed")] ?: 0
                prefs[androidx.datastore.preferences.core.intPreferencesKey("daily_tasks_completed")] = currentCompleted + 1
            }
        }
    }
    
    override suspend fun getRandomUnsortedPhoto(): PhotoEntity? {
        // Since we don't have a direct DAO method for random, we can fetch page 1 and pick random
        // or add a DAO method. Let's add a DAO method for efficiency.
        return photoDao.getRandomUnsortedPhoto()
    }
    
    private fun getTodayDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    // ==================== WRITE - Status ====================
    
    override suspend fun updatePhotoStatus(photoId: String, status: PhotoStatus) {
        photoDao.updateStatus(photoId, status)
        // Increment daily stats if sorting (KEEP, TRASH, MAYBE)
        if (status != PhotoStatus.UNSORTED) {
            incrementDailyStats(1)
        }
    }
    
    override suspend fun updatePhotoStatusBatch(photoIds: List<String>, status: PhotoStatus) {
        photoDao.updateStatusBatch(photoIds, status)
        // Increment daily stats if sorting
        if (status != PhotoStatus.UNSORTED) {
            incrementDailyStats(photoIds.size)
        }
    }
    
    override suspend fun keepPhoto(photoId: String) {
        photoDao.updateStatus(photoId, PhotoStatus.KEEP)
        incrementDailyStats(1)
    }
    
    override suspend fun trashPhoto(photoId: String) {
        photoDao.updateStatus(photoId, PhotoStatus.TRASH)
        incrementDailyStats(1)
    }
    
    override suspend fun maybePhoto(photoId: String) {
        photoDao.updateStatus(photoId, PhotoStatus.MAYBE)
        incrementDailyStats(1)
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

    override suspend fun deletePhotosByIds(photoIds: List<String>) {
        if (photoIds.isNotEmpty()) {
            photoDao.deleteByIds(photoIds)
        }
    }

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
