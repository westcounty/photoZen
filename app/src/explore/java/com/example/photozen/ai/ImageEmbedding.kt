package com.example.photozen.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
 * Image embedding generator using MobileNet V3 Small TFLite model.
 * Generates 1280-dimensional embedding vectors for image similarity search.
 */
@Singleton
class ImageEmbedding @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MODEL_FILE = "mobilenet_v3_small.tflite"
        private const val INPUT_SIZE = 224 // MobileNet V3 expects 224x224 input
        private const val EMBEDDING_SIZE = 1280 // MobileNet V3 Small output dimensions
        private const val MIN_MODEL_SIZE = 100_000L // 100KB minimum for valid model
        
        // MobileNet V3 normalization: [0, 255] -> [-1, 1]
        private const val PIXEL_MEAN = 127.5f
        private const val PIXEL_STD = 127.5f
    }
    
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    private var modelMissing = false
    private var lastError: String? = null
    
    /**
     * Check if the model file exists and is valid.
     */
    fun isModelAvailable(): Boolean {
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
     * Get the current model status.
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
     * Call this before using any embedding functions.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isModelLoaded) return@withContext true
        if (modelMissing) return@withContext false
        
        try {
            // First check if model exists
            if (!isModelAvailable()) {
                modelMissing = true
                lastError = "MobileNet V3 model not found. Please download and place $MODEL_FILE in app/src/main/assets/"
                android.util.Log.w("ImageEmbedding", lastError!!)
                return@withContext false
            }
            
            val modelBuffer = loadModelFile()
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)
                isModelLoaded = true
                lastError = null
                android.util.Log.i("ImageEmbedding", "MobileNet V3 model loaded successfully")
                true
            } else {
                lastError = "Failed to load model file"
                false
            }
        } catch (e: Exception) {
            lastError = "Model initialization failed: ${e.message}"
            android.util.Log.e("ImageEmbedding", lastError!!, e)
            false
        }
    }
    
    /**
     * Load the TFLite model file from assets.
     */
    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
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
     * Generate embedding from a photo URI.
     * 
     * @param photoUri URI of the photo
     * @return 1280-dimensional embedding vector, or null if failed
     */
    suspend fun generateEmbedding(photoUri: Uri): FloatArray? = withContext(Dispatchers.IO) {
        if (!isReady()) {
            if (!initialize()) return@withContext null
        }
        
        try {
            val bitmap = loadAndResizeBitmap(photoUri) ?: return@withContext null
            generateEmbedding(bitmap)
        } catch (e: Exception) {
            android.util.Log.e("ImageEmbedding", "Failed to generate embedding", e)
            null
        }
    }
    
    /**
     * Generate embedding from a Bitmap.
     * 
     * @param bitmap The image bitmap
     * @return 1280-dimensional embedding vector, or null if failed
     */
    suspend fun generateEmbedding(bitmap: Bitmap): FloatArray? = withContext(Dispatchers.IO) {
        if (!isReady()) {
            if (!initialize()) return@withContext null
        }
        
        try {
            // Resize to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            
            // Convert to input tensor
            val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
            
            // Prepare output buffer
            val outputArray = Array(1) { FloatArray(EMBEDDING_SIZE) }
            
            // Run inference
            interpreter?.run(inputBuffer, outputArray)
            
            // Normalize the embedding (L2 normalization)
            val embedding = outputArray[0]
            normalizeL2(embedding)
            
            embedding
        } catch (e: Exception) {
            android.util.Log.e("ImageEmbedding", "Inference failed", e)
            null
        }
    }
    
    /**
     * Batch generate embeddings for multiple photos.
     * 
     * @param photoUris List of photo URIs
     * @return Map of URI to embedding vector
     */
    suspend fun generateEmbeddings(photoUris: List<Uri>): Map<Uri, FloatArray> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<Uri, FloatArray>()
        
        for (uri in photoUris) {
            try {
                val embedding = generateEmbedding(uri)
                if (embedding != null) {
                    results[uri] = embedding
                }
            } catch (e: Exception) {
                android.util.Log.w("ImageEmbedding", "Failed to process $uri", e)
            }
        }
        
        results
    }
    
    /**
     * Load and resize bitmap from URI.
     */
    private fun loadAndResizeBitmap(uri: Uri): Bitmap? {
        return try {
            // First, get image dimensions without loading full image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight
            
            // Calculate sample size for memory efficiency
            val maxDimension = maxOf(imageWidth, imageHeight)
            val sampleSize = if (maxDimension > INPUT_SIZE * 4) {
                (maxDimension / (INPUT_SIZE * 2)).coerceAtLeast(1)
            } else {
                1
            }
            
            // Load the image with appropriate sample size
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, loadOptions)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageEmbedding", "Failed to load bitmap from $uri", e)
            null
        }
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
     * Get the embedding size.
     */
    fun getEmbeddingSize(): Int = EMBEDDING_SIZE
    
    /**
     * Close the interpreter and release resources.
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}
