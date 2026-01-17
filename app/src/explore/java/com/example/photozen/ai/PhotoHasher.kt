package com.example.photozen.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Photo hasher and quality analyzer.
 * Provides perceptual hashing for duplicate detection and image quality analysis.
 * 
 * Inspired by PhotoPrism's fingerprinting and quality assessment systems.
 * Reference: https://docs.photoprism.app/developer-guide/media-files/fingerprints/
 */
@Singleton
class PhotoHasher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val HASH_SIZE = 8 // 8x8 = 64 bit hash
        private const val DCT_SIZE = 32 // Size for DCT calculation
        
        // Quality thresholds
        private const val MIN_RESOLUTION_HIGH = 4000000 // 4MP for high quality
        private const val MIN_RESOLUTION_MEDIUM = 1000000 // 1MP for medium quality
    }
    
    /**
     * Analyze a photo and return all quality metrics.
     */
    suspend fun analyzePhoto(uri: Uri): PhotoAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            // Load bitmap with appropriate sampling
            val (bitmap, originalWidth, originalHeight) = loadBitmapForAnalysis(uri) 
                ?: return@withContext null
            
            // Calculate all metrics
            val phash = calculatePHash(bitmap)
            val (dominantColor, accentColor) = extractDominantColors(bitmap)
            val luminance = calculateLuminance(bitmap)
            val chroma = calculateChroma(bitmap)
            val quality = calculateQuality(originalWidth, originalHeight, bitmap)
            val sharpness = calculateSharpness(bitmap)
            val aspectRatio = if (originalHeight > 0) originalWidth.toFloat() / originalHeight else 0f
            
            PhotoAnalysisResult(
                phash = phash,
                dominantColor = dominantColor,
                accentColor = accentColor,
                luminance = luminance,
                chroma = chroma,
                quality = quality,
                sharpness = sharpness,
                aspectRatio = aspectRatio
            )
        } catch (e: Exception) {
            android.util.Log.e("PhotoHasher", "Error analyzing photo: ${e.message}", e)
            null
        }
    }
    
    /**
     * Calculate perceptual hash (pHash) for a photo.
     * Uses DCT-based algorithm similar to PhotoPrism.
     * 
     * Returns 64-bit hash as 16-character hex string.
     */
    suspend fun calculatePHash(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val (bitmap, _, _) = loadBitmapForAnalysis(uri) ?: return@withContext null
            calculatePHash(bitmap)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate pHash from bitmap.
     */
    private fun calculatePHash(bitmap: Bitmap): String {
        // Step 1: Resize to DCT_SIZE x DCT_SIZE and convert to grayscale
        val grayscale = bitmapToGrayscale(bitmap)
        
        // Step 2: Compute DCT
        val dct = computeDCT(grayscale)
        
        // Step 3: Extract low-frequency components (top-left 8x8)
        val lowFreq = Array(HASH_SIZE) { row ->
            DoubleArray(HASH_SIZE) { col ->
                dct[row][col]
            }
        }
        
        // Step 4: Calculate median (excluding DC component)
        val values = mutableListOf<Double>()
        for (row in 0 until HASH_SIZE) {
            for (col in 0 until HASH_SIZE) {
                if (row != 0 || col != 0) { // Skip DC component
                    values.add(lowFreq[row][col])
                }
            }
        }
        values.sort()
        val median = values[values.size / 2]
        
        // Step 5: Generate hash bits
        var hash = 0L
        var bit = 0
        for (row in 0 until HASH_SIZE) {
            for (col in 0 until HASH_SIZE) {
                if (lowFreq[row][col] > median) {
                    hash = hash or (1L shl bit)
                }
                bit++
            }
        }
        
        // Return as hex string
        return String.format("%016X", hash)
    }
    
    /**
     * Convert bitmap to grayscale array for DCT.
     */
    private fun bitmapToGrayscale(bitmap: Bitmap): Array<DoubleArray> {
        val scaled = Bitmap.createScaledBitmap(bitmap, DCT_SIZE, DCT_SIZE, true)
        val grayscale = Array(DCT_SIZE) { DoubleArray(DCT_SIZE) }
        
        val pixels = IntArray(DCT_SIZE * DCT_SIZE)
        scaled.getPixels(pixels, 0, DCT_SIZE, 0, 0, DCT_SIZE, DCT_SIZE)
        
        for (y in 0 until DCT_SIZE) {
            for (x in 0 until DCT_SIZE) {
                val pixel = pixels[y * DCT_SIZE + x]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // ITU-R BT.601 grayscale conversion
                grayscale[y][x] = 0.299 * r + 0.587 * g + 0.114 * b
            }
        }
        
        return grayscale
    }
    
    /**
     * Compute 2D DCT (Discrete Cosine Transform).
     */
    private fun computeDCT(input: Array<DoubleArray>): Array<DoubleArray> {
        val n = input.size
        val output = Array(n) { DoubleArray(n) }
        
        // DCT coefficient cache
        val c = DoubleArray(n) { if (it == 0) 1.0 / sqrt(2.0) else 1.0 }
        val cosCache = Array(n) { u ->
            DoubleArray(n) { x ->
                cos((2.0 * x + 1.0) * u * Math.PI / (2.0 * n))
            }
        }
        
        for (u in 0 until n) {
            for (v in 0 until n) {
                var sum = 0.0
                for (x in 0 until n) {
                    for (y in 0 until n) {
                        sum += input[x][y] * cosCache[u][x] * cosCache[v][y]
                    }
                }
                output[u][v] = c[u] * c[v] * sum * 2.0 / n
            }
        }
        
        return output
    }
    
    /**
     * Calculate Hamming distance between two hashes.
     * Lower distance = more similar images.
     * Distance 0 = likely exact duplicate.
     * Distance <= 10 = likely same image (different quality/size).
     * Distance <= 20 = similar images.
     */
    fun hammingDistance(hash1: String, hash2: String): Int {
        if (hash1.length != 16 || hash2.length != 16) return -1
        
        val h1 = hash1.toLongOrNull(16) ?: return -1
        val h2 = hash2.toLongOrNull(16) ?: return -1
        
        return java.lang.Long.bitCount(h1 xor h2)
    }
    
    /**
     * Check if two photos are likely duplicates.
     */
    fun areLikelyDuplicates(hash1: String?, hash2: String?, threshold: Int = 10): Boolean {
        if (hash1 == null || hash2 == null) return false
        val distance = hammingDistance(hash1, hash2)
        return distance >= 0 && distance <= threshold
    }
    
    /**
     * Extract dominant colors from bitmap.
     */
    private fun extractDominantColors(bitmap: Bitmap): Pair<String, String?> {
        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        val pixels = IntArray(50 * 50)
        scaled.getPixels(pixels, 0, 50, 0, 0, 50, 50)
        
        // Simple color clustering using k-means-like approach
        val colorCounts = mutableMapOf<Int, Int>()
        
        for (pixel in pixels) {
            // Quantize colors to reduce unique colors
            val quantized = quantizeColor(pixel)
            colorCounts[quantized] = (colorCounts[quantized] ?: 0) + 1
        }
        
        // Sort by frequency
        val sortedColors = colorCounts.entries.sortedByDescending { it.value }
        
        val dominantColor = if (sortedColors.isNotEmpty()) {
            String.format("#%06X", 0xFFFFFF and sortedColors[0].key)
        } else "#808080"
        
        val accentColor = if (sortedColors.size > 1) {
            String.format("#%06X", 0xFFFFFF and sortedColors[1].key)
        } else null
        
        return Pair(dominantColor, accentColor)
    }
    
    /**
     * Quantize a color to reduce precision (for grouping).
     */
    private fun quantizeColor(color: Int): Int {
        val r = (Color.red(color) / 32) * 32
        val g = (Color.green(color) / 32) * 32
        val b = (Color.blue(color) / 32) * 32
        return Color.rgb(r, g, b)
    }
    
    /**
     * Calculate average luminance (brightness) of image.
     * Returns 0-100.
     */
    private fun calculateLuminance(bitmap: Bitmap): Int {
        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        val pixels = IntArray(50 * 50)
        scaled.getPixels(pixels, 0, 50, 0, 0, 50, 50)
        
        var totalLuminance = 0.0
        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            // Relative luminance formula
            totalLuminance += 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        
        val avgLuminance = totalLuminance / pixels.size
        return ((avgLuminance / 255.0) * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Calculate chroma (color saturation).
     * Returns 0-100, where 0 is grayscale.
     */
    private fun calculateChroma(bitmap: Bitmap): Int {
        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        val pixels = IntArray(50 * 50)
        scaled.getPixels(pixels, 0, 50, 0, 0, 50, 50)
        
        var totalChroma = 0.0
        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val chroma = if (max > 0) (max - min).toDouble() / max else 0.0
            totalChroma += chroma
        }
        
        val avgChroma = totalChroma / pixels.size
        return (avgChroma * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Calculate image quality score based on multiple factors.
     * Returns 0-100.
     */
    private fun calculateQuality(width: Int, height: Int, bitmap: Bitmap): Int {
        val resolution = width * height
        
        // Resolution score (40 points max)
        val resolutionScore = when {
            resolution >= MIN_RESOLUTION_HIGH -> 40
            resolution >= MIN_RESOLUTION_MEDIUM -> 30
            else -> (resolution.toFloat() / MIN_RESOLUTION_MEDIUM * 30).toInt()
        }
        
        // Size consistency score (20 points) - prefer standard aspect ratios
        val aspectRatio = width.toFloat() / height.coerceAtLeast(1)
        val standardRatios = listOf(1.0f, 1.33f, 1.5f, 1.78f) // 1:1, 4:3, 3:2, 16:9
        val nearestRatio = standardRatios.minByOrNull { kotlin.math.abs(it - aspectRatio) } ?: 1.0f
        val aspectScore = if (kotlin.math.abs(aspectRatio - nearestRatio) < 0.1f) 20 else 15
        
        // Contrast score (20 points) - good photos have good dynamic range
        val contrastScore = calculateContrastScore(bitmap)
        
        // Sharpness contributes to remaining 20 points
        val sharpnessScore = (calculateSharpness(bitmap) * 0.2).toInt()
        
        return (resolutionScore + aspectScore + contrastScore + sharpnessScore).coerceIn(0, 100)
    }
    
    /**
     * Calculate contrast score from histogram spread.
     */
    private fun calculateContrastScore(bitmap: Bitmap): Int {
        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        val pixels = IntArray(50 * 50)
        scaled.getPixels(pixels, 0, 50, 0, 0, 50, 50)
        
        var minLum = 255.0
        var maxLum = 0.0
        
        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val lum = 0.299 * r + 0.587 * g + 0.114 * b
            minLum = minOf(minLum, lum)
            maxLum = maxOf(maxLum, lum)
        }
        
        val contrast = (maxLum - minLum) / 255.0
        return (contrast * 20).toInt().coerceIn(0, 20)
    }
    
    /**
     * Calculate sharpness using Laplacian variance.
     * Higher variance = sharper image.
     * Returns 0-100.
     */
    private fun calculateSharpness(bitmap: Bitmap): Int {
        val scaled = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
        val grayscale = Array(100) { y ->
            IntArray(100) { x ->
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }
        
        // Apply Laplacian kernel
        var variance = 0.0
        var count = 0
        
        for (y in 1 until 99) {
            for (x in 1 until 99) {
                val laplacian = (
                    -grayscale[y-1][x] - grayscale[y][x-1] +
                    4 * grayscale[y][x] -
                    grayscale[y+1][x] - grayscale[y][x+1]
                )
                variance += laplacian * laplacian
                count++
            }
        }
        
        variance /= count
        
        // Normalize to 0-100 range (variance > 500 is considered sharp)
        return (variance / 500.0 * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Load bitmap with appropriate sampling for analysis.
     * Returns (bitmap, originalWidth, originalHeight)
     */
    private fun loadBitmapForAnalysis(uri: Uri): Triple<Bitmap, Int, Int>? {
        return try {
            // First get dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            }
            
            val width = options.outWidth
            val height = options.outHeight
            
            if (width <= 0 || height <= 0) return null
            
            // Calculate sample size (target ~200x200 for analysis)
            val maxDim = maxOf(width, height)
            val sampleSize = (maxDim / 200).coerceAtLeast(1)
            
            // Load sampled bitmap
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, loadOptions)
            } ?: return null
            
            Triple(bitmap, width, height)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Photo analysis result containing all quality metrics.
 */
data class PhotoAnalysisResult(
    val phash: String,
    val dominantColor: String,
    val accentColor: String?,
    val luminance: Int,
    val chroma: Int,
    val quality: Int,
    val sharpness: Int,
    val aspectRatio: Float
)
