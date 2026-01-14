package com.example.photozen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photozen.ui.screens.flowsorter.ComboLevel
import com.example.photozen.ui.screens.flowsorter.ComboState
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed

/**
 * Combo overlay that displays the current combo count with animated effects.
 * 
 * Optimized for smooth, non-blocking animations during rapid swipes.
 * 
 * Visual feedback:
 * - x1-x4: White, normal size
 * - x5-x9: Light orange, slightly larger
 * - x10-x19: Orange, larger with glow
 * - x20+: Red/Fire effect, largest
 */
@Composable
fun ComboOverlay(
    comboState: ComboState,
    modifier: Modifier = Modifier
) {
    val isVisible = comboState.count >= 1 && comboState.isActive
    
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(80)
        ) + fadeIn(animationSpec = tween(80)),
        exit = scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(100)
        ) + fadeOut(animationSpec = tween(100)),
        modifier = modifier
    ) {
        ComboCounter(
            count = comboState.count,
            level = comboState.level
        )
    }
}

@Composable
private fun ComboCounter(
    count: Int,
    level: ComboLevel
) {
    // Scale based on level
    val baseScale = when (level) {
        ComboLevel.NONE -> 1f
        ComboLevel.NORMAL -> 1f
        ComboLevel.WARM -> 1.1f
        ComboLevel.HOT -> 1.2f
        ComboLevel.FIRE -> 1.3f
    }
    
    // Fast, non-blocking scale animation
    val animatedScale by animateFloatAsState(
        targetValue = baseScale,
        animationSpec = tween(50),
        label = "combo_scale"
    )
    
    // Color based on level
    val color = when (level) {
        ComboLevel.NONE -> Color.White
        ComboLevel.NORMAL -> Color.White
        ComboLevel.WARM -> Color(0xFFFFD54F) // Light amber
        ComboLevel.HOT -> MaybeAmber
        ComboLevel.FIRE -> TrashRed
    }
    
    // Font size based on level (smaller to be less intrusive)
    val fontSize = when (level) {
        ComboLevel.NONE -> 28.sp
        ComboLevel.NORMAL -> 30.sp
        ComboLevel.WARM -> 34.sp
        ComboLevel.HOT -> 38.sp
        ComboLevel.FIRE -> 44.sp
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(animatedScale)
            .graphicsLayer { 
                // Use hardware layer for smoother rendering
                this.alpha = 0.9f
            }
    ) {
        Text(
            text = "x$count",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = color,
            style = MaterialTheme.typography.displaySmall.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = color.copy(alpha = 0.5f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                    blurRadius = 6f
                )
            )
        )
        
        // Only show emoji for FIRE level to reduce visual noise
        if (level == ComboLevel.FIRE) {
            Text(
                text = "🔥",
                fontSize = 16.sp
            )
        }
    }
}

/**
 * Compact combo indicator for the top bar.
 */
@Composable
fun ComboIndicator(
    comboState: ComboState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = comboState.count >= 1,
        enter = fadeIn(animationSpec = tween(50)) + scaleIn(animationSpec = tween(50)),
        exit = fadeOut(animationSpec = tween(50)) + scaleOut(animationSpec = tween(50)),
        modifier = modifier
    ) {
        val color = when (comboState.level) {
            ComboLevel.NONE -> Color.White
            ComboLevel.NORMAL -> Color.White
            ComboLevel.WARM -> Color(0xFFFFD54F)
            ComboLevel.HOT -> MaybeAmber
            ComboLevel.FIRE -> TrashRed
        }
        
        Text(
            text = "x${comboState.count}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
