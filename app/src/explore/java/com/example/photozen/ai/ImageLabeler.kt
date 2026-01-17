package com.example.photozen.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of image labeling.
 */
data class LabelResult(
    val labels: List<String>,
    val confidences: List<Float>,
    val primaryCategory: String?,
    val primaryConfidence: Float
)

/**
 * Wrapper around ML Kit Image Labeling for detecting objects and scenes.
 */
@Singleton
class ImageLabeler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f) // Only return labels with 60%+ confidence
            .build()
    )
    
    /**
     * Analyze an image from URI and return detected labels.
     */
    suspend fun analyzeImage(imageUri: Uri): LabelResult = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val labels: List<ImageLabel> = labeler.process(inputImage).await()
            
            val labelTexts = labels.map { label -> label.text }
            val confidences = labels.map { label -> label.confidence }
            
            // Determine primary category (highest confidence label)
            val primaryLabel = labels.maxByOrNull { label -> label.confidence }
            
            LabelResult(
                labels = labelTexts,
                confidences = confidences,
                primaryCategory = primaryLabel?.text,
                primaryConfidence = primaryLabel?.confidence ?: 0f
            )
        } catch (e: Exception) {
            LabelResult(
                labels = emptyList(),
                confidences = emptyList(),
                primaryCategory = null,
                primaryConfidence = 0f
            )
        }
    }
    
    /**
     * Analyze a bitmap and return detected labels.
     */
    suspend fun analyzeBitmap(bitmap: Bitmap): LabelResult = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val labels: List<ImageLabel> = labeler.process(inputImage).await()
            
            val labelTexts = labels.map { label -> label.text }
            val confidences = labels.map { label -> label.confidence }
            
            val primaryLabel = labels.maxByOrNull { label -> label.confidence }
            
            LabelResult(
                labels = labelTexts,
                confidences = confidences,
                primaryCategory = primaryLabel?.text,
                primaryConfidence = primaryLabel?.confidence ?: 0f
            )
        } catch (e: Exception) {
            LabelResult(
                labels = emptyList(),
                confidences = emptyList(),
                primaryCategory = null,
                primaryConfidence = 0f
            )
        }
    }
    
    /**
     * Close the labeler when no longer needed.
     */
    fun close() {
        labeler.close()
    }
}
