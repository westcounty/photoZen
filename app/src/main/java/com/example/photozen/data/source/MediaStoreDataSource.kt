package com.example.photozen.data.source

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.CropState
import com.example.photozen.data.model.PhotoStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents an album (bucket) in MediaStore.
 */
data class Album(
    val id: String,
    val name: String,
    val photoCount: Int,
    val coverUri: String? = null,
    val isCamera: Boolean = false // True if this is a camera album (DCIM/Camera)
)

/**
 * Filter options for fetching photos.
 */
data class PhotoFilter(
    val albumIds: List<String>? = null,      // Filter by specific album IDs (null = all)
    val startDate: Long? = null,              // Start date in milliseconds (inclusive)
    val endDate: Long? = null,                // End date in milliseconds (inclusive)
    val cameraOnly: Boolean = false,          // Only camera photos
    val excludeCamera: Boolean = false        // Exclude camera photos
)

/**
 * Data source for reading photos from Android MediaStore.
 * Handles Android 13+ (API 33) permission changes and efficient querying.
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver
    
    /**
     * Collection URI for images based on Android version.
     */
    private val imageCollection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    
    /**
     * Projection (columns to query) for images.
     */
    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )
    
    /**
     * Check if an album name is a camera album.
     * Matches: Camera, camera, CAMERA, 相机, etc.
     */
    private fun isCameraAlbum(albumName: String): Boolean {
        val lowerName = albumName.lowercase()
        return lowerName == "camera" || 
               albumName == "相机" ||
               lowerName == "dcim" ||
               lowerName.startsWith("100") // 100ANDRO, 100MEDIA, etc.
    }
    
    /**
     * Fetch all images from MediaStore.
     * Returns a list of PhotoEntity objects ready to be inserted into Room.
     * 
     * @param limit Maximum number of photos to fetch (null = no limit)
     * @param offset Number of photos to skip
     */
    suspend fun fetchAllPhotos(
        limit: Int? = null,
        offset: Int = 0
    ): List<PhotoEntity> = withContext(Dispatchers.IO) {
        fetchPhotosWithFilter(PhotoFilter(), limit, offset)
    }
    
    /**
     * Fetch photos with filter options.
     */
    suspend fun fetchPhotosWithFilter(
        filter: PhotoFilter,
        limit: Int? = null,
        offset: Int = 0
    ): List<PhotoEntity> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<PhotoEntity>()
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        // Build selection based on filter
        val selectionBuilder = StringBuilder()
        val selectionArgs = mutableListOf<String>()
        
        // Album filter
        if (!filter.albumIds.isNullOrEmpty()) {
            val placeholders = filter.albumIds.joinToString(",") { "?" }
            selectionBuilder.append("${MediaStore.Images.Media.BUCKET_ID} IN ($placeholders)")
            selectionArgs.addAll(filter.albumIds)
        }
        
        // Date range filter
        if (filter.startDate != null) {
            if (selectionBuilder.isNotEmpty()) selectionBuilder.append(" AND ")
            selectionBuilder.append("${MediaStore.Images.Media.DATE_ADDED} >= ?")
            // DATE_ADDED is in seconds
            selectionArgs.add((filter.startDate / 1000).toString())
        }
        if (filter.endDate != null) {
            if (selectionBuilder.isNotEmpty()) selectionBuilder.append(" AND ")
            selectionBuilder.append("${MediaStore.Images.Media.DATE_ADDED} <= ?")
            selectionArgs.add((filter.endDate / 1000).toString())
        }
        
        // Camera only / exclude camera filter
        if (filter.cameraOnly || filter.excludeCamera) {
            val cameraAlbumIds = getCameraAlbumIds()
            if (cameraAlbumIds.isNotEmpty()) {
                val placeholders = cameraAlbumIds.joinToString(",") { "?" }
                if (selectionBuilder.isNotEmpty()) selectionBuilder.append(" AND ")
                if (filter.cameraOnly) {
                    selectionBuilder.append("${MediaStore.Images.Media.BUCKET_ID} IN ($placeholders)")
                } else {
                    selectionBuilder.append("${MediaStore.Images.Media.BUCKET_ID} NOT IN ($placeholders)")
                }
                selectionArgs.addAll(cameraAlbumIds)
            }
        }
        
        val selection = if (selectionBuilder.isNotEmpty()) selectionBuilder.toString() else null
        val args = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null
        
        contentResolver.query(
            imageCollection,
            projection,
            selection,
            args,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            
            // Skip to offset
            if (offset > 0) {
                cursor.moveToPosition(offset - 1)
            }
            
            var count = 0
            while (cursor.moveToNext()) {
                if (limit != null && count >= limit) break
                
                val photo = cursorToPhotoEntity(
                    cursor = cursor,
                    idColumn = idColumn,
                    displayNameColumn = displayNameColumn,
                    sizeColumn = sizeColumn,
                    widthColumn = widthColumn,
                    heightColumn = heightColumn,
                    mimeTypeColumn = mimeTypeColumn,
                    dateTakenColumn = dateTakenColumn,
                    dateAddedColumn = dateAddedColumn,
                    dateModifiedColumn = dateModifiedColumn,
                    bucketIdColumn = bucketIdColumn
                )
                photos.add(photo)
                count++
            }
        }
        
        photos
    }
    
    /**
     * Get all albums (buckets) from MediaStore.
     */
    suspend fun getAllAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albumMap = mutableMapOf<String, AlbumBuilder>()
        
        val albumProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        
        contentResolver.query(
            imageCollection,
            albumProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            
            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idColumn)
                val bucketId = cursor.getString(bucketIdColumn) ?: continue
                val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
                
                val builder = albumMap.getOrPut(bucketId) {
                    AlbumBuilder(
                        id = bucketId,
                        name = bucketName,
                        isCamera = isCameraAlbum(bucketName)
                    )
                }
                builder.count++
                if (builder.coverUri == null) {
                    builder.coverUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        mediaStoreId
                    ).toString()
                }
            }
        }
        
        albumMap.values.map { builder ->
            Album(
                id = builder.id,
                name = builder.name,
                photoCount = builder.count,
                coverUri = builder.coverUri,
                isCamera = builder.isCamera
            )
        }.sortedByDescending { it.photoCount }
    }
    
    /**
     * Get IDs of camera albums.
     */
    private suspend fun getCameraAlbumIds(): List<String> {
        return getAllAlbums().filter { it.isCamera }.map { it.id }
    }
    
    /**
     * Helper class for building Album objects.
     */
    private class AlbumBuilder(
        val id: String,
        val name: String,
        val isCamera: Boolean,
        var count: Int = 0,
        var coverUri: String? = null
    )
    
    /**
     * Get all MediaStore IDs currently in the system.
     * Used for detecting deleted photos during sync.
     */
    suspend fun getAllMediaStoreIds(): Set<String> = withContext(Dispatchers.IO) {
        val ids = mutableSetOf<String>()
        
        contentResolver.query(
            imageCollection,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idColumn)
                ids.add("ms_$mediaStoreId")
            }
        }
        
        ids
    }
    
    /**
     * Get count of photos matching the filter.
     */
    suspend fun getFilteredPhotoCount(filter: PhotoFilter): Int = withContext(Dispatchers.IO) {
        // Build selection based on filter
        val selectionBuilder = StringBuilder()
        val selectionArgs = mutableListOf<String>()
        
        // Album filter
        if (!filter.albumIds.isNullOrEmpty()) {
            val placeholders = filter.albumIds.joinToString(",") { "?" }
            selectionBuilder.append("${MediaStore.Images.Media.BUCKET_ID} IN ($placeholders)")
            selectionArgs.addAll(filter.albumIds)
        }
        
        // Date range filter
        if (filter.startDate != null) {
            if (selectionBuilder.isNotEmpty()) selectionBuilder.append(" AND ")
            selectionBuilder.append("${MediaStore.Images.Media.DATE_ADDED} >= ?")
            selectionArgs.add((filter.startDate / 1000).toString())
        }
        if (filter.endDate != null) {
            if (selectionBuilder.isNotEmpty()) selectionBuilder.append(" AND ")
            selectionBuilder.append("${MediaStore.Images.Media.DATE_ADDED} <= ?")
            selectionArgs.add((filter.endDate / 1000).toString())
        }
        
        // Camera only / exclude camera filter
        if (filter.cameraOnly || filter.excludeCamera) {
            val cameraAlbumIds = getCameraAlbumIds()
            if (cameraAlbumIds.isNotEmpty()) {
                val placeholders = cameraAlbumIds.joinToString(",") { "?" }
                if (selectionBuilder.isNotEmpty()) selectionBuilder.append(" AND ")
                if (filter.cameraOnly) {
                    selectionBuilder.append("${MediaStore.Images.Media.BUCKET_ID} IN ($placeholders)")
                } else {
                    selectionBuilder.append("${MediaStore.Images.Media.BUCKET_ID} NOT IN ($placeholders)")
                }
                selectionArgs.addAll(cameraAlbumIds)
            }
        }
        
        val selection = if (selectionBuilder.isNotEmpty()) selectionBuilder.toString() else null
        val args = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null
        
        contentResolver.query(
            imageCollection,
            arrayOf(MediaStore.Images.Media._ID),
            selection,
            args,
            null
        )?.use { it.count } ?: 0
    }
    
    /**
     * Fetch a single photo by its MediaStore ID.
     */
    suspend fun fetchPhotoById(mediaStoreId: Long): PhotoEntity? = withContext(Dispatchers.IO) {
        val selection = "${MediaStore.Images.Media._ID} = ?"
        val selectionArgs = arrayOf(mediaStoreId.toString())
        
        contentResolver.query(
            imageCollection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursorToPhotoEntity(
                    cursor = cursor,
                    idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID),
                    displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME),
                    sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE),
                    widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH),
                    heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT),
                    mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE),
                    dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN),
                    dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED),
                    dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                )
            } else null
        }
    }
    
    /**
     * Get total count of images in MediaStore.
     */
    suspend fun getPhotoCount(): Int = withContext(Dispatchers.IO) {
        contentResolver.query(
            imageCollection,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            null
        )?.use { it.count } ?: 0
    }
    
    /**
     * Check if a specific image still exists in MediaStore.
     * Useful for detecting deleted photos during sync.
     */
    suspend fun photoExists(contentUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(contentUri)
            contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Convert cursor row to PhotoEntity.
     */
    private fun cursorToPhotoEntity(
        cursor: Cursor,
        idColumn: Int,
        displayNameColumn: Int,
        sizeColumn: Int,
        widthColumn: Int,
        heightColumn: Int,
        mimeTypeColumn: Int,
        dateTakenColumn: Int,
        dateAddedColumn: Int,
        dateModifiedColumn: Int,
        bucketIdColumn: Int? = null
    ): PhotoEntity {
        val mediaStoreId = cursor.getLong(idColumn)
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaStoreId
        )
        
        return PhotoEntity(
            id = "ms_$mediaStoreId", // Prefix to distinguish from virtual copies
            systemUri = contentUri.toString(),
            status = PhotoStatus.UNSORTED,
            cropState = CropState.DEFAULT,
            isVirtualCopy = false,
            parentId = null,
            displayName = cursor.getString(displayNameColumn) ?: "",
            size = cursor.getLong(sizeColumn),
            width = cursor.getInt(widthColumn),
            height = cursor.getInt(heightColumn),
            mimeType = cursor.getString(mimeTypeColumn) ?: "image/*",
            dateTaken = cursor.getLong(dateTakenColumn),
            dateAdded = cursor.getLong(dateAddedColumn),
            dateModified = cursor.getLong(dateModifiedColumn),
            cameraMake = null, // Would need to read EXIF for this
            cameraModel = null,
            bucketId = bucketIdColumn?.let { cursor.getString(it) },
            isSynced = true
        )
    }
    
    /**
     * Create a new album (folder) in Pictures directory.
     * Returns the album name (used as relative path for Android 10+).
     * 
     * @param albumName Name of the album to create
     * @return The album name/path for use in subsequent operations, or null if failed
     */
    suspend fun createAlbum(albumName: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            // For Android 10+, we don't need to create the directory manually
            // It will be created automatically when we add the first file
            // Just return the album name for use in RELATIVE_PATH
            val relativePath = "${android.os.Environment.DIRECTORY_PICTURES}/$albumName"
            
            // Return a placeholder bucket_id (will be updated after first file is added)
            // and the relative path for MediaStore operations
            Pair(albumName.hashCode().toString(), relativePath)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Copy a photo to a target album using MediaStore API (works on Android 10+).
     * 
     * @param sourceUri Source content URI of the photo
     * @param targetAlbumPath Relative path for the target album (e.g., "Pictures/MyAlbum")
     * @param displayName Display name for the new file (if null, uses original name)
     * @return New PhotoEntity with updated systemUri and id, or null if failed
     */
    suspend fun copyPhotoToAlbum(
        sourceUri: Uri,
        targetAlbumPath: String,
        displayName: String? = null
    ): PhotoEntity? = withContext(Dispatchers.IO) {
        try {
            // Get source file info
            var originalName = displayName
            var mimeType = "image/jpeg"
            
            contentResolver.query(
                sourceUri,
                arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    if (originalName == null) {
                        originalName = cursor.getString(0)
                    }
                    mimeType = cursor.getString(1) ?: "image/jpeg"
                }
            }
            
            if (originalName == null) {
                originalName = "IMG_${System.currentTimeMillis()}.jpg"
            }
            
            // Create new entry in MediaStore using proper API for Android 10+
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, originalName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, targetAlbumPath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val newUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: return@withContext null
            
            // Copy the file content
            try {
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    contentResolver.openOutputStream(newUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Mark as not pending (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val updateValues = android.content.ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    contentResolver.update(newUri, updateValues, null, null)
                }
            } catch (e: Exception) {
                // Clean up on failure
                contentResolver.delete(newUri, null, null)
                return@withContext null
            }
            
            // Fetch the newly created photo entity
            val mediaStoreId = ContentUris.parseId(newUri)
            fetchPhotoById(mediaStoreId)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Move a photo to a target album (copy then delete original).
     * Returns the new PhotoEntity, or null if failed.
     */
    suspend fun movePhotoToAlbum(
        sourceUri: Uri,
        targetAlbumPath: String,
        displayName: String? = null
    ): PhotoEntity? = withContext(Dispatchers.IO) {
        val newPhoto = copyPhotoToAlbum(sourceUri, targetAlbumPath, displayName)
        if (newPhoto != null) {
            // Try to delete original
            try {
                contentResolver.delete(sourceUri, null, null)
            } catch (e: Exception) {
                // Ignore deletion errors - at least the copy succeeded
            }
        }
        newPhoto
    }
    
    /**
     * Get photos from a specific album by bucket ID.
     */
    suspend fun getPhotosFromAlbum(bucketId: String): List<PhotoEntity> = withContext(Dispatchers.IO) {
        fetchPhotosWithFilter(PhotoFilter(albumIds = listOf(bucketId)))
    }
    
    /**
     * Get all MediaStore IDs from a specific album.
     */
    suspend fun getPhotoIdsFromAlbum(bucketId: String): Set<String> = withContext(Dispatchers.IO) {
        val ids = mutableSetOf<String>()
        
        contentResolver.query(
            imageCollection,
            arrayOf(MediaStore.Images.Media._ID),
            "${MediaStore.Images.Media.BUCKET_ID} = ?",
            arrayOf(bucketId),
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idColumn)
                ids.add("ms_$mediaStoreId")
            }
        }
        
        ids
    }
    
    /**
     * Check if an album exists by name and return its bucket ID.
     */
    suspend fun findAlbumByName(albumName: String): Album? = withContext(Dispatchers.IO) {
        getAllAlbums().find { it.name.equals(albumName, ignoreCase = true) }
    }
    
    /**
     * Delete an album and all its photos.
     * Returns true if successful.
     */
    suspend fun deleteAlbum(bucketId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete all photos in the album
            val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
            val selectionArgs = arrayOf(bucketId)
            
            contentResolver.delete(
                imageCollection,
                selection,
                selectionArgs
            )
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        /**
         * Generate a unique ID for a virtual copy.
         */
        fun generateVirtualCopyId(): String = "vc_${UUID.randomUUID()}"
    }
}
