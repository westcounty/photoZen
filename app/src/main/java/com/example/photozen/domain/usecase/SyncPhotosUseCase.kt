package com.example.photozen.domain.usecase

import com.example.photozen.data.repository.PhotoRepository
import javax.inject.Inject

/**
 * UseCase for synchronizing photos between MediaStore and local database.
 * 
 * Handles:
 * - Initial sync when app first launches
 * - Incremental sync to detect new photos
 * - Checking sync status
 */
class SyncPhotosUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    /**
     * Result of sync operation.
     */
    data class SyncResult(
        val newPhotosCount: Int,
        val isInitialSync: Boolean
    )
    
    /**
     * Perform photo sync from MediaStore to local database.
     * 
     * @return SyncResult containing the number of new photos and whether it was initial sync
     */
    suspend operator fun invoke(): SyncResult {
        val isInitialSync = !photoRepository.hasPerformedInitialSync()
        val newPhotosCount = photoRepository.syncFromMediaStore()
        
        return SyncResult(
            newPhotosCount = newPhotosCount,
            isInitialSync = isInitialSync
        )
    }
    
    /**
     * Check if initial sync has been performed.
     */
    suspend fun hasPerformedInitialSync(): Boolean {
        return photoRepository.hasPerformedInitialSync()
    }
}
