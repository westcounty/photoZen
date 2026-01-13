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
     * @param scale Zoom level (0.5 = half, 1.0 = original, up to 10.0 = 10x zoom)
     * @param offsetX Horizontal pan offset in pixels
     * @param offsetY Vertical pan offset in pixels
     * @param rotation Rotation angle in degrees (-45 to 45)
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
     * Update crop state with CropState object (includes aspect ratio and frame dimensions).
     */
    suspend fun update(photoId: String, cropState: CropState) {
        photoRepository.updateCropState(
            photoId = photoId,
            scale = cropState.scale,
            offsetX = cropState.offsetX,
            offsetY = cropState.offsetY,
            rotation = cropState.rotation,
            aspectRatioId = cropState.aspectRatioId,
            cropFrameWidth = cropState.cropFrameWidth,
            cropFrameHeight = cropState.cropFrameHeight
        )
    }
    
    /**
     * Reset crop state to default (no transformations).
     */
    suspend fun reset(photoId: String) {
        photoRepository.resetCropState(photoId)
    }
}
