package com.example.photozen.domain.usecase

import androidx.paging.PagingData
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoSortOrder
import com.example.photozen.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for retrieving unsorted photos for the Flow Sorter screen.
 * Unsorted photos are those that haven't been categorized as KEEP, TRASH, or MAYBE.
 */
class GetUnsortedPhotosUseCase @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val photoDao: PhotoDao
) {
    companion object {
        /**
         * Page size for paginated queries.
         * 500 is chosen to balance between memory usage and number of database calls.
         * Too large may cause CursorWindow overflow on rapid scrolling.
         */
        const val PAGE_SIZE = 500
        
        /**
         * Threshold for preloading next batch.
         * When remaining photos drop below this, load the next page.
         */
        const val PRELOAD_THRESHOLD = 50
    }
    
    /**
     * Get all unsorted photos as Flow.
     * Used for Flow Sorter's swipe card stack.
     */
    operator fun invoke(): Flow<List<PhotoEntity>> {
        return photoRepository.getUnsortedPhotos()
    }
    
    /**
     * Get all unsorted photos as Flow with sort order.
     * @param ascending If true, sort by date ascending (oldest first), otherwise descending (newest first).
     */
    operator fun invoke(ascending: Boolean): Flow<List<PhotoEntity>> {
        return photoRepository.getUnsortedPhotos(ascending)
    }
    
    /**
     * Get unsorted photos filtered by bucket IDs (for camera only mode).
     */
    fun byBuckets(bucketIds: List<String>): Flow<List<PhotoEntity>> {
        return photoRepository.getUnsortedPhotosByBuckets(bucketIds)
    }
    
    /**
     * Get unsorted photos filtered by bucket IDs with sort order.
     */
    fun byBuckets(bucketIds: List<String>, ascending: Boolean): Flow<List<PhotoEntity>> {
        return photoRepository.getUnsortedPhotosByBuckets(bucketIds, ascending)
    }
    
    /**
     * Get unsorted photos excluding specific bucket IDs (for exclude camera mode).
     */
    fun excludingBuckets(bucketIds: List<String>): Flow<List<PhotoEntity>> {
        return photoRepository.getUnsortedPhotosExcludingBuckets(bucketIds)
    }
    
    /**
     * Get unsorted photos excluding specific bucket IDs with sort order.
     */
    fun excludingBuckets(bucketIds: List<String>, ascending: Boolean): Flow<List<PhotoEntity>> {
        return photoRepository.getUnsortedPhotosExcludingBuckets(bucketIds, ascending)
    }
    
    /**
     * Get unsorted photos with paging support.
     */
    fun paged(): Flow<PagingData<PhotoEntity>> {
        return photoRepository.getUnsortedPhotosPaged()
    }
    
    /**
     * Get count of unsorted photos.
     * Used to show progress indicator.
     */
    fun getCount(): Flow<Int> {
        return photoRepository.getUnsortedCount()
    }
    
    /**
     * Get count of unsorted photos filtered by bucket IDs.
     */
    fun getCountByBuckets(bucketIds: List<String>): Flow<Int> {
        return photoRepository.getUnsortedCountByBuckets(bucketIds)
    }
    
    /**
     * Get count of unsorted photos excluding specific bucket IDs.
     */
    fun getCountExcludingBuckets(bucketIds: List<String>): Flow<Int> {
        return photoRepository.getUnsortedCountExcludingBuckets(bucketIds)
    }
    
    // ==================== PAGINATED QUERIES ====================
    // These methods support proper pagination with database-level sorting.
    // Sorting is applied to ALL matching photos, then paginated.
    
    /**
     * Get a page of unsorted photos with specified sort order.
     * Sorting is done at database level on ALL unsorted photos, then paginated.
     * 
     * @param page Page number (0-indexed)
     * @param sortOrder How to sort the photos
     * @param randomSeed Seed for random sorting (ensures consistent order across pages)
     * @return List of photos for the requested page
     */
    suspend fun getPage(
        page: Int,
        sortOrder: PhotoSortOrder,
        randomSeed: Long = 0
    ): List<PhotoEntity> {
        val offset = page * PAGE_SIZE
        return when (sortOrder) {
            PhotoSortOrder.DATE_DESC -> photoDao.getUnsortedPhotosPagedDesc(PAGE_SIZE, offset)
            PhotoSortOrder.DATE_ASC -> photoDao.getUnsortedPhotosPagedAsc(PAGE_SIZE, offset)
            PhotoSortOrder.RANDOM -> photoDao.getUnsortedPhotosPagedRandom(randomSeed, PAGE_SIZE, offset)
        }
    }
    
    /**
     * Get a page of unsorted photos filtered by bucket IDs.
     */
    suspend fun getPageByBuckets(
        bucketIds: List<String>,
        page: Int,
        sortOrder: PhotoSortOrder,
        randomSeed: Long = 0
    ): List<PhotoEntity> {
        val offset = page * PAGE_SIZE
        return when (sortOrder) {
            PhotoSortOrder.DATE_DESC -> photoDao.getUnsortedPhotosByBucketsPagedDesc(bucketIds, PAGE_SIZE, offset)
            PhotoSortOrder.DATE_ASC -> photoDao.getUnsortedPhotosByBucketsPagedAsc(bucketIds, PAGE_SIZE, offset)
            PhotoSortOrder.RANDOM -> photoDao.getUnsortedPhotosByBucketsPagedRandom(bucketIds, randomSeed, PAGE_SIZE, offset)
        }
    }
    
    /**
     * Get a page of unsorted photos excluding specific bucket IDs.
     */
    suspend fun getPageExcludingBuckets(
        bucketIds: List<String>,
        page: Int,
        sortOrder: PhotoSortOrder,
        randomSeed: Long = 0
    ): List<PhotoEntity> {
        val offset = page * PAGE_SIZE
        return when (sortOrder) {
            PhotoSortOrder.DATE_DESC -> photoDao.getUnsortedPhotosExcludingBucketsPagedDesc(bucketIds, PAGE_SIZE, offset)
            PhotoSortOrder.DATE_ASC -> photoDao.getUnsortedPhotosExcludingBucketsPagedAsc(bucketIds, PAGE_SIZE, offset)
            PhotoSortOrder.RANDOM -> photoDao.getUnsortedPhotosExcludingBucketsPagedRandom(bucketIds, randomSeed, PAGE_SIZE, offset)
        }
    }
}
