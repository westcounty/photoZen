package com.example.photozen.domain.usecase

import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.CropState
import com.example.photozen.data.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for creating and managing virtual copies of photos.
 * 
 * Virtual copies allow users to create multiple versions of a photo
 * with different crop states without duplicating the actual file.
 * 
 * Use cases:
 * - Comparing different crops of the same photo
 * - Creating multiple aspect ratio versions
 * - Experimenting with different edits non-destructively
 */
class CreateVirtualCopyUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    /**
     * Create a new virtual copy of a photo with optional crop state.
     * 
     * @param photoId The ID of the original photo to copy
     * @param cropState The crop state to apply to the virtual copy (uses current crop if null)
     * @return The ID of the newly created virtual copy
     */
    suspend operator fun invoke(photoId: String, cropState: CropState? = null): String {
        return photoRepository.createVirtualCopy(photoId, cropState)
    }
    
    /**
     * Delete a virtual copy.
     * Only works on virtual copies, not original photos.
     * 
     * @param virtualCopyId The ID of the virtual copy to delete
     */
    suspend fun deleteVirtualCopy(virtualCopyId: String) {
        photoRepository.deleteVirtualCopy(virtualCopyId)
    }
    
    /**
     * Get all virtual copies of a photo.
     * 
     * @param parentId The ID of the original photo
     * @return Flow of virtual copies list
     */
    fun getVirtualCopies(parentId: String): Flow<List<PhotoEntity>> {
        return photoRepository.getVirtualCopies(parentId)
    }
}
