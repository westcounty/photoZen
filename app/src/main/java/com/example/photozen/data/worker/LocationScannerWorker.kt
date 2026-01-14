package com.example.photozen.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.photozen.data.local.dao.PhotoDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker that scans photos for GPS EXIF data.
 * 
 * Runs in the background to extract GPS coordinates from photo metadata
 * and updates the database with latitude/longitude values.
 * 
 * Features:
 * - Batch processing to avoid memory issues
 * - Progress reporting
 * - Graceful error handling per photo
 */
@HiltWorker
class LocationScannerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoDao: PhotoDao
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val TAG = "LocationScannerWorker"
        const val WORK_NAME = "location_scanner_work"
        
        // Progress data keys
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val KEY_SCANNED = "scanned"
        const val KEY_WITH_GPS = "with_gps"
        
        // Batch size for processing
        private const val BATCH_SIZE = 50
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting GPS location scan...")
        
        var totalScanned = 0
        var totalWithGps = 0
        
        try {
            // Get total count first for progress calculation
            val totalPending = photoDao.getPendingGpsScanCountSync()
            Log.d(TAG, "Total photos to scan: $totalPending")
            
            // Report initial progress with total
            setProgress(workDataOf(
                KEY_TOTAL to totalPending,
                KEY_SCANNED to 0,
                KEY_WITH_GPS to 0,
                KEY_PROGRESS to 0
            ))
            
            // Process in batches to avoid memory issues
            while (true) {
                val photosToScan = photoDao.getPhotosNeedingGpsScan(BATCH_SIZE)
                
                if (photosToScan.isEmpty()) {
                    Log.d(TAG, "No more photos to scan")
                    break
                }
                
                Log.d(TAG, "Processing batch of ${photosToScan.size} photos")
                
                for (photo in photosToScan) {
                    try {
                        val gpsData = extractGpsFromUri(photo.systemUri)
                        
                        if (gpsData != null) {
                            photoDao.updateGpsLocation(
                                photoId = photo.id,
                                latitude = gpsData.first,
                                longitude = gpsData.second
                            )
                            totalWithGps++
                            Log.d(TAG, "Found GPS for ${photo.displayName}: ${gpsData.first}, ${gpsData.second}")
                        } else {
                            // Mark as scanned even if no GPS data found
                            photoDao.markGpsScanned(photo.id)
                        }
                        
                        totalScanned++
                        
                        // Report progress with percentage
                        val progress = if (totalPending > 0) {
                            (totalScanned * 100) / totalPending
                        } else 0
                        
                        setProgress(workDataOf(
                            KEY_TOTAL to totalPending,
                            KEY_SCANNED to totalScanned,
                            KEY_WITH_GPS to totalWithGps,
                            KEY_PROGRESS to progress
                        ))
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error extracting GPS from ${photo.displayName}: ${e.message}")
                        // Mark as scanned to avoid retrying failed photos
                        try {
                            photoDao.markGpsScanned(photo.id)
                        } catch (dbError: Exception) {
                            Log.e(TAG, "Error marking photo as scanned: ${dbError.message}")
                        }
                        totalScanned++
                    }
                }
                
                // Check if we should stop (e.g., worker cancelled)
                if (isStopped) {
                    Log.d(TAG, "Worker stopped by user, progress saved. Scanned: $totalScanned")
                    // Return success so that progress is preserved
                    return@withContext Result.success(workDataOf(
                        KEY_TOTAL to totalPending,
                        KEY_SCANNED to totalScanned,
                        KEY_WITH_GPS to totalWithGps,
                        KEY_PROGRESS to if (totalPending > 0) (totalScanned * 100) / totalPending else 100
                    ))
                }
            }
            
            Log.d(TAG, "GPS scan complete. Scanned: $totalScanned, With GPS: $totalWithGps")
            
            Result.success(workDataOf(
                KEY_TOTAL to totalPending,
                KEY_SCANNED to totalScanned,
                KEY_WITH_GPS to totalWithGps,
                KEY_PROGRESS to 100
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "GPS scan failed: ${e.message}", e)
            Result.failure(workDataOf(
                KEY_SCANNED to totalScanned,
                KEY_WITH_GPS to totalWithGps
            ))
        }
    }
    
    /**
     * Extract GPS coordinates from photo URI using ExifInterface.
     * 
     * @param uriString The content URI string of the photo
     * @return Pair of (latitude, longitude) or null if no GPS data
     */
    private fun extractGpsFromUri(uriString: String): Pair<Double, Double>? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                
                // Get GPS coordinates using latLong property (non-deprecated way)
                val latLong = exif.latLong
                if (latLong != null && latLong.size == 2) {
                    Pair(latLong[0], latLong[1])
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read EXIF from $uriString: ${e.message}")
            null
        }
    }
}
