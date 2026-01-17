package com.example.photozen.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detected face information.
 */
data class DetectedFace(
    val id: String,
    val boundingBox: RectF,
    val normalizedBoundingBox: RectF, // Normalized to 0.0 - 1.0
    val confidence: Float,
    val landmarks: Map<String, Pair<Float, Float>>? = null
)

/**
 * Result of face detection.
 */
data class FaceDetectionResult(
    val faces: List<DetectedFace>,
    val imageWidth: Int,
    val imageHeight: Int
)

/**
 * Wrapper around ML Kit Face Detection.
 */
@Singleton
class FaceDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f) // Detect faces at least 10% of image size
            .enableTracking() // Enable face tracking for consistency
            .build()
    )
    
    /**
     * Detect faces in an image from URI.
     */
    suspend fun detectFaces(imageUri: Uri): FaceDetectionResult = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val faces: List<Face> = detector.process(inputImage).await()
            
            val imageWidth = inputImage.width
            val imageHeight = inputImage.height
            
            val detectedFaces = faces.mapIndexed { index: Int, face: Face ->
                convertToDetectedFace(face, index, imageWidth, imageHeight)
            }
            
            FaceDetectionResult(
                faces = detectedFaces,
                imageWidth = imageWidth,
                imageHeight = imageHeight
            )
        } catch (e: Exception) {
            FaceDetectionResult(
                faces = emptyList(),
                imageWidth = 0,
                imageHeight = 0
            )
        }
    }
    
    /**
     * Detect faces in a bitmap.
     */
    suspend fun detectFaces(bitmap: Bitmap): FaceDetectionResult = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces: List<Face> = detector.process(inputImage).await()
            
            val imageWidth = bitmap.width
            val imageHeight = bitmap.height
            
            val detectedFaces = faces.mapIndexed { index: Int, face: Face ->
                convertToDetectedFace(face, index, imageWidth, imageHeight)
            }
            
            FaceDetectionResult(
                faces = detectedFaces,
                imageWidth = imageWidth,
                imageHeight = imageHeight
            )
        } catch (e: Exception) {
            FaceDetectionResult(
                faces = emptyList(),
                imageWidth = 0,
                imageHeight = 0
            )
        }
    }
    
    /**
     * Convert ML Kit Face to our DetectedFace model.
     */
    private fun convertToDetectedFace(
        face: Face,
        index: Int,
        imageWidth: Int,
        imageHeight: Int
    ): DetectedFace {
        val bounds = face.boundingBox
        val absoluteBox = RectF(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat()
        )
        
        // Normalize bounding box to 0.0 - 1.0 range
        val normalizedBox = RectF(
            bounds.left.toFloat() / imageWidth,
            bounds.top.toFloat() / imageHeight,
            bounds.right.toFloat() / imageWidth,
            bounds.bottom.toFloat() / imageHeight
        )
        
        // Extract landmarks if available
        val landmarks = mutableMapOf<String, Pair<Float, Float>>()
        face.getLandmark(FaceLandmark.LEFT_EYE)?.let { landmark ->
            landmarks["leftEye"] = Pair(landmark.position.x / imageWidth, landmark.position.y / imageHeight)
        }
        face.getLandmark(FaceLandmark.RIGHT_EYE)?.let { landmark ->
            landmarks["rightEye"] = Pair(landmark.position.x / imageWidth, landmark.position.y / imageHeight)
        }
        face.getLandmark(FaceLandmark.NOSE_BASE)?.let { landmark ->
            landmarks["nose"] = Pair(landmark.position.x / imageWidth, landmark.position.y / imageHeight)
        }
        
        return DetectedFace(
            id = "${System.currentTimeMillis()}_$index",
            boundingBox = absoluteBox,
            normalizedBoundingBox = normalizedBox,
            confidence = face.trackingId?.let { 1.0f } ?: 0.8f, // Tracking ID indicates high confidence
            landmarks = landmarks.ifEmpty { null }
        )
    }
    
    /**
     * Close the detector when no longer needed.
     */
    fun close() {
        detector.close()
    }
}
