package com.example.photozen.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Face embedding generator using ArcFace TFLite model.
 * Generates 512-dimensional embedding vectors for face recognition and clustering.
 * 
 * ArcFace (Additive Angular Margin Loss) provides higher accuracy than MobileFaceNet,
 * especially for:
 * - Asian faces
 * - Children
 * - Challenging angles and lighting conditions
 * 
 * Reference: https://github.com/mobilesec/arcface-tensorflowlite
 * Aligned with PhotoPrism's approach of using 512-dim embeddings.
 */
@Singleton
class FaceEmbedding @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MODEL_FILE = "arcface.tflite"
        private const val INPUT_SIZE = 112 // ArcFace standard input size
        const val EMBEDDING_SIZE = 512 // ArcFace uses 512-dim embeddings (like PhotoPrism's FaceNet)
        private const val PIXEL_MEAN = 127.5f
        private const val PIXEL_STD = 128f
        private const val MIN_MODEL_SIZE = 500_000L // 500KB minimum for valid model
        
        // 兼容旧模型的配置
        private const val LEGACY_MODEL_FILE = "mobilefacenet.tflite"
        private const val LEGACY_EMBEDDING_SIZE = 128
    }
    
    // 当前使用的嵌入维度（支持新旧模型自动切换）
    var currentEmbeddingSize: Int = EMBEDDING_SIZE
        private set
    
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    private var modelMissing = false
    private var lastError: String? = null
    
    /**
     * Check if any face embedding model is available.
     * Prefers ArcFace but falls back to MobileFaceNet if available.
     */
    fun isModelAvailable(): Boolean {
        return isArcFaceAvailable() || isLegacyModelAvailable()
    }
    
    /**
     * Check if ArcFace model is available.
     */
    private fun isArcFaceAvailable(): Boolean {
        return try {
            val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val size = assetFileDescriptor.declaredLength
            assetFileDescriptor.close()
            size >= MIN_MODEL_SIZE
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if legacy MobileFaceNet model is available.
     */
    private fun isLegacyModelAvailable(): Boolean {
        return try {
            val assetFileDescriptor = context.assets.openFd(LEGACY_MODEL_FILE)
            val size = assetFileDescriptor.declaredLength
            assetFileDescriptor.close()
            size >= MIN_MODEL_SIZE
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get the reason why the model is not available.
     */
    fun getModelStatus(): ModelStatus {
        return when {
            isModelLoaded -> ModelStatus.Ready
            modelMissing -> ModelStatus.Missing
            lastError != null -> ModelStatus.Error(lastError!!)
            else -> ModelStatus.NotInitialized
        }
    }
    
    sealed class ModelStatus {
        data object Ready : ModelStatus()
        data object Missing : ModelStatus()
        data object NotInitialized : ModelStatus()
        data class Error(val message: String) : ModelStatus()
    }
    
    /**
     * Initialize the TFLite interpreter.
     * Prefers ArcFace model but falls back to MobileFaceNet if ArcFace is not available.
     * Call this before using any embedding functions.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isModelLoaded) return@withContext true
        if (modelMissing) return@withContext false
        
        try {
            // Try ArcFace first, then fall back to legacy model
            val (modelToLoad, embeddingSize) = when {
                isArcFaceAvailable() -> MODEL_FILE to EMBEDDING_SIZE
                isLegacyModelAvailable() -> LEGACY_MODEL_FILE to LEGACY_EMBEDDING_SIZE
                else -> {
                    modelMissing = true
                    lastError = "人脸嵌入模型未找到。人脸聚类功能将被禁用，但人脸检测仍可使用（通过 ML Kit）。"
                    android.util.Log.i("FaceEmbedding", lastError!!)
                    // 返回 false 但不阻塞其他功能
                    return@withContext false
                }
            }
            
            currentEmbeddingSize = embeddingSize
            
            val modelBuffer = loadModelFile(modelToLoad)
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    // 使用 CPU 核心数的一半，参考 PhotoPrism 的做法
                    val numThreads = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(2)
                    setNumThreads(numThreads)
                }
                interpreter = Interpreter(modelBuffer, options)
                isModelLoaded = true
                lastError = null
                
                val modelName = if (embeddingSize == EMBEDDING_SIZE) "ArcFace" else "MobileFaceNet (legacy)"
                android.util.Log.i("FaceEmbedding", "$modelName model loaded successfully ($embeddingSize-dim embeddings)")
                true
            } else {
                lastError = "Failed to load model file"
                false
            }
        } catch (e: Exception) {
            lastError = "Model initialization failed: ${e.message}"
            android.util.Log.e("FaceEmbedding", lastError!!, e)
            false
        }
    }
    
    /**
     * Load the TFLite model file from assets.
     */
    private fun loadModelFile(modelFileName: String = MODEL_FILE): MappedByteBuffer? {
        return try {
            val assetFileDescriptor = context.assets.openFd(modelFileName)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Check if the model is loaded and ready.
     */
    fun isReady(): Boolean = isModelLoaded && interpreter != null
    
    /**
     * Generate embedding from a face bitmap.
     * The bitmap should be a cropped face image.
     * 
     * @param faceBitmap The cropped face image
     * @return Embedding vector (512-dim for ArcFace, 128-dim for legacy), or null if failed
     */
    suspend fun generateEmbedding(faceBitmap: Bitmap): FloatArray? = withContext(Dispatchers.IO) {
        if (!isReady()) {
            if (!initialize()) return@withContext null
        }
        
        try {
            // Resize to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)
            
            // Convert to input tensor
            val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
            
            // Prepare output buffer with current embedding size
            val outputArray = Array(1) { FloatArray(currentEmbeddingSize) }
            
            // Run inference
            interpreter?.run(inputBuffer, outputArray)
            
            // Normalize the embedding (L2 normalization) - PhotoPrism best practice
            val embedding = outputArray[0]
            normalizeL2(embedding)
            
            embedding
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Generate embedding from a photo URI and bounding box.
     * Crops the face region and generates the embedding.
     * 
     * @param photoUri URI of the photo
     * @param boundingBox Normalized bounding box (0.0-1.0)
     * @return Embedding vector (512-dim for ArcFace, 128-dim for legacy), or null if failed
     */
    suspend fun generateEmbedding(photoUri: Uri, boundingBox: RectF): FloatArray? = withContext(Dispatchers.IO) {
        try {
            val faceBitmap = cropFaceFromImage(photoUri, boundingBox) ?: return@withContext null
            generateEmbedding(faceBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Crop face region from an image.
     * 
     * @param photoUri URI of the photo
     * @param boundingBox Normalized bounding box (0.0-1.0)
     * @return Cropped face bitmap
     */
    private fun cropFaceFromImage(photoUri: Uri, boundingBox: RectF): Bitmap? {
        return try {
            // First, get image dimensions without loading full image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight
            
            // Calculate sample size for memory efficiency
            val faceWidth = ((boundingBox.right - boundingBox.left) * imageWidth).toInt()
            val faceHeight = ((boundingBox.bottom - boundingBox.top) * imageHeight).toInt()
            val maxFaceSize = maxOf(faceWidth, faceHeight)
            
            // If face region is very large, sample down
            val sampleSize = if (maxFaceSize > 500) {
                (maxFaceSize / 500).coerceAtLeast(1)
            } else {
                1
            }
            
            // Load the full image with appropriate sample size
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val original = context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, loadOptions)
            } ?: return null
            
            // Adjust bounding box for sampled image
            val scaledWidth = original.width
            val scaledHeight = original.height
            
            // Calculate crop rectangle with padding
            val padding = 0.15f // Add 15% padding around face
            val left = ((boundingBox.left - padding) * scaledWidth).toInt().coerceIn(0, scaledWidth)
            val top = ((boundingBox.top - padding) * scaledHeight).toInt().coerceIn(0, scaledHeight)
            val right = ((boundingBox.right + padding) * scaledWidth).toInt().coerceIn(0, scaledWidth)
            val bottom = ((boundingBox.bottom + padding) * scaledHeight).toInt().coerceIn(0, scaledHeight)
            
            val cropWidth = (right - left).coerceAtLeast(1)
            val cropHeight = (bottom - top).coerceAtLeast(1)
            
            // Crop the face region
            val croppedFace = Bitmap.createBitmap(original, left, top, cropWidth, cropHeight)
            
            // Make it square (face models work better with square inputs)
            makeSquare(croppedFace)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Make a bitmap square by padding or cropping.
     */
    private fun makeSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width == height) return bitmap
        
        val size = maxOf(width, height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawColor(android.graphics.Color.BLACK)
        
        val left = (size - width) / 2f
        val top = (size - height) / 2f
        canvas.drawBitmap(bitmap, left, top, null)
        
        return output
    }
    
    /**
     * Convert bitmap to ByteBuffer for TFLite input.
     * Normalizes pixel values to [-1, 1] range.
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        for (pixelValue in intValues) {
            // Extract RGB values
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)
            
            // Normalize to [-1, 1]
            byteBuffer.putFloat((r - PIXEL_MEAN) / PIXEL_STD)
            byteBuffer.putFloat((g - PIXEL_MEAN) / PIXEL_STD)
            byteBuffer.putFloat((b - PIXEL_MEAN) / PIXEL_STD)
        }
        
        return byteBuffer
    }
    
    /**
     * Normalize a vector using L2 normalization (in-place).
     */
    private fun normalizeL2(vector: FloatArray) {
        var norm = 0f
        for (v in vector) {
            norm += v * v
        }
        norm = sqrt(norm)
        
        if (norm > 0) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
    }
    
    /**
     * Calculate cosine similarity between two embedding vectors.
     * Returns a value between -1 and 1, where 1 means identical.
     * 
     * @param a First embedding
     * @param b Second embedding
     * @return Cosine similarity score
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embeddings must have the same size" }
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }
    
    /**
     * Calculate cosine distance (1 - similarity) between two embeddings.
     * Returns a value between 0 and 2, where 0 means identical.
     */
    fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        return 1f - cosineSimilarity(a, b)
    }
    
    /**
     * Convert embedding to ByteArray for database storage.
     */
    fun embeddingToByteArray(embedding: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(embedding.size * 4)
        buffer.order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(embedding)
        return buffer.array()
    }
    
    /**
     * Convert ByteArray back to embedding FloatArray.
     */
    fun byteArrayToEmbedding(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.nativeOrder())
        val floatArray = FloatArray(bytes.size / 4)
        buffer.asFloatBuffer().get(floatArray)
        return floatArray
    }
    
    /**
     * Calculate average embedding from multiple embeddings.
     * Useful for creating a representative embedding for a person.
     */
    fun calculateAverageEmbedding(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return FloatArray(currentEmbeddingSize)
        if (embeddings.size == 1) return embeddings[0].copyOf()
        
        val embeddingSize = embeddings[0].size
        val average = FloatArray(embeddingSize)
        for (embedding in embeddings) {
            for (i in average.indices.take(embedding.size)) {
                average[i] += embedding[i]
            }
        }
        
        for (i in average.indices) {
            average[i] /= embeddings.size
        }
        
        // L2 normalize the average - PhotoPrism best practice
        normalizeL2(average)
        
        return average
    }
    
    /**
     * Find the most similar embedding from a list.
     * 
     * @param target The target embedding to match
     * @param candidates List of candidate embeddings with their IDs
     * @return Pair of (best matching ID, similarity score), or null if no match above threshold
     */
    fun findMostSimilar(
        target: FloatArray,
        candidates: List<Pair<String, FloatArray>>,
        threshold: Float = 0.5f
    ): Pair<String, Float>? {
        var bestMatch: Pair<String, Float>? = null
        var bestScore = threshold
        
        for ((id, embedding) in candidates) {
            val similarity = cosineSimilarity(target, embedding)
            if (similarity > bestScore) {
                bestScore = similarity
                bestMatch = Pair(id, similarity)
            }
        }
        
        return bestMatch
    }
    
    /**
     * Close the interpreter and release resources.
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}
