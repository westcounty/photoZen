package com.example.photozen.ai

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Status of AI analysis.
 */
data class AIAnalysisStatus(
    val isRunning: Boolean,
    val progress: Float,
    val currentCount: Int,
    val totalCount: Int,
    val totalAnalyzed: Int,
    val totalRemaining: Int,
    val state: WorkInfo.State?,
    val shouldContinue: Boolean = false
)

/**
 * Status of face embedding generation.
 */
data class FaceEmbeddingStatus(
    val isRunning: Boolean,
    val progress: Float,
    val currentCount: Int,
    val totalCount: Int,
    val totalProcessed: Int,
    val totalRemaining: Int,
    val state: WorkInfo.State?,
    val shouldContinue: Boolean = false
)

/**
 * Manager for AI analysis operations.
 * Provides a high-level interface to start, stop, and monitor AI analysis.
 */
@Singleton
class AIAnalysisManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Start AI analysis for all unanalyzed photos.
     * Uses chained work requests for continuous processing.
     */
    fun startAnalysis(batchSize: Int = 20) {
        val workRequest = AIAnalysisWorker.createWorkRequest(batchSize)
        
        workManager.enqueueUniqueWork(
            AIAnalysisWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP, // Don't restart if already running
            workRequest
        )
    }
    
    /**
     * Continue analysis with another batch (called when previous batch completes).
     */
    fun continueAnalysis(batchSize: Int = 20) {
        val workRequest = AIAnalysisWorker.createWorkRequest(batchSize)
        
        workManager.enqueueUniqueWork(
            AIAnalysisWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace to start new batch
            workRequest
        )
    }
    
    /**
     * Start continuous background analysis (periodic).
     */
    fun startContinuousAnalysis() {
        val workRequest = AIAnalysisWorker.createPeriodicWorkRequest()
        
        workManager.enqueueUniquePeriodicWork(
            "${AIAnalysisWorker.WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Stop all ongoing AI analysis.
     */
    fun stopAnalysis() {
        workManager.cancelUniqueWork(AIAnalysisWorker.WORK_NAME)
        workManager.cancelUniqueWork("${AIAnalysisWorker.WORK_NAME}_periodic")
    }
    
    /**
     * Get the current status of AI analysis as a Flow.
     */
    fun getAnalysisStatusFlow(): Flow<AIAnalysisStatus> {
        return workManager
            .getWorkInfosForUniqueWorkFlow(AIAnalysisWorker.WORK_NAME)
            .map { workInfos ->
                val workInfo = workInfos.firstOrNull()
                
                if (workInfo == null) {
                    AIAnalysisStatus(
                        isRunning = false,
                        progress = 0f,
                        currentCount = 0,
                        totalCount = 0,
                        totalAnalyzed = 0,
                        totalRemaining = 0,
                        state = null,
                        shouldContinue = false
                    )
                } else {
                    val progress = workInfo.progress
                    val outputData = workInfo.outputData
                    
                    // Check if work completed and should continue
                    val shouldContinue = workInfo.state == WorkInfo.State.SUCCEEDED &&
                            outputData.getBoolean("continue", false)
                    
                    AIAnalysisStatus(
                        isRunning = workInfo.state == WorkInfo.State.RUNNING,
                        progress = progress.getFloat(AIAnalysisWorker.KEY_PROGRESS, 0f),
                        currentCount = progress.getInt(AIAnalysisWorker.KEY_CURRENT, 0),
                        totalCount = progress.getInt(AIAnalysisWorker.KEY_TOTAL, 0),
                        totalAnalyzed = progress.getInt(AIAnalysisWorker.KEY_TOTAL_ANALYZED, 
                            outputData.getInt(AIAnalysisWorker.KEY_TOTAL_ANALYZED, 0)),
                        totalRemaining = progress.getInt(AIAnalysisWorker.KEY_TOTAL_REMAINING,
                            outputData.getInt(AIAnalysisWorker.KEY_TOTAL_REMAINING, 0)),
                        state = workInfo.state,
                        shouldContinue = shouldContinue
                    )
                }
            }
    }
    
    /**
     * Check if analysis is currently running.
     */
    suspend fun isAnalysisRunning(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(AIAnalysisWorker.WORK_NAME).get()
        return workInfos.any { it.state == WorkInfo.State.RUNNING }
    }
    
    // ==================== FACE EMBEDDING ====================
    
    /**
     * Start face embedding generation for all faces without embeddings.
     */
    fun startFaceEmbedding(batchSize: Int = 20) {
        val workRequest = FaceEmbeddingWorker.createWorkRequest(batchSize)
        
        workManager.enqueueUniqueWork(
            FaceEmbeddingWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * Continue face embedding with another batch.
     */
    fun continueFaceEmbedding(batchSize: Int = 20) {
        val workRequest = FaceEmbeddingWorker.createWorkRequest(batchSize)
        
        workManager.enqueueUniqueWork(
            FaceEmbeddingWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * Stop face embedding generation.
     */
    fun stopFaceEmbedding() {
        workManager.cancelUniqueWork(FaceEmbeddingWorker.WORK_NAME)
    }
    
    /**
     * Get the current status of face embedding as a Flow.
     */
    fun getFaceEmbeddingStatusFlow(): Flow<FaceEmbeddingStatus> {
        return workManager
            .getWorkInfosForUniqueWorkFlow(FaceEmbeddingWorker.WORK_NAME)
            .map { workInfos ->
                val workInfo = workInfos.firstOrNull()
                
                if (workInfo == null) {
                    FaceEmbeddingStatus(
                        isRunning = false,
                        progress = 0f,
                        currentCount = 0,
                        totalCount = 0,
                        totalProcessed = 0,
                        totalRemaining = 0,
                        state = null,
                        shouldContinue = false
                    )
                } else {
                    val progress = workInfo.progress
                    val outputData = workInfo.outputData
                    
                    val shouldContinue = workInfo.state == WorkInfo.State.SUCCEEDED &&
                            outputData.getBoolean("continue", false)
                    
                    FaceEmbeddingStatus(
                        isRunning = workInfo.state == WorkInfo.State.RUNNING,
                        progress = progress.getFloat(FaceEmbeddingWorker.KEY_PROGRESS, 0f),
                        currentCount = progress.getInt(FaceEmbeddingWorker.KEY_CURRENT, 0),
                        totalCount = progress.getInt(FaceEmbeddingWorker.KEY_TOTAL, 0),
                        totalProcessed = progress.getInt(FaceEmbeddingWorker.KEY_TOTAL_PROCESSED,
                            outputData.getInt(FaceEmbeddingWorker.KEY_TOTAL_PROCESSED, 0)),
                        totalRemaining = progress.getInt(FaceEmbeddingWorker.KEY_TOTAL_REMAINING,
                            outputData.getInt(FaceEmbeddingWorker.KEY_TOTAL_REMAINING, 0)),
                        state = workInfo.state,
                        shouldContinue = shouldContinue
                    )
                }
            }
    }
    
    /**
     * Check if face embedding is currently running.
     */
    suspend fun isFaceEmbeddingRunning(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(FaceEmbeddingWorker.WORK_NAME).get()
        return workInfos.any { it.state == WorkInfo.State.RUNNING }
    }
}
