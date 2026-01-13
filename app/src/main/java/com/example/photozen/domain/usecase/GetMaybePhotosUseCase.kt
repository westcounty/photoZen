package com.example.photozen.domain.usecase

import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.PhotoStatus
import com.example.photozen.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for retrieving "Maybe" photos for the Light Table comparison screen.
 * Maybe photos are those marked for later review during sorting.
 */
class GetMaybePhotosUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    /**
     * Get all "Maybe" photos as Flow.
     * Used for Light Table's comparison grid.
     */
    operator fun invoke(): Flow<List<PhotoEntity>> {
        return photoRepository.getMaybePhotos()
    }
    
    /**
     * Get count of "Maybe" photos.
     */
    fun getCount(): Flow<Int> {
        return photoRepository.getCountByStatus(PhotoStatus.MAYBE)
    }
}
