package com.example.photozen.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.photozen.R
import com.example.photozen.data.local.dao.FaceDao
import com.example.photozen.data.local.dao.PhotoDao
import com.example.photozen.data.local.entity.FaceEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker for batch face embedding generation.
 * Processes faces without embeddings and generates 128-dim vectors using MobileFaceNet.
 */
@HiltWorker
class FaceEmbeddingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val faceDao: FaceDao,
    private val photoDao: PhotoDao,
    private val faceEmbedding: FaceEmbedding
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "face_embedding_work"
        const val KEY_BATCH_SIZE = "batch_size"
        const val KEY_PROGRESS = "progress"
        const val KEY_CURRENT = "current"
        const val KEY_TOTAL = "total"
        const val KEY_TOTAL_REMAINING = "total_remaining"
        const val KEY_TOTAL_PROCESSED = "total_processed"
        
        private const val DEFAULT_BATCH_SIZE = 20
        private const val NOTIFICATION_CHANNEL_ID = "face_embedding_channel"
        private const val NOTIFICATION_ID = 1002
        
        /**
         * Create a one-time work request for face embedding generation.
         */
        fun createWorkRequest(batchSize: Int = DEFAULT_BATCH_SIZE): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<FaceEmbeddingWorker>()
                .setInputData(workDataOf(KEY_BATCH_SIZE to batchSize))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
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
                "人脸特征提取",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台提取人脸特征的通知"
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
            .setContentTitle("正在提取人脸特征")
            .setContentText("已处理 $progress / $total")
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
            // Initialize the face embedding model
            if (!faceEmbedding.initialize()) {
                // Model not available, skip silently
                return@withContext Result.success(
                    workDataOf(
                        "error" to "模型未就绪，请确保 mobilefacenet.tflite 存在"
                    )
                )
            }
            
            val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)
            
            // Get faces without embeddings
            val facesWithoutEmbedding = getFacesWithoutEmbedding(batchSize)
            
            if (facesWithoutEmbedding.isEmpty()) {
                return@withContext Result.success(
                    workDataOf(
                        KEY_TOTAL_REMAINING to 0,
                        "complete" to true
                    )
                )
            }
            
            val totalRemaining = getTotalFacesWithoutEmbedding()
            val batchTotal = facesWithoutEmbedding.size
            var batchCurrent = 0
            var successCount = 0
            
            // Update foreground notification
            setForeground(getForegroundInfo())
            
            for (face in facesWithoutEmbedding) {
                // Check if cancelled
                if (isStopped) {
                    return@withContext Result.failure()
                }
                
                try {
                    // Get the photo for this face
                    val photo = photoDao.getById(face.photoId)
                    if (photo == null) {
                        batchCurrent++
                        continue
                    }
                    
                    // Parse bounding box from JSON
                    val boundingBox = parseBoundingBox(face.boundingBox)
                    if (boundingBox == null) {
                        batchCurrent++
                        continue
                    }
                    
                    // Generate embedding
                    val photoUri = Uri.parse(photo.systemUri)
                    val embedding = faceEmbedding.generateEmbedding(photoUri, boundingBox)
                    
                    if (embedding != null) {
                        // Update face with embedding
                        val embeddingBytes = faceEmbedding.embeddingToByteArray(embedding)
                        val updatedFace = face.copy(embedding = embeddingBytes)
                        faceDao.updateFace(updatedFace)
                        successCount++
                    }
                    
                } catch (e: Exception) {
                    // Log error but continue with next face
                    e.printStackTrace()
                }
                
                batchCurrent++
                
                // Update progress
                val overallProgress = if (totalRemaining > 0) {
                    batchCurrent.toFloat() / totalRemaining
                } else 0f
                
                setProgress(
                    workDataOf(
                        KEY_PROGRESS to overallProgress,
                        KEY_CURRENT to batchCurrent,
                        KEY_TOTAL to batchTotal,
                        KEY_TOTAL_REMAINING to (totalRemaining - batchCurrent),
                        KEY_TOTAL_PROCESSED to successCount
                    )
                )
                
                // Update notification
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.notify(
                    NOTIFICATION_ID,
                    createNotification(batchCurrent, totalRemaining)
                )
            }
            
            // Check if there are more faces to process
            val newRemainingCount = getTotalFacesWithoutEmbedding()
            
            Result.success(
                workDataOf(
                    KEY_TOTAL_REMAINING to newRemainingCount,
                    KEY_TOTAL_PROCESSED to successCount,
                    "continue" to (newRemainingCount > 0)
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
    
    /**
     * Get faces without embedding (limited by batch size).
     */
    private suspend fun getFacesWithoutEmbedding(limit: Int): List<FaceEntity> {
        return faceDao.getUnassignedFacesSync()
            .filter { it.embedding == null }
            .take(limit)
    }
    
    /**
     * Get total count of faces without embedding.
     */
    private suspend fun getTotalFacesWithoutEmbedding(): Int {
        return faceDao.getUnassignedFacesSync().count { it.embedding == null }
    }
    
    /**
     * Parse bounding box from JSON string.
     * Format: {"left": 0.1, "top": 0.2, "right": 0.5, "bottom": 0.6}
     */
    private fun parseBoundingBox(boundingBoxJson: String): RectF? {
        return try {
            val json = Json.parseToJsonElement(boundingBoxJson).jsonObject
            RectF(
                json["left"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return null,
                json["top"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return null,
                json["right"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return null,
                json["bottom"]?.jsonPrimitive?.content?.toFloatOrNull() ?: return null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
