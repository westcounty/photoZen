package com.example.photozen.data.source

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
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
 * Result of a delete operation that may require user confirmation.
 */
sealed class DeleteResult {
    /** Deletion succeeded without user confirmation needed */
    data object Success : DeleteResult()
    
    /** User confirmation is required via IntentSender */
    data class RequiresConfirmation(val intentSender: IntentSender, val uris: List<Uri>) : DeleteResult()
    
    /** Deletion failed */
    data class Failed(val message: String) : DeleteResult()
}

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
     * Matches various camera album naming conventions across different devices:
     * - Standard: Camera, camera, CAMERA
     * - Chinese: 相机
     * - DCIM folder: DCIM
     * - Numbered folders: 100ANDRO, 100MEDIA, etc.
     * - Device-specific: OpenCamera, Gcam, etc.
     * NOTE: Screenshots are NOT included as camera photos
     */
    private fun isCameraAlbum(albumName: String): Boolean {
        val lowerName = albumName.lowercase()
        return lowerName == "camera" || 
               lowerName.contains("camera") ||
               albumName == "相机" ||
               albumName.contains("相机") ||
               lowerName == "dcim" ||
               lowerName.startsWith("100") ||  // 100ANDRO, 100MEDIA, etc.
               lowerName == "opencamera" ||
               lowerName == "gcam"
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
     * GPS coordinates are NOT read here for performance - they should be loaded lazily
     * when needed (e.g., when displaying photo details).
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
        
        // GPS coordinates are NOT loaded here for performance reasons.
        // Reading EXIF for every photo during scan would be extremely slow.
        // GPS data should be loaded lazily when displaying photo details.
        
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
            latitude = null,      // Loaded lazily when needed
            longitude = null,     // Loaded lazily when needed
            gpsScanned = false,   // Not scanned yet - will be done lazily
            isSynced = true
        )
    }
    
    /**
     * Extract GPS coordinates from EXIF data.
     * Returns a pair of (latitude, longitude), both can be null if not available.
     * 
     * This handles various GPS formats in EXIF:
     * - Standard GPS tags (TAG_GPS_LATITUDE, TAG_GPS_LONGITUDE)
     * - Reference tags (N/S for lat, E/W for lon)
     */
    private fun extractGpsFromExif(contentUri: Uri): Pair<Double?, Double?> {
        return try {
            contentResolver.openInputStream(contentUri)?.use { inputStream ->
                val exif = android.media.ExifInterface(inputStream)
                
                // Method 1: Use getLatLong (most reliable)
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    return Pair(latLong[0].toDouble(), latLong[1].toDouble())
                }
                
                // Method 2: Parse manually if getLatLong fails
                val latString = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE)
                val latRef = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE_REF)
                val lonString = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LONGITUDE)
                val lonRef = exif.getAttribute(android.media.ExifInterface.TAG_GPS_LONGITUDE_REF)
                
                if (latString != null && lonString != null) {
                    val lat = convertDmsToDecimal(latString, latRef)
                    val lon = convertDmsToDecimal(lonString, lonRef)
                    if (lat != null && lon != null) {
                        return Pair(lat, lon)
                    }
                }
                
                Pair(null, null)
            } ?: Pair(null, null)
        } catch (e: Exception) {
            // Silently fail - not all photos have GPS data
            Pair(null, null)
        }
    }
    
    /**
     * Convert DMS (Degrees/Minutes/Seconds) string to decimal degrees.
     * Format: "deg/1,min/1,sec/1" or "deg,min,sec"
     * 
     * @param dms The DMS string from EXIF
     * @param ref The reference (N/S for latitude, E/W for longitude)
     * @return Decimal degrees, or null if parsing fails
     */
    private fun convertDmsToDecimal(dms: String, ref: String?): Double? {
        return try {
            val parts = dms.split(",")
            if (parts.size != 3) return null
            
            fun parsePart(part: String): Double {
                val fraction = part.trim().split("/")
                return if (fraction.size == 2) {
                    fraction[0].toDouble() / fraction[1].toDouble()
                } else {
                    part.trim().toDouble()
                }
            }
            
            val degrees = parsePart(parts[0])
            val minutes = parsePart(parts[1])
            val seconds = parsePart(parts[2])
            
            var decimal = degrees + minutes / 60.0 + seconds / 3600.0
            
            // Apply reference direction
            if (ref == "S" || ref == "W") {
                decimal = -decimal
            }
            
            decimal
        } catch (e: Exception) {
            null
        }
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
     * Preserves all EXIF metadata including timestamps.
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
            // Get source file info including original timestamps
            var originalName = displayName
            var mimeType = "image/jpeg"
            var dateTaken: Long? = null
            var dateAdded: Long? = null
            var dateModified: Long? = null
            
            contentResolver.query(
                sourceUri,
                arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DATE_MODIFIED
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
                    dateTaken = cursor.getLongOrNull(2)
                    dateAdded = cursor.getLongOrNull(3)
                    dateModified = cursor.getLongOrNull(4)
                }
            }
            
            if (originalName == null) {
                originalName = "IMG_${System.currentTimeMillis()}.jpg"
            }
            
            // Create new entry in MediaStore with preserved timestamps
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, originalName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                // Preserve original timestamps
                dateAdded?.let { put(MediaStore.Images.Media.DATE_ADDED, it) }
                dateModified?.let { put(MediaStore.Images.Media.DATE_MODIFIED, it) }
                dateTaken?.let { put(MediaStore.Images.Media.DATE_TAKEN, it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, targetAlbumPath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val newUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: return@withContext null
            
            // Copy the file content with EXIF preservation
            try {
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    contentResolver.openOutputStream(newUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Copy EXIF data to preserve all metadata
                copyExifData(sourceUri, newUri)
                
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
     * Copy EXIF data from source to destination.
     * Preserves all metadata including timestamps, GPS, camera info, etc.
     */
    private fun copyExifData(sourceUri: Uri, destUri: Uri) {
        try {
            val sourceDescriptor = contentResolver.openFileDescriptor(sourceUri, "r") ?: return
            val destDescriptor = contentResolver.openFileDescriptor(destUri, "rw") ?: run {
                sourceDescriptor.close()
                return
            }
            
            val sourceExif = android.media.ExifInterface(sourceDescriptor.fileDescriptor)
            val destExif = android.media.ExifInterface(destDescriptor.fileDescriptor)
            
            // List of all EXIF tags to copy
            val exifTags = listOf(
                android.media.ExifInterface.TAG_DATETIME,
                android.media.ExifInterface.TAG_DATETIME_ORIGINAL,
                android.media.ExifInterface.TAG_DATETIME_DIGITIZED,
                android.media.ExifInterface.TAG_MAKE,
                android.media.ExifInterface.TAG_MODEL,
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.TAG_F_NUMBER,
                android.media.ExifInterface.TAG_EXPOSURE_TIME,
                android.media.ExifInterface.TAG_ISO_SPEED_RATINGS,
                android.media.ExifInterface.TAG_FOCAL_LENGTH,
                android.media.ExifInterface.TAG_GPS_LATITUDE,
                android.media.ExifInterface.TAG_GPS_LATITUDE_REF,
                android.media.ExifInterface.TAG_GPS_LONGITUDE,
                android.media.ExifInterface.TAG_GPS_LONGITUDE_REF,
                android.media.ExifInterface.TAG_GPS_ALTITUDE,
                android.media.ExifInterface.TAG_GPS_ALTITUDE_REF,
                android.media.ExifInterface.TAG_GPS_TIMESTAMP,
                android.media.ExifInterface.TAG_GPS_DATESTAMP,
                android.media.ExifInterface.TAG_WHITE_BALANCE,
                android.media.ExifInterface.TAG_FLASH,
                android.media.ExifInterface.TAG_IMAGE_WIDTH,
                android.media.ExifInterface.TAG_IMAGE_LENGTH,
                android.media.ExifInterface.TAG_SOFTWARE,
                android.media.ExifInterface.TAG_ARTIST,
                android.media.ExifInterface.TAG_COPYRIGHT,
                android.media.ExifInterface.TAG_USER_COMMENT,
                android.media.ExifInterface.TAG_SUBSEC_TIME,
                android.media.ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                android.media.ExifInterface.TAG_SUBSEC_TIME_DIGITIZED
            )
            
            for (tag in exifTags) {
                sourceExif.getAttribute(tag)?.let { value ->
                    destExif.setAttribute(tag, value)
                }
            }
            
            destExif.saveAttributes()
            
            sourceDescriptor.close()
            destDescriptor.close()
        } catch (e: Exception) {
            // Silently fail - photo is copied, just without EXIF
        }
    }
    
    /**
     * Helper extension to safely get Long from cursor.
     */
    private fun Cursor.getLongOrNull(columnIndex: Int): Long? {
        return if (isNull(columnIndex)) null else getLong(columnIndex)
    }
    
    /**
     * Result of a move operation.
     */
    sealed class MoveResult {
        /** Move succeeded */
        data class Success(val newPhoto: PhotoEntity) : MoveResult()
        
        /** Move requires user confirmation to delete original (Android 11+) */
        data class RequiresDeleteConfirmation(
            val newPhoto: PhotoEntity,
            val intentSender: IntentSender,
            val originalUri: Uri
        ) : MoveResult()
        
        /** Move failed */
        data object Failed : MoveResult()
    }
    
    /**
     * Move a photo to a target album.
     * On Android 10+, first attempts direct path modification (requires write permission).
     * If that fails, falls back to copy-then-delete approach.
     * 
     * @return MoveResult indicating success, need for delete confirmation, or failure
     */
    suspend fun movePhotoToAlbum(
        sourceUri: Uri,
        targetAlbumPath: String,
        displayName: String? = null
    ): PhotoEntity? = withContext(Dispatchers.IO) {
        // Try direct move first on Android Q+ by updating RELATIVE_PATH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val directMoveResult = tryDirectMove(sourceUri, targetAlbumPath)
            if (directMoveResult != null) {
                return@withContext directMoveResult
            }
        }
        
        // Fall back to copy-then-delete approach
        val newPhoto = copyPhotoToAlbum(sourceUri, targetAlbumPath, displayName)
        if (newPhoto != null) {
            // Try to delete original
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // On Android 11+, deletion may require user confirmation
                    // We attempt direct delete; if it fails, the copy is still valid
                    contentResolver.delete(sourceUri, null, null)
                } else {
                    contentResolver.delete(sourceUri, null, null)
                }
            } catch (e: SecurityException) {
                // On Android 11+, deletion of other apps' files requires user consent
                // The copy succeeded, so the move is partially complete
            } catch (e: Exception) {
                // Ignore other deletion errors - at least the copy succeeded
            }
        }
        newPhoto
    }
    
    /**
     * Move a photo with explicit handling of delete confirmation requirement.
     * Returns MoveResult which may require user confirmation for deletion.
     */
    suspend fun movePhotoToAlbumWithConfirmation(
        sourceUri: Uri,
        targetAlbumPath: String,
        displayName: String? = null
    ): MoveResult = withContext(Dispatchers.IO) {
        // Try direct move first on Android Q+ by updating RELATIVE_PATH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val directMoveResult = tryDirectMove(sourceUri, targetAlbumPath)
            if (directMoveResult != null) {
                return@withContext MoveResult.Success(directMoveResult)
            }
        }
        
        // Fall back to copy-then-delete approach
        val newPhoto = copyPhotoToAlbum(sourceUri, targetAlbumPath, displayName)
            ?: return@withContext MoveResult.Failed
        
        // Try to delete original
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intentSender = MediaStore.createDeleteRequest(
                    contentResolver, 
                    listOf(sourceUri)
                ).intentSender
                return@withContext MoveResult.RequiresDeleteConfirmation(newPhoto, intentSender, sourceUri)
            } catch (e: Exception) {
                // If we can't create delete request, just return success with copy
                return@withContext MoveResult.Success(newPhoto)
            }
        } else {
            try {
                contentResolver.delete(sourceUri, null, null)
            } catch (e: Exception) {
                // Ignore deletion errors
            }
            return@withContext MoveResult.Success(newPhoto)
        }
    }
    
    /**
     * Request write permission for multiple photos using createWriteRequest.
     * Returns IntentSender if user confirmation needed, null if not needed or not supported.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    suspend fun requestWritePermission(uris: List<Uri>): IntentSender? = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext null
        try {
            MediaStore.createWriteRequest(contentResolver, uris).intentSender
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Move photo after write permission has been granted.
     * This should succeed if write permission was granted via createWriteRequest.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    suspend fun movePhotoAfterPermission(
        sourceUri: Uri,
        targetAlbumPath: String
    ): PhotoEntity? = withContext(Dispatchers.IO) {
        tryDirectMove(sourceUri, targetAlbumPath)
    }
    
    /**
     * Batch move photos to album. For Android 11+, this may return a write permission request.
     * 
     * @return BatchMoveResult with either success info or pending permission request
     */
    suspend fun batchMovePhotosToAlbum(
        photos: List<Pair<Uri, String>>, // (sourceUri, photoId)
        targetAlbumPath: String
    ): BatchMoveResult = withContext(Dispatchers.IO) {
        val movedPhotos = mutableListOf<Pair<String, PhotoEntity>>() // (originalId, newPhoto)
        val needsPermissionUris = mutableListOf<Pair<Uri, String>>() // (uri, photoId)
        
        // First pass: try direct move for each photo
        for ((sourceUri, photoId) in photos) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val directMoveResult = tryDirectMove(sourceUri, targetAlbumPath)
                if (directMoveResult != null) {
                    movedPhotos.add(photoId to directMoveResult)
                } else {
                    // This photo needs write permission
                    needsPermissionUris.add(sourceUri to photoId)
                }
            } else {
                // Pre-Android 10: try direct move
                val directMoveResult = tryDirectMovePreQ(sourceUri, targetAlbumPath)
                if (directMoveResult != null) {
                    movedPhotos.add(photoId to directMoveResult)
                }
            }
        }
        
        // If all photos were moved successfully, return success
        if (needsPermissionUris.isEmpty()) {
            return@withContext BatchMoveResult.Success(movedPhotos)
        }
        
        // On Android 11+, request write permission for remaining photos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val uris = needsPermissionUris.map { it.first }
                val intentSender = MediaStore.createWriteRequest(contentResolver, uris).intentSender
                return@withContext BatchMoveResult.RequiresWritePermission(
                    alreadyMoved = movedPhotos,
                    pendingUris = needsPermissionUris,
                    intentSender = intentSender,
                    targetAlbumPath = targetAlbumPath
                )
            } catch (e: Exception) {
                // Fall through to copy+delete
            }
        }
        
        // Fall back to copy+delete for remaining photos
        val copiedPhotos = mutableListOf<Triple<String, PhotoEntity, Uri>>() // (originalId, newPhoto, originalUri)
        for ((sourceUri, photoId) in needsPermissionUris) {
            val newPhoto = copyPhotoToAlbum(sourceUri, targetAlbumPath)
            if (newPhoto != null) {
                copiedPhotos.add(Triple(photoId, newPhoto, sourceUri))
            }
        }
        
        // Request delete permission for originals
        if (copiedPhotos.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val deleteUris = copiedPhotos.map { it.third }
                val intentSender = MediaStore.createDeleteRequest(contentResolver, deleteUris).intentSender
                return@withContext BatchMoveResult.RequiresDeletePermission(
                    alreadyMoved = movedPhotos,
                    copiedPhotos = copiedPhotos.map { it.first to it.second },
                    pendingDeleteUris = deleteUris,
                    photoIdsToCleanup = copiedPhotos.map { it.first },
                    intentSender = intentSender
                )
            } catch (e: Exception) {
                // Just return what we have
            }
        }
        
        // Pre-Android 11 or failed to create delete request: try direct delete
        for ((_, _, originalUri) in copiedPhotos) {
            try {
                contentResolver.delete(originalUri, null, null)
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        // Combine moved and copied photos
        val allMoved = movedPhotos + copiedPhotos.map { it.first to it.second }
        return@withContext BatchMoveResult.Success(allMoved)
    }
    
    /**
     * Complete batch move after write permission was granted.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    suspend fun completeBatchMoveAfterPermission(
        pendingUris: List<Pair<Uri, String>>,
        targetAlbumPath: String
    ): List<Pair<String, PhotoEntity>> = withContext(Dispatchers.IO) {
        val movedPhotos = mutableListOf<Pair<String, PhotoEntity>>()
        for ((sourceUri, photoId) in pendingUris) {
            val result = tryDirectMove(sourceUri, targetAlbumPath)
            if (result != null) {
                movedPhotos.add(photoId to result)
            }
        }
        movedPhotos
    }
    
    /**
     * Try direct move for pre-Android Q using file system operations.
     */
    private suspend fun tryDirectMovePreQ(sourceUri: Uri, targetAlbumPath: String): PhotoEntity? {
        // Pre-Android Q doesn't support RELATIVE_PATH, so we can't do direct moves easily
        // Return null to trigger copy+delete fallback
        return null
    }
    
    /**
     * Result of batch move operation.
     */
    sealed class BatchMoveResult {
        /** All photos moved successfully */
        data class Success(
            val movedPhotos: List<Pair<String, PhotoEntity>> // (originalId, newPhoto)
        ) : BatchMoveResult()
        
        /** Need write permission to move remaining photos */
        data class RequiresWritePermission(
            val alreadyMoved: List<Pair<String, PhotoEntity>>,
            val pendingUris: List<Pair<Uri, String>>, // (uri, photoId)
            val intentSender: IntentSender,
            val targetAlbumPath: String
        ) : BatchMoveResult()
        
        /** Photos were copied, need delete permission for originals */
        data class RequiresDeletePermission(
            val alreadyMoved: List<Pair<String, PhotoEntity>>,
            val copiedPhotos: List<Pair<String, PhotoEntity>>, // (originalId, newPhoto)
            val pendingDeleteUris: List<Uri>,
            val photoIdsToCleanup: List<String>,
            val intentSender: IntentSender
        ) : BatchMoveResult()
    }
    
    /**
     * Try to move photo by directly updating its RELATIVE_PATH.
     * This is more efficient than copy-delete but requires write permission.
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun tryDirectMove(sourceUri: Uri, targetAlbumPath: String): PhotoEntity? {
        return try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.RELATIVE_PATH, targetAlbumPath)
            }
            
            val rowsUpdated = contentResolver.update(sourceUri, values, null, null)
            if (rowsUpdated > 0) {
                // Successfully moved - fetch updated entity
                val mediaStoreId = ContentUris.parseId(sourceUri)
                fetchPhotoById(mediaStoreId)
            } else {
                null
            }
        } catch (e: SecurityException) {
            // Need write permission - fall back to copy-delete
            null
        } catch (e: Exception) {
            null
        }
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
     * Get the relative path for an existing album by bucket ID.
     * Returns the RELATIVE_PATH that can be used for copying/moving photos to this album.
     */
    suspend fun getAlbumPath(bucketId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Query any photo in this album to get its RELATIVE_PATH
            contentResolver.query(
                imageCollection,
                arrayOf(MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.BUCKET_DISPLAY_NAME),
                "${MediaStore.Images.Media.BUCKET_ID} = ?",
                arrayOf(bucketId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                    val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    
                    if (pathColumn >= 0) {
                        val path = cursor.getString(pathColumn)
                        if (!path.isNullOrBlank()) {
                            return@withContext path.trimEnd('/')
                        }
                    }
                    
                    // Fallback: construct path from bucket name
                    if (nameColumn >= 0) {
                        val albumName = cursor.getString(nameColumn)
                        if (!albumName.isNullOrBlank()) {
                            return@withContext "${android.os.Environment.DIRECTORY_PICTURES}/$albumName"
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Delete an album and all its photos.
     * On Android 10+, this may require user confirmation.
     * 
     * @return DeleteResult indicating success, failure, or need for user confirmation
     */
    suspend fun deleteAlbum(bucketId: String): DeleteResult = withContext(Dispatchers.IO) {
        try {
            // Get all photo URIs in the album
            val uris = mutableListOf<Uri>()
            
            contentResolver.query(
                imageCollection,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.BUCKET_ID} = ?",
                arrayOf(bucketId),
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    uris.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
                }
            }
            
            if (uris.isEmpty()) {
                return@withContext DeleteResult.Success
            }
            
            // On Android 11+, use createDeleteRequest for user confirmation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = MediaStore.createDeleteRequest(contentResolver, uris).intentSender
                DeleteResult.RequiresConfirmation(intentSender, uris)
            } else {
                // On older versions, try direct deletion
                var deleted = 0
                uris.forEach { uri ->
                    try {
                        deleted += contentResolver.delete(uri, null, null)
                    } catch (e: Exception) {
                        // Ignore individual deletion errors
                    }
                }
                if (deleted > 0) DeleteResult.Success else DeleteResult.Failed("无法删除照片")
            }
        } catch (e: Exception) {
            DeleteResult.Failed("删除相册失败: ${e.message}")
        }
    }
    
    /**
     * Delete specific photos by their URIs.
     * On Android 10+, this may require user confirmation.
     * 
     * @return DeleteResult indicating success, failure, or need for user confirmation
     */
    suspend fun deletePhotos(uris: List<Uri>): DeleteResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext DeleteResult.Success
        }
        
        try {
            // On Android 11+, use createDeleteRequest for user confirmation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = MediaStore.createDeleteRequest(contentResolver, uris).intentSender
                DeleteResult.RequiresConfirmation(intentSender, uris)
            } else {
                // On older versions, try direct deletion
                var deleted = 0
                uris.forEach { uri ->
                    try {
                        deleted += contentResolver.delete(uri, null, null)
                    } catch (e: Exception) {
                        // Ignore individual deletion errors
                    }
                }
                if (deleted > 0) DeleteResult.Success else DeleteResult.Failed("无法删除照片")
            }
        } catch (e: Exception) {
            DeleteResult.Failed("删除照片失败: ${e.message}")
        }
    }
    
    /**
     * Copy a photo to the same album, preserving all EXIF data and timestamps.
     * Creates a duplicate of the photo with a unique filename.
     * CRITICAL: Must preserve DATE_TAKEN, DATE_ADDED, DATE_MODIFIED for proper sorting.
     * 
     * @param sourceUri Source content URI of the photo
     * @return New PhotoEntity with updated systemUri and id, or null if failed
     */
    suspend fun duplicatePhoto(sourceUri: Uri): PhotoEntity? = withContext(Dispatchers.IO) {
        try {
            // Get source file info including original timestamps and path
            var originalName: String? = null
            var mimeType = "image/jpeg"
            var dateTaken: Long? = null
            var dateAdded: Long? = null
            var dateModified: Long? = null
            var relativePath: String? = null
            
            contentResolver.query(
                sourceUri,
                arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.RELATIVE_PATH
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    originalName = cursor.getString(0)
                    mimeType = cursor.getString(1) ?: "image/jpeg"
                    dateTaken = cursor.getLongOrNull(2)
                    dateAdded = cursor.getLongOrNull(3)
                    dateModified = cursor.getLongOrNull(4)
                    relativePath = cursor.getString(5)
                }
            }
            
            if (originalName == null) {
                originalName = "IMG_${System.currentTimeMillis()}.jpg"
            }
            
            // Generate unique filename - use minimal suffix to keep similar name
            val baseName = originalName!!.substringBeforeLast(".")
            val extension = originalName!!.substringAfterLast(".", "jpg")
            val copyNum = (System.currentTimeMillis() % 10000).toInt()
            val newName = "${baseName}_$copyNum.$extension"
            
            // Use same path or default to Pictures
            val targetPath = relativePath ?: "${android.os.Environment.DIRECTORY_PICTURES}/PicZen"
            
            // Create new entry in MediaStore
            // Note: DATE_ADDED and DATE_MODIFIED are initially set by system,
            // but we can update them after insertion on Android Q+
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                // Set DATE_TAKEN in initial insert
                dateTaken?.let { put(MediaStore.Images.Media.DATE_TAKEN, it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, targetPath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val newUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: return@withContext null
            
            // Copy the file content with EXIF preservation
            try {
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    contentResolver.openOutputStream(newUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Copy EXIF data to preserve all metadata including datetime
                copyExifData(sourceUri, newUri)
                
                // Update timestamps AFTER file copy to preserve original times
                // This is critical for proper sorting
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val updateValues = android.content.ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                        // Update DATE_ADDED and DATE_MODIFIED to match original
                        dateAdded?.let { put(MediaStore.Images.Media.DATE_ADDED, it) }
                        dateModified?.let { put(MediaStore.Images.Media.DATE_MODIFIED, it) }
                        dateTaken?.let { put(MediaStore.Images.Media.DATE_TAKEN, it) }
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
    
    companion object {
        /**
         * Generate a unique ID for a virtual copy.
         */
        fun generateVirtualCopyId(): String = "vc_${UUID.randomUUID()}"
    }
}
