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
        MediaStore.Images.Media.DATE_MODIFIED
    )
    
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
        val photos = mutableListOf<PhotoEntity>()
        
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        
        // Build selection with limit/offset for pagination
        val selection: String? = null
        val selectionArgs: Array<String>? = null
        
        contentResolver.query(
            imageCollection,
            projection,
            selection,
            selectionArgs,
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
                    dateModifiedColumn = dateModifiedColumn
                )
                photos.add(photo)
                count++
            }
        }
        
        photos
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
        dateModifiedColumn: Int
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
            isSynced = true
        )
    }
    
    companion object {
        /**
         * Generate a unique ID for a virtual copy.
         */
        fun generateVirtualCopyId(): String = "vc_${UUID.randomUUID()}"
    }
}
