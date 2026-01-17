package com.example.photozen.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.photozen.R
import com.example.photozen.data.local.dao.FaceDao
import com.example.photozen.data.local.dao.PhotoAnalysisDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.dao.PhotoLabelDao
import com.example.photozen.data.local.entity.FaceEntity
import com.example.photozen.data.local.entity.PhotoAnalysisEntity
import com.example.photozen.data.local.entity.PhotoLabelEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker for background AI analysis of photos.
 * Analyzes photos in batches to detect labels, faces, and extract metadata.
 */
@HiltWorker
class AIAnalysisWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val photoDao: PhotoDao,
    private val photoAnalysisDao: PhotoAnalysisDao,
    private val faceDao: FaceDao,
    private val photoLabelDao: PhotoLabelDao,
    private val imageLabeler: ImageLabeler,
    private val faceDetector: FaceDetector,
    private val imageEmbedding: ImageEmbedding
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "ai_analysis_work"
        const val KEY_BATCH_SIZE = "batch_size"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val KEY_CURRENT = "current"
        const val KEY_TOTAL_REMAINING = "total_remaining"
        const val KEY_TOTAL_ANALYZED = "total_analyzed"
        
        private const val DEFAULT_BATCH_SIZE = 20
        private const val NOTIFICATION_CHANNEL_ID = "ai_analysis_channel"
        private const val NOTIFICATION_ID = 1001
        
        /**
         * Create a one-time work request for AI analysis.
         */
        fun createWorkRequest(batchSize: Int = DEFAULT_BATCH_SIZE): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<AIAnalysisWorker>()
                .setInputData(
                    workDataOf(KEY_BATCH_SIZE to batchSize)
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true) // Don't run when battery is low
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        }
        
        /**
         * Create a periodic work request for continuous analysis.
         */
        fun createPeriodicWorkRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<AIAnalysisWorker>(
                1, TimeUnit.HOURS // Run every hour
            )
                .setInputData(
                    workDataOf(KEY_BATCH_SIZE to DEFAULT_BATCH_SIZE)
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresCharging(true) // Only when charging
                        .build()
                )
                .build()
        }
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "AI 照片分析",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台分析照片的通知"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create foreground notification
     */
    private fun createNotification(progress: Int, total: Int): android.app.Notification {
        createNotificationChannel()
        
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("正在分析照片")
            .setContentText("已分析 $progress / $total 张")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(total, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val notification = createNotification(0, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)
            
            // Get total counts for progress tracking
            val totalRemaining = photoAnalysisDao.getUnanalyzedCount()
            val totalAnalyzed = photoAnalysisDao.getAnalyzedCount()
            val grandTotal = totalRemaining + totalAnalyzed
            
            // Get unanalyzed photo IDs for this batch
            val unanalyzedIds = photoAnalysisDao.getUnanalyzedPhotoIds(batchSize)
            
            if (unanalyzedIds.isEmpty()) {
                return@withContext Result.success(
                    workDataOf(
                        KEY_TOTAL_REMAINING to 0,
                        KEY_TOTAL_ANALYZED to totalAnalyzed,
                        "complete" to true
                    )
                )
            }
            
            val batchTotal = unanalyzedIds.size
            var batchCurrent = 0
            
            // Update foreground notification
            setForeground(getForegroundInfo())
            
            for (photoId in unanalyzedIds) {
                // Check if cancelled
                if (isStopped) {
                    return@withContext Result.failure()
                }
                
                // Get photo details
                val photo = photoDao.getById(photoId)
                if (photo == null) {
                    batchCurrent++
                    continue
                }
                
                try {
                    val uri = Uri.parse(photo.systemUri)
                    
                    // Analyze image labels
                    val labelResult = imageLabeler.analyzeImage(uri)
                    
                    // Detect faces
                    val faceResult = faceDetector.detectFaces(uri)
                    
                    // Generate image embedding for similarity search
                    val embeddingArray = try {
                        imageEmbedding.generateEmbedding(uri)
                    } catch (e: Exception) {
                        // Embedding generation is optional - continue if it fails
                        null
                    }
                    val embeddingBytes = embeddingArray?.let { 
                        imageEmbedding.embeddingToByteArray(it) 
                    }
                    
                    // Create analysis entity
                    val analysisEntity = PhotoAnalysisEntity(
                        photoId = photoId,
                        labels = Json.encodeToString(labelResult.labels),
                        embedding = embeddingBytes,
                        analyzedAt = System.currentTimeMillis(),
                        hasGps = photo.latitude != null && photo.longitude != null,
                        latitude = photo.latitude,
                        longitude = photo.longitude,
                        faceCount = faceResult.faces.size,
                        primaryCategory = labelResult.primaryCategory,
                        primaryCategoryConfidence = labelResult.primaryConfidence
                    )
                    
                    // Save analysis result
                    photoAnalysisDao.insert(analysisEntity)
                    
                    // Save labels to association table for fast aggregation queries
                    if (labelResult.labels.isNotEmpty()) {
                        val labelEntities = labelResult.labels.mapIndexed { index, label ->
                            PhotoLabelEntity(
                                photoId = photoId,
                                label = label.lowercase(), // Normalize to lowercase
                                confidence = labelResult.confidences.getOrElse(index) { 0f }
                            )
                        }
                        photoLabelDao.insertAll(labelEntities)
                    }
                    
                    // Save detected faces
                    if (faceResult.faces.isNotEmpty()) {
                        val faceEntities = faceResult.faces.map { face ->
                            FaceEntity(
                                id = UUID.randomUUID().toString(),
                                photoId = photoId,
                                boundingBox = Json.encodeToString(
                                    mapOf(
                                        "left" to face.normalizedBoundingBox.left,
                                        "top" to face.normalizedBoundingBox.top,
                                        "right" to face.normalizedBoundingBox.right,
                                        "bottom" to face.normalizedBoundingBox.bottom
                                    )
                                ),
                                embedding = null, // TODO: Generate face embedding
                                personId = null,
                                confidence = face.confidence,
                                detectedAt = System.currentTimeMillis()
                            )
                        }
                        faceDao.insertFaces(faceEntities)
                    }
                    
                } catch (e: Exception) {
                    // Log error but continue with next photo
                    e.printStackTrace()
                }
                
                batchCurrent++
                
                // Calculate overall progress
                val currentAnalyzed = totalAnalyzed + batchCurrent
                val overallProgress = if (grandTotal > 0) currentAnalyzed.toFloat() / grandTotal else 0f
                
                // Update progress
                setProgress(
                    workDataOf(
                        KEY_PROGRESS to overallProgress,
                        KEY_CURRENT to batchCurrent,
                        KEY_TOTAL to batchTotal,
                        KEY_TOTAL_REMAINING to (totalRemaining - batchCurrent),
                        KEY_TOTAL_ANALYZED to currentAnalyzed
                    )
                )
                
                // Update notification
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.notify(
                    NOTIFICATION_ID,
                    createNotification(currentAnalyzed, grandTotal)
                )
            }
            
            // Check if there are more photos to analyze
            val newRemainingCount = photoAnalysisDao.getUnanalyzedCount()
            val newAnalyzedCount = photoAnalysisDao.getAnalyzedCount()
            
            Result.success(
                workDataOf(
                    KEY_TOTAL_REMAINING to newRemainingCount,
                    KEY_TOTAL_ANALYZED to newAnalyzedCount,
                    "continue" to (newRemainingCount > 0)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
