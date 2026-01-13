package com.example.photozen.domain.usecase

import androidx.paging.PagingData
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for retrieving photos from the repository.
 * Provides various methods for different photo listing scenarios.
 */
class GetPhotosUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    /**
     * Get all photos as Flow.
     */
    fun getAllPhotos(): Flow<List<PhotoEntity>> {
        return photoRepository.getAllPhotos()
    }
    
    /**
     * Get all photos with paging support.
     */
    fun getAllPhotosPaged(): Flow<PagingData<PhotoEntity>> {
        return photoRepository.getAllPhotosPaged()
    }
    
    /**
     * Get photos by status.
     */
    fun getPhotosByStatus(status: PhotoStatus): Flow<List<PhotoEntity>> {
        return photoRepository.getPhotosByStatus(status)
    }
    
    /**
     * Get photos by status with paging.
     */
    fun getPhotosByStatusPaged(status: PhotoStatus): Flow<PagingData<PhotoEntity>> {
        return photoRepository.getPhotosByStatusPaged(status)
    }
    
    /**
     * Get a single photo by ID (suspend).
     */
    suspend fun getPhotoByIdSuspend(photoId: String): PhotoEntity? {
        return photoRepository.getPhotoById(photoId)
    }
    
    /**
     * Get a single photo by ID as Flow (for reactive updates).
     */
    fun getPhotoById(photoId: String): Flow<PhotoEntity?> {
        return photoRepository.getPhotoByIdFlow(photoId)
    }
    
    /**
     * Get a single photo by ID as Flow (alias).
     */
    fun getPhotoByIdFlow(photoId: String): Flow<PhotoEntity?> {
        return photoRepository.getPhotoByIdFlow(photoId)
    }
    
    /**
     * Get virtual copies of a photo.
     * Virtual copies are photos that have isVirtualCopy=true and parentId pointing to the original.
     */
    fun getVirtualCopies(photoId: String): Flow<List<PhotoEntity>> {
        return photoRepository.getVirtualCopies(photoId)
    }
    
    /**
     * Get total photo count.
     */
    fun getTotalCount(): Flow<Int> {
        return photoRepository.getTotalCount()
    }
    
    /**
     * Get count by status.
     */
    fun getCountByStatus(status: PhotoStatus): Flow<Int> {
        return photoRepository.getCountByStatus(status)
    }
}
