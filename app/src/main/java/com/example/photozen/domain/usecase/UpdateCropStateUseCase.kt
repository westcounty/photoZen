package com.example.photozen.domain.usecase

import com.example.photozen.data.model.CropState
import com.example.photozen.data.repository.PhotoRepository
import javax.inject.Inject

/**
 * UseCase for updating crop/transformation state of a photo.
 * 
 * Non-destructive: Only stores metadata, never modifies original file.
 * The crop state is used when displaying/exporting the photo.
 */
class UpdateCropStateUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    /**
     * Update crop state with individual parameters.
     * 
     * @param photoId Photo to update
     * @param scale Zoom level (1.0 = original, 2.0 = 2x zoom)
     * @param offsetX Horizontal pan offset (normalized 0-1)
     * @param offsetY Vertical pan offset (normalized 0-1)
     * @param rotation Rotation angle in degrees
     */
    suspend operator fun invoke(
        photoId: String,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float = 0f
    ) {
        photoRepository.updateCropState(
            photoId = photoId,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = rotation
        )
    }
    
    /**
     * Update crop state with CropState object.
     */
    suspend fun update(photoId: String, cropState: CropState) {
        photoRepository.updateCropState(
            photoId = photoId,
            scale = cropState.scale,
            offsetX = cropState.offsetX,
            offsetY = cropState.offsetY,
            rotation = cropState.rotation
        )
    }
    
    /**
     * Reset crop state to default (no transformations).
     */
    suspend fun reset(photoId: String) {
        photoRepository.resetCropState(photoId)
    }
}
