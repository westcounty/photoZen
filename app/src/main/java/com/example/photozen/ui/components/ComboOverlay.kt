package com.example.photozen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

/**
 * Combo overlay that displays the current combo count with animated effects.
 * 
 * Visual feedback:
 * - x1-x4: White, normal size
 * - x5-x9: Light orange, slightly larger
 * - x10-x19: Orange, larger with glow
 * - x20+: Red/Fire effect, largest with pulsing
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
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(),
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
        ComboLevel.WARM -> 1.15f
        ComboLevel.HOT -> 1.3f
        ComboLevel.FIRE -> 1.5f
    }
    
    // Animated scale with bounce on change
    var targetScale by remember { mutableFloatStateOf(baseScale) }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "combo_scale"
    )
    
    // Trigger bounce animation on count change
    LaunchedEffect(count) {
        targetScale = baseScale * 1.3f
        delay(100)
        targetScale = baseScale
    }
    
    // Color based on level
    val color = when (level) {
        ComboLevel.NONE -> Color.White
        ComboLevel.NORMAL -> Color.White
        ComboLevel.WARM -> Color(0xFFFFD54F) // Light amber
        ComboLevel.HOT -> MaybeAmber
        ComboLevel.FIRE -> TrashRed
    }
    
    // Pulsing effect for FIRE level
    var pulseAlpha by remember { mutableFloatStateOf(1f) }
    if (level == ComboLevel.FIRE) {
        LaunchedEffect(Unit) {
            while (true) {
                pulseAlpha = 0.7f
                delay(300)
                pulseAlpha = 1f
                delay(300)
            }
        }
    } else {
        pulseAlpha = 1f
    }
    
    // Font size based on level
    val fontSize = when (level) {
        ComboLevel.NONE -> 32.sp
        ComboLevel.NORMAL -> 36.sp
        ComboLevel.WARM -> 42.sp
        ComboLevel.HOT -> 48.sp
        ComboLevel.FIRE -> 56.sp
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(animatedScale)
            .graphicsLayer { alpha = pulseAlpha }
    ) {
        Text(
            text = "x$count",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = color,
            style = MaterialTheme.typography.displaySmall.copy(
                // Add text shadow for visibility
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = color.copy(alpha = 0.5f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                    blurRadius = 8f
                )
            )
        )
        
        // Show "COMBO!" text for high combos
        if (level == ComboLevel.HOT || level == ComboLevel.FIRE) {
            Text(
                text = if (level == ComboLevel.FIRE) "🔥 COMBO!" else "COMBO!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.8f)
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
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
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
