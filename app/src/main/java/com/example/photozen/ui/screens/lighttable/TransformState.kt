package com.example.photozen.ui.screens.lighttable

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Holds the transformation state (scale, offset) for synchronized zoom/pan.
 * This state is shared across all images in comparison mode.
 * 
 * Design: State Hoisting pattern - parent holds state, children observe.
 */
@Stable
class TransformState {
    /**
     * Current zoom scale (1.0 = fit to container, higher = zoomed in)
     */
    var scale by mutableFloatStateOf(1f)
        private set
    
    /**
     * Current pan offset in pixels
     */
    var offsetX by mutableFloatStateOf(0f)
        private set
    
    var offsetY by mutableFloatStateOf(0f)
        private set
    
    /**
     * Minimum allowed scale
     */
    val minScale = 1f
    
    /**
     * Maximum allowed scale
     */
    val maxScale = 5f
    
    /**
     * Update scale with bounds checking.
     */
    fun updateScale(newScale: Float, centroid: Offset = Offset.Zero) {
        val oldScale = scale
        scale = newScale.coerceIn(minScale, maxScale)
        
        // Adjust offset to zoom towards centroid
        if (oldScale != scale) {
            val scaleChange = scale / oldScale
            offsetX = (offsetX - centroid.x) * scaleChange + centroid.x
            offsetY = (offsetY - centroid.y) * scaleChange + centroid.y
        }
    }
    
    /**
     * Update pan offset.
     */
    fun updateOffset(deltaX: Float, deltaY: Float) {
        offsetX += deltaX
        offsetY += deltaY
    }
    
    /**
     * Set offset directly.
     */
    fun setOffset(x: Float, y: Float) {
        offsetX = x
        offsetY = y
    }
    
    /**
     * Reset to default state (fit to container, no pan).
     */
    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    /**
     * Copy state from another TransformState.
     * Used for state synchronization when switching between sync and individual modes.
     */
    fun copyFrom(other: TransformState) {
        scale = other.scale
        offsetX = other.offsetX
        offsetY = other.offsetY
    }
    
    /**
     * Apply bounds to prevent panning outside image.
     * Call this after gesture ends to snap back if needed.
     */
    fun applyBounds(containerWidth: Float, containerHeight: Float) {
        // Calculate the scaled image bounds
        val scaledWidth = containerWidth * scale
        val scaledHeight = containerHeight * scale
        
        // Calculate max offsets (how much we can pan)
        val maxOffsetX = (scaledWidth - containerWidth) / 2
        val maxOffsetY = (scaledHeight - containerHeight) / 2
        
        // Clamp offsets
        if (scale > 1f) {
            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            // When not zoomed, center the image
            offsetX = 0f
            offsetY = 0f
        }
    }
}

/**
 * Factory function to remember a TransformState.
 */
@androidx.compose.runtime.Composable
fun rememberTransformState(): TransformState {
    return androidx.compose.runtime.remember { TransformState() }
}
