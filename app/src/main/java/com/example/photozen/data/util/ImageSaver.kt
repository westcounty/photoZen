package com.example.photozen.data.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.data.model.AspectRatio
import com.example.photozen.data.model.CropState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for saving cropped/edited photos to MediaStore.
 */
@Singleton
class ImageSaver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Save a photo with crop/rotation applied as a new image file.
     * 
     * @param photo The source photo entity
     * @return URI of the saved image, or null if failed
     */
    suspend fun saveWithCrop(photo: PhotoEntity): Uri? = withContext(Dispatchers.IO) {
        try {
            // Load original bitmap
            val uri = Uri.parse(photo.systemUri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) return@withContext null
            
            val cropState = photo.cropState
            val aspectRatio = AspectRatio.fromId(cropState.aspectRatioId)
            
            // Calculate output dimensions
            val (outputWidth, outputHeight) = calculateOutputDimensions(
                originalBitmap.width,
                originalBitmap.height,
                cropState,
                aspectRatio
            )
            
            // Create output bitmap
            val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            
            // Apply transformations
            val matrix = Matrix()
            
            // Center the image
            val centerX = outputWidth / 2f
            val centerY = outputHeight / 2f
            
            // Apply scale
            val scale = cropState.scale
            matrix.postScale(scale, scale, centerX, centerY)
            
            // Apply rotation
            matrix.postRotate(cropState.rotation, centerX, centerY)
            
            // Apply offset
            matrix.postTranslate(cropState.offsetX, cropState.offsetY)
            
            // Calculate source rect to fit the output dimensions
            val srcRect = RectF(0f, 0f, originalBitmap.width.toFloat(), originalBitmap.height.toFloat())
            val dstRect = RectF(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat())
            
            // Fit the original bitmap to the output size maintaining aspect ratio
            val fitMatrix = Matrix()
            fitMatrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER)
            fitMatrix.postConcat(matrix)
            
            canvas.drawBitmap(originalBitmap, fitMatrix, paint)
            
            // Save to MediaStore
            val savedUri = saveBitmapToMediaStore(outputBitmap, generateFileName(photo))
            
            // Clean up
            originalBitmap.recycle()
            outputBitmap.recycle()
            
            savedUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun calculateOutputDimensions(
        originalWidth: Int,
        originalHeight: Int,
        cropState: CropState,
        aspectRatio: AspectRatio
    ): Pair<Int, Int> {
        val maxSize = 4096 // Max output dimension
        
        return when (aspectRatio) {
            AspectRatio.ORIGINAL -> {
                originalWidth.coerceAtMost(maxSize) to originalHeight.coerceAtMost(maxSize)
            }
            else -> {
                // Calculate based on aspect ratio
                val ratio = aspectRatio.ratio ?: (originalWidth.toFloat() / originalHeight)
                if (ratio > 1f) {
                    // Landscape
                    val width = originalWidth.coerceAtMost(maxSize)
                    val height = (width / ratio).toInt().coerceIn(100, maxSize)
                    width to height
                } else {
                    // Portrait
                    val height = originalHeight.coerceAtMost(maxSize)
                    val width = (height * ratio).toInt().coerceIn(100, maxSize)
                    width to height
                }
            }
        }
    }
    
    private fun generateFileName(photo: PhotoEntity): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val baseName = photo.displayName.substringBeforeLast(".")
        return "PicZen_${baseName}_$timestamp.jpg"
    }
    
    private fun saveBitmapToMediaStore(bitmap: Bitmap, fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PicZen")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        
        val uri = context.contentResolver.insert(collection, values) ?: return null
        
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            } ?: run {
                context.contentResolver.delete(uri, null, null)
                return null
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            
            return uri
        } catch (e: IOException) {
            context.contentResolver.delete(uri, null, null)
            return null
        }
    }
}
