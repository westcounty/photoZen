package com.example.photozen.domain.usecase

import android.content.IntentSender
import android.net.Uri
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.TagDao
import com.example.photozen.data.local.entity.AlbumCopyMode
import com.example.photozen.data.local.entity.PhotoTagCrossRef
import com.example.photozen.data.local.entity.TagEntity
import com.example.photozen.data.source.Album
import com.example.photozen.data.source.DeleteResult
import com.example.photozen.data.source.MediaStoreDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Result of linking a tag to an album.
 */
sealed class LinkAlbumResult {
    data class Success(
        val albumId: String,
        val albumName: String,
        val photosAdded: Int
    ) : LinkAlbumResult()
    
    data class Error(val message: String) : LinkAlbumResult()
}

/**
 * Result of syncing a tag with its linked album.
 */
data class SyncResult(
    val photosAddedToTag: Int,
    val photosRemovedFromTag: Int,
    val photosCopiedToAlbum: Int
)

/**
 * Result of deleting a tag with its linked album.
 */
sealed class DeleteTagResult {
    /** Tag deleted successfully (no album deletion or album deleted without confirmation) */
    data object Success : DeleteTagResult()
    
    /** User confirmation required to delete album photos */
    data class RequiresConfirmation(
        val tagId: String,
        val intentSender: IntentSender,
        val uris: List<Uri>
    ) : DeleteTagResult()
    
    /** Deletion failed */
    data class Failed(val message: String) : DeleteTagResult()
}

/**
 * Result of creating and linking album (with potential pending deletions for MOVE mode).
 */
sealed class CreateLinkAlbumResult {
    data class Success(
        val albumId: String,
        val albumName: String,
        val photosAdded: Int
    ) : CreateLinkAlbumResult()
    
    /** User confirmation required to delete original photos (MOVE mode) */
    data class RequiresDeleteConfirmation(
        val albumId: String,
        val albumName: String,
        val photosAdded: Int,
        val intentSender: IntentSender,
        val pendingDeleteUris: List<Uri>
    ) : CreateLinkAlbumResult()
    
    data class Error(val message: String) : CreateLinkAlbumResult()
}

/**
 * UseCase for managing tag-album synchronization.
 * 
 * This handles:
 * - Creating new albums and linking them to tags
 * - Linking existing albums to tags
 * - Syncing photos between tags and albums (bidirectional)
 * - Deleting linked albums when tags are deleted
 */
class TagAlbumSyncUseCase @Inject constructor(
    private val tagDao: TagDao,
    private val photoDao: PhotoDao,
    private val mediaStoreDataSource: MediaStoreDataSource
) {
    
    /**
     * Get all available albums for linking.
     */
    suspend fun getAvailableAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val linkedAlbumIds = tagDao.getAlbumLinkedTags().first()
            .mapNotNull { it.linkedAlbumId }
            .toSet()
        
        mediaStoreDataSource.getAllAlbums()
            .filter { it.id !in linkedAlbumIds }
    }
    
    /**
     * Create a new album and link it to a tag.
     * 
     * @param tagId The tag to link
     * @param albumName Name for the new album
     * @param copyMode Whether to COPY or MOVE photos
     * @return Result indicating success, need for confirmation (MOVE mode), or failure
     */
    suspend fun createAndLinkAlbum(
        tagId: String,
        albumName: String,
        copyMode: AlbumCopyMode
    ): CreateLinkAlbumResult = withContext(Dispatchers.IO) {
        try {
            // Check if album name already exists
            val existingAlbum = mediaStoreDataSource.findAlbumByName(albumName)
            if (existingAlbum != null) {
                return@withContext CreateLinkAlbumResult.Error("相册「$albumName」已存在")
            }
            
            // Get the relative path for the new album
            val albumInfo = mediaStoreDataSource.createAlbum(albumName)
                ?: return@withContext CreateLinkAlbumResult.Error("无法创建相册路径")
            
            val (tempBucketId, albumPath) = albumInfo
            
            // Get photos with this tag
            val photoIds = tagDao.getPhotoIdsWithTag(tagId).first()
            var photosCopied = 0
            var actualBucketId = tempBucketId
            
            // Collect URIs of original photos for MOVE mode deletion
            val originalUrisToDelete = mutableListOf<Uri>()
            
            // Always use copy first (for both COPY and MOVE modes)
            for (photoId in photoIds) {
                val photo = photoDao.getById(photoId)
                if (photo != null) {
                    val sourceUri = Uri.parse(photo.systemUri)
                    
                    // Always copy first
                    val newPhoto = mediaStoreDataSource.copyPhotoToAlbum(sourceUri, albumPath)
                    
                    if (newPhoto != null) {
                        // Save the new photo to our database
                        photoDao.insert(newPhoto)
                        // Add the tag to the new photo
                        tagDao.addTagToPhoto(PhotoTagCrossRef(newPhoto.id, tagId))
                        
                        // Get the actual bucket_id from the first copied photo
                        if (photosCopied == 0 && newPhoto.bucketId != null) {
                            actualBucketId = newPhoto.bucketId
                        }
                        
                        // Always remove the old photo-tag relationship to avoid duplicates
                        tagDao.removeTagFromPhoto(photoId, tagId)
                        
                        // For MOVE mode, collect original URIs for deletion
                        if (copyMode == AlbumCopyMode.MOVE) {
                            originalUrisToDelete.add(sourceUri)
                        }
                        
                        photosCopied++
                    }
                }
            }
            
            // If we didn't copy any photos, try to find the album anyway
            if (photosCopied == 0) {
                val foundAlbum = mediaStoreDataSource.findAlbumByName(albumName)
                if (foundAlbum != null) {
                    actualBucketId = foundAlbum.id
                }
            }
            
            // Update the tag with album link
            tagDao.updateAlbumLink(tagId, actualBucketId, albumName, copyMode.name)
            
            // For MOVE mode, request deletion of original photos
            if (copyMode == AlbumCopyMode.MOVE && originalUrisToDelete.isNotEmpty()) {
                when (val deleteResult = mediaStoreDataSource.deletePhotos(originalUrisToDelete)) {
                    is DeleteResult.RequiresConfirmation -> {
                        CreateLinkAlbumResult.RequiresDeleteConfirmation(
                            albumId = actualBucketId,
                            albumName = albumName,
                            photosAdded = photosCopied,
                            intentSender = deleteResult.intentSender,
                            pendingDeleteUris = deleteResult.uris
                        )
                    }
                    is DeleteResult.Success -> {
                        CreateLinkAlbumResult.Success(
                            albumId = actualBucketId,
                            albumName = albumName,
                            photosAdded = photosCopied
                        )
                    }
                    is DeleteResult.Failed -> {
                        // Photos copied but original deletion failed - still return success
                        // User can manually delete originals
                        CreateLinkAlbumResult.Success(
                            albumId = actualBucketId,
                            albumName = albumName,
                            photosAdded = photosCopied
                        )
                    }
                }
            } else {
                CreateLinkAlbumResult.Success(
                    albumId = actualBucketId,
                    albumName = albumName,
                    photosAdded = photosCopied
                )
            }
        } catch (e: Exception) {
            CreateLinkAlbumResult.Error("关联相册失败: ${e.message}")
        }
    }
    
    /**
     * Link an existing album to a tag.
     * Photos in the album will be automatically tagged.
     * 
     * @param tagId The tag to link
     * @param album The existing album to link
     * @param copyMode Mode for future photo additions
     * @return Result indicating success or failure
     */
    suspend fun linkExistingAlbum(
        tagId: String,
        album: Album,
        copyMode: AlbumCopyMode
    ): LinkAlbumResult = withContext(Dispatchers.IO) {
        try {
            // Check if album is already linked
            if (tagDao.isAlbumLinked(album.id)) {
                return@withContext LinkAlbumResult.Error("该相册已关联到其他标签")
            }
            
            // Get all photos from the album
            val albumPhotos = mediaStoreDataSource.getPhotosFromAlbum(album.id)
            var photosTagged = 0
            
            // Tag each photo in the album
            for (photo in albumPhotos) {
                // Ensure photo exists in our database
                val existingPhoto = photoDao.getById(photo.id)
                if (existingPhoto == null) {
                    photoDao.insert(photo)
                }
                
                // Add the tag
                if (!tagDao.photoHasTag(photo.id, tagId)) {
                    tagDao.addTagToPhoto(PhotoTagCrossRef(photo.id, tagId))
                    photosTagged++
                }
            }
            
            // Update the tag with album link
            tagDao.updateAlbumLink(tagId, album.id, album.name, copyMode.name)
            
            LinkAlbumResult.Success(
                albumId = album.id,
                albumName = album.name,
                photosAdded = photosTagged
            )
        } catch (e: Exception) {
            LinkAlbumResult.Error("关联相册失败: ${e.message}")
        }
    }
    
    /**
     * Sync a tag with its linked album.
     * - Photos added to album externally -> add tag
     * - Photos removed from album externally -> remove tag
     * - Photos added to tag -> copy to album
     */
    suspend fun syncTagWithAlbum(tagId: String): SyncResult = withContext(Dispatchers.IO) {
        val tag = tagDao.getById(tagId)
        if (tag?.linkedAlbumId == null) {
            return@withContext SyncResult(0, 0, 0)
        }
        
        var photosAddedToTag = 0
        var photosRemovedFromTag = 0
        var photosCopiedToAlbum = 0
        
        val albumId = tag.linkedAlbumId
        val copyMode = tag.albumCopyMode ?: AlbumCopyMode.COPY
        
        // Get current photos in album from MediaStore
        val albumPhotoIds = mediaStoreDataSource.getPhotoIdsFromAlbum(albumId)
        
        // Get current photos with this tag
        val taggedPhotoIds = tagDao.getPhotoIdsWithTag(tagId).first().toSet()
        
        // Photos in album but not tagged -> add tag
        for (photoId in albumPhotoIds) {
            if (photoId !in taggedPhotoIds) {
                // Ensure photo exists in our database
                val photo = photoDao.getById(photoId)
                if (photo == null) {
                    // Fetch from MediaStore and save
                    val msId = photoId.removePrefix("ms_").toLongOrNull()
                    if (msId != null) {
                        val newPhoto = mediaStoreDataSource.fetchPhotoById(msId)
                        if (newPhoto != null) {
                            photoDao.insert(newPhoto)
                        }
                    }
                }
                
                tagDao.addTagToPhoto(PhotoTagCrossRef(photoId, tagId))
                photosAddedToTag++
            }
        }
        
        // Photos tagged but not in album -> either copy to album or remove tag
        // For now, we remove the tag (assuming external deletion)
        for (photoId in taggedPhotoIds) {
            val photoEntity = photoDao.getById(photoId)
            if (photoEntity == null) {
                // Photo deleted from our DB, remove tag
                tagDao.removeTagFromPhoto(photoId, tagId)
                photosRemovedFromTag++
                continue
            }
            
            // Check if photo's bucket_id matches the linked album
            if (photoEntity.bucketId == albumId) {
                // Photo is in the album, all good
                continue
            }
            
            // Photo is not in the album - check if it exists in MediaStore
            val exists = mediaStoreDataSource.photoExists(photoEntity.systemUri)
            if (!exists) {
                // Photo was deleted externally, remove tag
                tagDao.removeTagFromPhoto(photoId, tagId)
                photosRemovedFromTag++
            }
            // If photo exists but in different album, keep the tag
            // (user might have multiple tags)
        }
        
        SyncResult(photosAddedToTag, photosRemovedFromTag, photosCopiedToAlbum)
    }
    
    /**
     * Add a photo to a tag and sync to linked album if applicable.
     */
    suspend fun addPhotoToTagWithSync(photoId: String, tagId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Add the tag
            tagDao.addTagToPhoto(PhotoTagCrossRef(photoId, tagId))
            
            // Check if tag has a linked album
            val tag = tagDao.getById(tagId)
            if (tag?.linkedAlbumId != null && tag.linkedAlbumName != null) {
                val photoEntity = photoDao.getById(photoId)
                if (photoEntity != null && photoEntity.bucketId != tag.linkedAlbumId) {
                    // Photo is not in the linked album - copy it there
                    val album = mediaStoreDataSource.findAlbumByName(tag.linkedAlbumName)
                    if (album != null) {
                        val (_, albumPath) = mediaStoreDataSource.createAlbum(tag.linkedAlbumName)
                            ?: return@withContext true
                        
                        val sourceUri = Uri.parse(photoEntity.systemUri)
                        val copyMode = tag.albumCopyMode ?: AlbumCopyMode.COPY
                        
                        val newPhoto = if (copyMode == AlbumCopyMode.COPY) {
                            mediaStoreDataSource.copyPhotoToAlbum(sourceUri, albumPath)
                        } else {
                            mediaStoreDataSource.movePhotoToAlbum(sourceUri, albumPath)
                        }
                        
                        if (newPhoto != null) {
                            photoDao.insert(newPhoto)
                            tagDao.addTagToPhoto(PhotoTagCrossRef(newPhoto.id, tagId))
                        }
                    }
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Unlink album from tag.
     */
    suspend fun unlinkAlbum(tagId: String) = withContext(Dispatchers.IO) {
        tagDao.removeAlbumLink(tagId)
    }
    
    /**
     * Delete tag and optionally its linked album.
     * 
     * @param tagId The tag to delete
     * @param deleteLinkedAlbum Whether to also delete the linked album
     * @return DeleteTagResult indicating success, need for confirmation, or failure
     */
    suspend fun deleteTagWithAlbum(
        tagId: String,
        deleteLinkedAlbum: Boolean
    ): DeleteTagResult = withContext(Dispatchers.IO) {
        try {
            val tag = tagDao.getById(tagId)
            
            // Delete linked album if requested
            if (deleteLinkedAlbum && tag?.linkedAlbumId != null) {
                when (val deleteResult = mediaStoreDataSource.deleteAlbum(tag.linkedAlbumId)) {
                    is DeleteResult.Success -> {
                        // Album deleted, now delete the tag
                        tagDao.deleteById(tagId)
                        DeleteTagResult.Success
                    }
                    is DeleteResult.RequiresConfirmation -> {
                        // Need user confirmation - don't delete tag yet
                        DeleteTagResult.RequiresConfirmation(
                            tagId = tagId,
                            intentSender = deleteResult.intentSender,
                            uris = deleteResult.uris
                        )
                    }
                    is DeleteResult.Failed -> {
                        DeleteTagResult.Failed(deleteResult.message)
                    }
                }
            } else {
                // No album to delete, just delete the tag
                tagDao.deleteById(tagId)
                DeleteTagResult.Success
            }
        } catch (e: Exception) {
            DeleteTagResult.Failed("删除标签失败: ${e.message}")
        }
    }
    
    /**
     * Complete tag deletion after user confirms album photo deletion.
     * Called after user confirms the delete request.
     */
    suspend fun completeTagDeletion(tagId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            tagDao.deleteById(tagId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Sync all tags with linked albums.
     * Should be called during app sync.
     */
    suspend fun syncAllLinkedTags(): Int = withContext(Dispatchers.IO) {
        var totalChanges = 0
        
        val linkedTags = tagDao.getAlbumLinkedTags().first()
        for (tag in linkedTags) {
            val result = syncTagWithAlbum(tag.id)
            totalChanges += result.photosAddedToTag + result.photosRemovedFromTag
        }
        
        totalChanges
    }
}
